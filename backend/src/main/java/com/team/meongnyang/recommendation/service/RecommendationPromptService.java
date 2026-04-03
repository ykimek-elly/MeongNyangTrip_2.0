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
            ? RecommendationTextUtils.defaultIfBlank(context.getTopPlaceEvidenceSection(), "추천 근거 없음")
            : "추천 근거 없음";
    String supplementalGuideline = context != null
            ? RecommendationTextUtils.defaultIfBlank(context.getSupplementalGuidelineSection(), "추가 지침 없음")
            : "추가 지침 없음";

    explanationFocus = RecommendationTextUtils.abbreviate(explanationFocus, 500, "설명 필수 근거 없음");
    topPlaceEvidence = RecommendationTextUtils.abbreviate(topPlaceEvidence, 700, "추천 근거 없음");
    supplementalGuideline = RecommendationTextUtils.abbreviate(supplementalGuideline, 220, "추가 지침 없음");

    return """
          [역할]
          당신은 반려동물 동반 장소 추천 설명 생성기다.
          순위는 이미 계산되어 있으니 입력으로 준 근거만 사용한다.

          [절대 규칙]
          - 입력에 없는 사실은 만들지 않는다.
          - 출력 형식은 반드시 지킨다.
          - 설명 필수 근거 섹션의 문장을 우선 사용한다.
          - 점수 항목명만 반복하지 말고 실제 이용 상황 문장으로 풀어 쓴다.

          [추천 설명 작성 규칙]
          - 총 4~6문장으로 작성
          - 1위 장소의 boost 근거를 최소 2개 이상 반드시 반영
          - 날씨 관련 근거를 최소 1개 반드시 포함
          - 반려동물 관련 boost가 설명 필수 근거에 있으면 반드시 포함
          - 각 근거는 "왜 이 사용자/반려동물에게 맞는지"가 드러나는 인과 문장으로 작성
          - "좋습니다", "추천드립니다", "적합합니다", "가보세요", "즐거운", "행복한" 같은 단순 권유 표현 금지
          - "야외 공간에서 산책하기 좋습니다"처럼 추상적이고 일반적인 문장 금지
          - 2위, 3위와 비교해 1위가 앞선 이유를 마지막 문장에 짧게 포함

          [문장 구성 가이드]
          1문장: 1위 장소의 핵심 판단
          2~3문장: 설명 필수 근거 중 서로 다른 boost 2개 이상을 풀어서 설명
          4문장: 날씨 요소가 어떻게 반영됐는지 설명
          마지막 문장: 2위, 3위 대비 우위 또는 감점 차이 설명

          [알림 요약 작성 규칙]
          - 한 문장
          - 45자 이내
          - 1위 장소명 포함
          - 구체적인 이유 1개 이상 포함
          - 날씨 또는 반려동물 관련 요소 1개 포함
          - 권유형 표현 금지

          [출력 형식]
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
