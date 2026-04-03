package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천 설명 생성을 위한 AI 프롬프트를 구성한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationPromptService {

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
    String explanationFocus = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getExplanationFocusSection(), "설명 필수 근거 없음")
            : "설명 필수 근거 없음";
    String topPlaceEvidence = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getTopPlaceEvidenceSection(), "상위 장소 근거 없음")
            : "상위 장소 근거 없음";
    String supplementalGuideline = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getSupplementalGuidelineSection(), "추가 지침 없음")
            : "추가 지침 없음";

    explanationFocus = RecommendationTextUtils.abbreviate(explanationFocus, 500, "설명 필수 근거 없음");
    topPlaceEvidence = RecommendationTextUtils.abbreviate(topPlaceEvidence, 700, "상위 장소 근거 없음");
    supplementalGuideline = RecommendationTextUtils.abbreviate(supplementalGuideline, 220, "추가 지침 없음");

    return """
          [역할]
          당신은 반려동물 동반 장소 추천 결과를 사용자에게 자연스럽게 설명하는 작성자다.
          추천 순위는 이미 내부 로직으로 결정되었으며, 입력으로 주어진 근거만 사용해 사용자가 납득하기 쉬운 말로 풀어쓴다.

          [가장 먼저 할 일]
          - 장소의 category를 먼저 해석한다.
          - category는 이 장소를 설명하는 가장 우선적인 기준이다.
          - overview, 태그, 부가 정보는 반드시 category의 틀 안에서 해석한다.
          - overview에 야외, 테라스, 뷰, 넓은 공간 같은 표현이 있어도 category를 바꾸지 않는다.

          [category 해석 원칙]
          - STAY: 숙박, 머무름, 쉬는 흐름, 동반 가능 편의, 휴식성 중심으로 설명한다.
          - DINING: 식음, 카페, 브런치, 테라스, 동반 체류, 분위기, 쉬어가기 중심으로 설명한다.
          - DINING은 산책 장소처럼 설명하지 않는다.
          - DINING에서 "산책 동선", "활동량 해소", "야외 활동에 맞는다" 같은 표현은 overview에 근거가 있어도 보조적으로만 쓴다.
          - PLACE: 관광, 산책, 야외 공간, 이동 동선, 둘러보기, 활동성 중심으로 설명한다.

          [category와 overview를 함께 해석하는 원칙]
          - overview의 세부 정보는 category의 틀 안에서만 사용한다.
          - 예:
            - DINING + 테라스 + 수변공원 뷰 -> 산책 장소가 아니라 머무르기 좋은 카페/식음 공간으로 설명
            - PLACE + 넓은 공간 + 산책로 -> 산책, 이동, 활동 중심 설명 가능
            - STAY + 정원 + 야외 공간 -> 숙박 중 쉬어가기 좋은 보조 요소로 설명
          - category와 맞지 않는 문체로 흘러가면 안 된다.

          [핵심 원칙]
          - 입력에 없는 사실은 만들지 않는다.
          - 내부 점수 계산 과정은 절대 드러내지 않는다.
          - 점수, 가점, 감점, 수치, 랭킹 계산 표현은 절대 쓰지 않는다.
          - 장소 소개문이 아니라 왜 오늘 이 장소가 선택되었는지를 설명한다.
          - 억지로 항목을 나열하지 말고 하나의 흐름으로 읽히게 쓴다.
          - 사람이 직접 설명하듯 자연스럽고 읽기 쉽게 쓴다.

          [추천설명 작성 기준]
          - 역할: 상세한 이유 설명
          - 목적: 사용자가 왜 이 장소가 선택되었는지 자연스럽게 이해하도록 돕는 것
          - 길이: 3~4문장 권장
          - 문장 수를 억지로 맞추기보다 자연스러운 흐름을 우선한다
          - 첫 문장은 해당 장소의 본질적 성격부터 설명한다
          - 즉, 무엇을 하는 공간인지 먼저 말하고 그다음 반려동물 특성이나 날씨와 연결한다
          - 반드시 반려동물과 연결된 이유를 1개 이상 포함한다
          - 날씨, 안정성/혼잡도, 공간 특성 중 실제로 중요한 이유를 자연스럽게 연결한다
          - 마지막 문장은 감성 표현 없이 왜 다른 후보보다 이 장소가 더 앞섰는지를 정리한다

          [추천설명에서 category별 강조 축]
          - STAY: 숙박, 머무름, 휴식, 동반 편의, 쉬는 흐름
          - DINING: 카페/식음/브런치, 테라스, 동반 머무름, 분위기, 쉬어가기, 착석 편의
          - PLACE: 산책, 둘러보기, 이동 흐름, 개방감, 야외 동선, 활동성

          [추천설명 스타일 가이드]
          - 보호자에게 직접 설명하듯 부드럽고 자연스럽게 쓴다
          - 문장을 억지로 끊지 말고 접속어와 흐름을 살린다
          - 같은 어미를 반복하지 않는다
          - 문장 시작을 반복하지 않는다
          - "그리고", "또한", "~하며"를 연속으로 반복하지 않는다
          - 같은 뜻을 다른 말로 되풀이하지 않는다
          - "적합하다", "좋다", "추천한다", "만족스럽다", "기대할 수 있다" 같은 추상 평가 표현은 피한다
          - "점수가 높다", "감점이 적다", "가점이 있다", "우세하다" 같은 내부 판단 표현도 피한다
          - 특히 DINING은 산책 장소처럼 쓰지 않는다

          [알림요약 작성 기준]
          - 역할: 한눈에 이해되는 핵심 전달
          - 목적: 사용자가 바로 장소명과 핵심 이유를 파악하게 하는 것
          - 길이: 한 문장, 짧고 직관적으로
          - 장소명은 반드시 포함한다
          - 핵심 이유는 1개만 선명하게 잡는다
          - 추천설명을 줄여 쓰지 않는다
          - 설명문처럼 늘어놓지 않는다

          [알림요약 category 어휘 가이드]
          - STAY: 머무름, 휴식, 숙박, 동반 편의, 쉬어가기
          - DINING: 쉬어가기, 머무르기, 테라스, 동반 카페, 브런치, 식음 공간, 분위기
          - PLACE: 산책, 둘러보기, 야외 동선, 개방감, 활동 흐름

          [표현 변환 규칙]
          - 점수, 가점, 감점, 수치 표현 금지
          - "1.08점 감점", "가점이 높음", "우선순위 점수" 같은 표현 절대 금지
          - 내부 판단은 사용자 언어로 바꿔 쓴다
          - 예:
            - 감점 -> 다른 후보 대비 일부 조건에서 밀림
            - 가점 -> 특정 조건에서 우선 고려됨
            - 점수 -> 언급 금지

          [출력 형식]
          [추천설명]
          (category에 맞는 본질을 먼저 설명하고, 이후 반려동물/날씨와 연결한 자연스러운 설명문)

          [알림요약]
          (category에 맞는 어휘를 사용한 짧고 직관적인 한 문장)

          [입력]
          사용자 정보:
          %s

          반려동물 정보:
          %s

          날씨 정보:
          %s

          추천 판단 요약:
          %s

          설명 필수 근거:
          %s

          상위 장소 근거:
          %s

          추가 지침:
          %s
          """.formatted(
            userProfile,
            petProfile,
            weatherSection,
            decisionSummary,
            explanationFocus,
            topPlaceEvidence,
            supplementalGuideline
    );
  }
}
