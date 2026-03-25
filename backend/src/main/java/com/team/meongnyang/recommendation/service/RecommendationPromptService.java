package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            당신은 반려동물 동반 장소 추천 설명 도우미다.
            순위는 서버가 이미 계산했으므로 제공된 근거만 사용해 설명한다.

            [작성 규칙]
            - 한국어로만 작성한다.
            - 추천 설명은 4문장 이상 6문장 이하로 작성한다.
            - 입력에 없는 사실은 추측하지 않는다.
            - 1순위 장소의 장점은 최소 2가지 이상 설명한다.
            - 2순위와 3순위와의 차이는 짧게만 비교한다.
            - 점수 필드명은 그대로 노출하지 않는다.

            [알림 요약 규칙]
            - 한 문장, 45자 이내로 작성한다.
            - 반드시 1순위 장소명을 포함한다.
            - 반려동물 이름, 날씨, 추천 이유 중 최소 1개를 함께 반영한다.
            - 반드시 추천 이유를 포함한다.
            - 추천 이유는 거리, 날씨 적합성, 산책 편의성, 반려동물 성향 적합성 등 입력 근거 중 하나를 사용한다.
            - 반려동물 이름 또는 날씨 정보를 함께 반영한다.
            - 알림요약은 추천 권유 문장이 아니라 추천 근거를 전달하는 문장으로 작성한다.
            - "어떠세요?", "추천드려요", "가보세요", "함께해요" 같은 권유형 표현은 사용하지 않는다.
            - 추상적인 표현보다 구체적인 이유를 우선한다.
            
            [알림 요약 예시]
            - 좋은 예시:
              - 몽실이에게 수원 산책공원 1은 맑은 날 산책하기 좋아 추천됐습니다.
              - 맑은 날씨와 산책 편의성이 좋아 수원 산책공원 1이 1순위로 선정됐습니다.
              - 몽실이의 야외 산책 성향과 잘 맞아 수원 산책공원 1이 추천됐습니다.
            - 나쁜 예시:
              - 몽실이와 함께 수원 산책공원 1에서 즐거운 산책 어떠세요?
              - 오늘은 수원 산책공원 1을 추천드려요.

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

            [출력 형식]
            - [추천설명]
            4~6문장 한국어 설명

            - [알림요약]
            카카오 알림에 바로 사용할 한 문장
            """
            .formatted(
                    userProfile,
                    petProfile,
                    weatherSection,
                    decisionSummary,
                    topPlaceEvidence,
                    supplementalGuideline
            );

    log.info("[프롬프트] 컨텍스트 기반 프롬프트 생성 완료 length={}", prompt.length());
    log.debug("[프롬프트] 프롬프트 미리보기={}",
            RecommendationTextUtils.abbreviate(prompt, 240, "정보 없음"));
    return prompt;
  }
}
