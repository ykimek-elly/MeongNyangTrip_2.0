#!/usr/bin/env node
/**
 * 멍냥트립 장소 데이터 AI 자동 보강 스크립트
 * - Gemini 2.5 Flash API로 1190건 순차 처리
 * - 20건씩 배치 → API 호출 → 결과 누적 저장
 * - 중단 시 이어서 실행 가능 (progress 파일 기반)
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');

// ─── 설정 ───
const GEMINI_API_KEY = 'AIzaSyBh2WOeXeM3Eh_IQIoc0TJzW_0lSP5gUig';
const GEMINI_API_KEY_2 = 'AIzaSyAtNZVCfmrYIwaVNYUvUw--wAttqBXiesk';
const MODEL = 'gemini-2.5-flash';
const BATCH_SIZE = 10;
const DELAY_MS = 4000; // 요청 간 4초 딜레이 (RPM 제한 대비)

const INPUT_FILE = path.join(ROOT, 'exports', 'places_to_enrich.json');
const OUTPUT_FILE = path.join(ROOT, 'exports', 'place_enrich_output.json');
const PROGRESS_FILE = path.join(ROOT, 'exports', '.enrich_progress.json');

// ─── 프롬프트 ───
const SYSTEM_PROMPT = `당신은 반려동물 동반 여행 플랫폼 "멍냥트립"의 장소 데이터 분석 전문가입니다.

## 입력 필드 설명
- id: DB 고유 식별자 (수정 금지)
- title: 장소명
- address: 주소 (+ addr2: 상세주소)
- category: PLACE(명소) / STAY(숙박) / DINING(식당/카페)
- current_overview: 기존 소개글 (있으면 참고, 없으면 새로 작성)
- chk_pet_inside: 실내 동반 가능 여부 (Y/N/빈값)
- accom_count_pet: 반려동물 수용 가능 수 (공공데이터 원문)
- pet_turn_adroose: 반려동물 동반 상세정보 (공공데이터 원문)
- homepage: 홈페이지 URL
- phone: 전화번호
- current_tags: 기존 태그 (있으면 유지하되 보강 가능)

## 출력 규칙
각 장소에 대해 아래 필드를 가진 JSON 배열을 반환하세요:

1. **id**: 입력된 id 그대로 유지
2. **overview**: 반려동물 동반 관점의 장소 소개 (3~4문장, 자연스러운 한국어)
   - current_overview가 있으면 내용을 기반으로 개선, 없으면 title/address/category로 추론
3. **pet_facility**: 반려동물 전용 시설 정보 (pet_turn_adroose 참고, 없으면 "정보 없음")
4. **pet_policy**: 반려동물 동반 규정 및 주의사항 (accom_count_pet/chk_pet_inside 참고, 없으면 "정보 없음")
5. **operating_hours**: 영업 시간 및 휴무일 (알 수 없으면 "정보 없음")
6. **operation_policy**: 일반 운영 정책 특이사항 (없으면 "정보 없음")
7. **tags**: 반려동물 관련 태그 문자열 (쉼표 구분, 3~6개)
   - current_tags가 있으면 유지하고 누락된 태그만 추가
   - 반려동물: 실내동반, 실외동반, 대형견가능, 소형견전용, 애견수영장, 애견운동장
   - 시설: 주차가능, 야외테라스, 룸형, 프라이빗, 독채, 루프탑
   - 분위기: 감성카페, 자연친화, 도심속힐링, 가족여행, 반려견동반필수

## 중요
- 반드시 JSON 배열만 반환하세요. 마크다운 코드블록(\`\`\`)이나 설명 없이 순수 JSON만 출력합니다.
- id는 반드시 입력과 동일하게 유지하세요.
- 허구 정보는 절대 작성하지 마세요. 불확실하면 "정보 없음"으로 기록하세요.`;

// ─── 유틸 ───
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function loadProgress() {
  if (fs.existsSync(PROGRESS_FILE)) {
    return JSON.parse(fs.readFileSync(PROGRESS_FILE, 'utf-8'));
  }
  return { completedIds: [], lastBatch: 0 };
}

function saveProgress(progress) {
  fs.writeFileSync(PROGRESS_FILE, JSON.stringify(progress, null, 2));
}

function loadExistingOutput() {
  if (fs.existsSync(OUTPUT_FILE)) {
    try {
      return JSON.parse(fs.readFileSync(OUTPUT_FILE, 'utf-8'));
    } catch {
      return [];
    }
  }
  return [];
}

function saveOutput(results) {
  fs.writeFileSync(OUTPUT_FILE, JSON.stringify(results, null, 2), 'utf-8');
}

// ─── Gemini API 호출 ───
async function callGemini(batch, apiKey) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${apiKey}`;

  const body = {
    contents: [
      {
        parts: [
          { text: SYSTEM_PROMPT + '\n\n## 분석할 데이터:\n' + JSON.stringify(batch, null, 2) }
        ]
      }
    ],
    generationConfig: {
      temperature: 0.3,
      maxOutputTokens: 16384,
      responseMimeType: 'application/json',
    }
  };

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const err = await response.json();
    throw new Error(`API Error ${response.status}: ${JSON.stringify(err)}`);
  }

  const data = await response.json();
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!text) {
    throw new Error('빈 응답');
  }

  // JSON 파싱 (여러 시도)
  let cleaned = text.trim();

  // 1. 마크다운 코드블록 제거
  if (cleaned.startsWith('```')) {
    cleaned = cleaned.replace(/^```(?:json)?\n?/, '').replace(/\n?```$/, '');
  }

  try {
    return JSON.parse(cleaned);
  } catch {
    // 2. Fallback: JSON 배열 부분만 정규식 추출
    const match = cleaned.match(/\[[\s\S]*\]/);
    if (match) {
      return JSON.parse(match[0]);
    }
    throw new Error('JSON 파싱 실패: ' + cleaned.substring(0, 200));
  }
}

// ─── 메인 실행 ───
async function main() {
  console.log('🐾 멍냥트립 장소 데이터 AI 보강 스크립트 시작\n');

  // 입력 파일 로드
  const allPlaces = JSON.parse(fs.readFileSync(INPUT_FILE, 'utf-8'));
  console.log(`📂 전체 장소: ${allPlaces.length}건`);

  // 이전 진행상황 로드
  const progress = loadProgress();
  const existingOutput = loadExistingOutput();
  const completedIds = new Set(progress.completedIds);

  // 이미 완료된 항목 제외
  const remaining = allPlaces.filter(p => !completedIds.has(p.id));
  console.log(`✅ 이미 완료: ${completedIds.size}건`);
  console.log(`📋 남은 작업: ${remaining.length}건`);
  console.log(`📦 배치 크기: ${BATCH_SIZE}건`);
  console.log(`⏱  요청 간격: ${DELAY_MS / 1000}초`);

  const totalBatches = Math.ceil(remaining.length / BATCH_SIZE);
  console.log(`🔄 총 배치 수: ${totalBatches}회\n`);

  if (remaining.length === 0) {
    console.log('🎉 모든 장소가 이미 처리되었습니다!');
    return;
  }

  let currentKey = GEMINI_API_KEY;
  let keySwapCount = 0;
  let successCount = 0;
  let errorCount = 0;
  const results = [...existingOutput];

  for (let i = 0; i < remaining.length; i += BATCH_SIZE) {
    const batchNum = Math.floor(i / BATCH_SIZE) + 1;
    const batch = remaining.slice(i, i + BATCH_SIZE);
    const ids = batch.map(p => p.id);

    console.log(`\n── 배치 ${batchNum}/${totalBatches} ──`);
    console.log(`   IDs: ${ids.join(', ')}`);

    let retries = 0;
    const MAX_RETRIES = 3;

    while (retries < MAX_RETRIES) {
      try {
        const enriched = await callGemini(batch, currentKey);

        // 결과 병합
        for (const item of enriched) {
          // 중복 방지
          const existIdx = results.findIndex(r => r.id === item.id);
          if (existIdx >= 0) {
            results[existIdx] = item;
          } else {
            results.push(item);
          }
          completedIds.add(item.id);
        }

        successCount += batch.length;
        console.log(`   ✅ 성공! (${enriched.length}건 처리, 누적 ${successCount}건)`);

        // 진행상황 저장 (중단 대비)
        saveProgress({ completedIds: [...completedIds], lastBatch: batchNum });
        saveOutput(results);

        break; // 성공 시 루프 탈출

      } catch (err) {
        retries++;
        console.log(`   ❌ 오류 (시도 ${retries}/${MAX_RETRIES}): ${err.message}`);

        if (err.message.includes('429') || err.message.includes('RESOURCE_EXHAUSTED')) {
          // Rate Limit: 키 교체 시도
          if (currentKey === GEMINI_API_KEY && keySwapCount === 0) {
            currentKey = GEMINI_API_KEY_2;
            keySwapCount++;
            console.log(`   🔑 API 키 2로 교체, 10초 대기...`);
            await sleep(10000);
          } else {
            console.log(`   ⏳ Rate Limit 대기 60초...`);
            await sleep(60000);
          }
        } else if (retries < MAX_RETRIES) {
          console.log(`   ⏳ ${5 * retries}초 후 재시도...`);
          await sleep(5000 * retries);
        } else {
          errorCount += batch.length;
          console.log(`   ⚠️ 배치 ${batchNum} 건너뜀 (${batch.length}건 실패)`);
        }
      }
    }

    // 다음 배치 전 딜레이
    if (i + BATCH_SIZE < remaining.length) {
      process.stdout.write(`   ⏱  ${DELAY_MS / 1000}초 대기 중...`);
      await sleep(DELAY_MS);
      process.stdout.write(' 완료\n');
    }
  }

  // 최종 결과
  console.log('\n════════════════════════════════');
  console.log(`🎉 처리 완료!`);
  console.log(`   ✅ 성공: ${successCount}건`);
  console.log(`   ❌ 실패: ${errorCount}건`);
  console.log(`   📂 결과: ${OUTPUT_FILE}`);
  console.log('════════════════════════════════\n');

  // 완료 시 progress 파일 삭제
  if (errorCount === 0 && fs.existsSync(PROGRESS_FILE)) {
    fs.unlinkSync(PROGRESS_FILE);
    console.log('🧹 진행 파일 정리 완료');
  }
}

main().catch(err => {
  console.error('\n💥 치명적 오류:', err.message);
  process.exit(1);
});
