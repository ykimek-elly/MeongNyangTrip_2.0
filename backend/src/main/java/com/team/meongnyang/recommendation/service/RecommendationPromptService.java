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

    String prompt = """
            [시스템 역할]
            당신은 반려동물 동반 장소 추천 서비스의 추천 설명 어시스턴트다.
            순위 결정은 이미 서버에서 끝났고, 당신의 역할은 그 결과를 사용자에게 자연스럽게 설명하는 것이다.
            
            [목표]
            현재 1순위 장소가 왜 지금 가장 적합한지 설득력 있게 설명하라.
            2순위와 3순위는 어떤 차이가 있는지 짧게 비교해 사용자가 선택 기준을 이해할 수 있게 하라.
            
            [작성 규칙]
            - 최종 답변은 반드시 한국어로 작성한다.
            - 답변 길이는 5문장 이상 7문장 이하로 유지한다.
            - 아래에 제공된 문맥만 사용하고 없는 사실은 추측하지 않는다.
            - 장소 메타데이터, 점수 근거, 날씨 제약, 반려동물 특성처럼 서비스가 가진 근거 중심으로 설명한다.
            - 추가 지침은 현재 추천과 직접 관련 있을 때만 반영한다.
            - 1순위 장소는 최소 두 가지 이상의 구체적인 이유로 설명한다.
            - 주의점이나 감점 요소가 중요하면 자연스럽게 함께 언급한다.
            - dogFitScore, weatherScore 같은 원시 필드명은 그대로 노출하지 않는다.
            
            [카카오 알림용 1줄 요약]
            - 45자 이내로 작성한다.
            - 한국어 한 문장으로 작성한다.
            - 반드시 1순위 장소명을 포함한다.
            - 반려동물 이름, 현재 날씨, 추천 이유 중 최소 1개를 자연스럽게 함께 포함한다.
            - 입력 문맥에 없는 날씨 표현은 사용하지 않는다.
            - '맑은 날씨', '날씨 좋은 날' 같은 표현은 실제 날씨 정보와 다르면 사용하지 않는다.
            - '딱!', '좋아요!'처럼 정보가 부족한 짧은 문장은 금지한다.
            - 바로 알림에 넣을 수 있도록 자연스럽고 완결된 문장으로 작성한다.
            - 불필요한 인사말, 이모지, 제목은 넣지 않는다.
            
            [입력 문맥]
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
            반드시 아래 형식을 그대로 지켜 작성한다.
            
            - [추천설명]
            5문장 이상 7문장 이하의 한국어 설명
            
            - [알림요약]
            카카오 알림 메시지에 바로 넣을 수 있는 45자 이내 한 문장
            
            
            [출력 예시]
            
            - [추천설명]
            오늘은 이동 부담이 적은 날씨라 반려동물과 함께 무리 없이 외출하기 좋습니다.
            1순위 장소는 산책 동선이 안정적이고 활동량을 채우기에도 적합해 가장 높은 우선순위로 선정되었습니다.
            2순위 장소는 실내라는 장점이 있지만 오늘 기준으로는 활동 만족도가 조금 낮았습니다.
            3순위 장소는 편안한 환경은 강점이지만 거리 조건에서 다소 불리했습니다.
            현재 조건에서는 1순위 장소가 가장 균형 잡힌 선택입니다.
            
            - [알림요약]
            산책 동선이 안정적이라 오늘 부담 없이 다녀오기 좋아요.
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
