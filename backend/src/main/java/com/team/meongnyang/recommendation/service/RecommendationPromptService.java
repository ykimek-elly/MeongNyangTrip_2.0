package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천 설명 생성을 위한 AI 프롬프트를 구성하는 서비스
 * 추천 근거 컨텍스트를 바탕으로 Gemini가 일관된 형식의 추천설명과 알림요약을 생성하도록
 * 입력 문장을 조합하고 정리한다.
 *
 * 사용자 정보, 반려동물 정보, 날씨 정보, 추천 판단 요약, 상위 장소 근거를
 * 하나의 프롬프트로 묶어 전달하며,
 * 불필요하게 긴 텍스트는 축약해 호출 비용과 응답 지연을 줄인다.
 */

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationPromptService {

  /**
   * 추천 근거 컨텍스트를 기반으로 AI 호출용 프롬프트를 생성한다.
   * 추천설명과 알림요약이 지정된 형식과 규칙에 맞게 생성되도록 입력을 구성한다.
   */
  public String buildRecommendationPrompt(RecommendationEvidenceContext context) {
    String userProfile = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getUserProfileSection(), "사용자 정보 없음")
            : "사용자 정보 없음";
    String petProfile = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getPetProfileSection(), "반려동물 정보 없음")
            : "반려동물 정보 없음";
    String weatherSection = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getWeatherSection(), "날씨 정보 없음")
            : "날씨 정보 없음";
    String decisionSummary = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getRecommendationDecisionSummary(), "추천 판단 정보 없음")
            : "추천 판단 정보 없음";
    String topPlaceEvidence = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getTopPlaceEvidenceSection(), "추천 근거 없음")
            : "추천 근거 없음";
    String supplementalGuideline = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getSupplementalGuidelineSection(), "추가 지침 없음")
            : "추가 지침 없음";

    // Gemini 호출 비용과 지연을 줄이기 위해 긴 근거 영역은 미리 압축한다.
    topPlaceEvidence = RecommendationTextUtils.abbreviate(topPlaceEvidence, 700, "추천 근거 없음");
    supplementalGuideline = RecommendationTextUtils.abbreviate(supplementalGuideline, 180, "추가 지침 없음");

    String prompt = """
            [역할]
            당신은 반려동물 동반 장소 추천 설명 생성기다.
            순위는 서버가 이미 계산했으므로 반드시 제공된 근거만 사용한다.
            
            [절대 규칙]
            - 입력에 없는 정보는 절대 생성하지 않는다.
            - 출력 형식을 반드시 지킨다.
            - 규칙을 어길 경우 출력하지 말고 다시 작성한다.
            
            [추천설명 작성 규칙]
            - 한국어로 4~6문장 작성
            - 1순위 장소의 장점은 최소 2가지 이상 반드시 포함
            - 근거 기반으로만 설명 (날씨, 거리, 시설, 성향 등)
            - 2순위/3순위는 한 문장으로 짧게 비교만 한다
            - 점수, score 같은 필드명 절대 언급 금지
            
            [알림요약 작성 규칙]
            - 반드시 한 문장
            - 45자 이내
            - 반드시 "1순위 장소명" 포함
            - 반드시 "추천 이유 1개 이상" 포함
            - 반드시 아래 중 하나 이상 포함:
              → 반려동물 이름 OR 날씨 정보
            
            [추천 이유로 사용할 수 있는 항목]
            - 거리
            - 날씨 적합성
            - 산책 편의성
            - 반려동물 성향 적합성
            - 시설 적합성
            
            [금지 표현]
            - 어떠세요 / 추천드려요 / 가보세요 / 함께해요
            - 감성적 표현 (즐거운, 행복한 등)
            - 추상적 표현 (좋은 장소, 적합한 곳 등 단독 사용 금지)
            
            [설명 구체화 규칙]
            - "적합도", "적합성", "환경이 좋다" 같은 추상 표현 금지
            - 반드시 실제 특징으로 풀어서 설명한다
              (예: 시설, 공간, 이용 조건, 후기 기반 특징 등)
            
            [출력 형식 - 반드시 그대로 사용]
            [추천설명]
            (여기에 4~6문장)
            
            [알림요약]
            (여기에 한 문장)
            
            [입력]
            사용자 정보:
            %s
            
            반려동물 정보:
            %s
            
            날씨 정보:
            %s
            
            추천 판단 요약:
            %s
            
            상위 장소 근거:
            %s
            
            추가 지침:
            %s
            """
            .formatted(
                    userProfile,
                    petProfile,
                    weatherSection,
                    decisionSummary,
                    topPlaceEvidence,
                    supplementalGuideline
            );

    return prompt;
  }
}
