package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * 추천 결과를 알림 채널에 맞는 짧은 메시지 형태로 정리하는 조립기이다.
 *
 * <p>알림 서비스에서 외부 발송 직전에 호출되며,
 * 사용자 닉네임, 추천 장소, AI 코멘트를 하나의 읽기 쉬운 알림 본문으로 묶는다.
 * 생성된 결과는 카카오 알림 요청 본문으로 사용된다.
 */
@Component
public class NotificationMessageBuilder {

  /**
   * 사용자와 추천 장소 정보를 바탕으로 알림 메시지 본문을 생성한다.
   *
   * @param user 메시지 수신 대상 사용자 정보
   * @param place 알림에 포함할 추천 장소 정보
   * @param message AI가 생성한 추천 코멘트
   * @param weatherType 날씨 유형 (SUNNY, RAINY, CLOUDY, HEATWAVE, COLD_WAVE)
   * @return 외부 알림 API에 전달할 최종 메시지 본문
   */
  public String buildMessage(User user, Place place, String message, String weatherType) {
    String nickname = user != null ? user.getNickname() : "사용자";
    String placeTitle = place != null ? place.getTitle() : "추천 장소";

    String intro = buildWeatherIntro(weatherType);

    return String.format(
            "%s %s님을 위한 오늘의 추천 장소는 %s입니다. %s",
            intro,
            nickname,
            placeTitle,
            message != null ? message : "즐거운 외출 되세요!"
    );
  }

  /**
   * 날씨 유형에 따라 알림 시작 문구를 만든다.
   *
   * @param weatherType 날씨 유형
   * @return 알림 서두 문장
   */
  private String buildWeatherIntro(String weatherType) {
    if (weatherType == null) {
      return "오늘도 반려견과 좋은 하루 보내세요.";
    }

    return switch (weatherType.toUpperCase()) {
      case "SUNNY" -> "맑은 날씨네요.";
      case "RAINY" -> "비 소식이 있어요.";
      case "CLOUDY" -> "조금 흐린 날씨예요.";
      case "HEATWAVE" -> "더운 날씨가 이어지고 있어요.";
      case "COLD_WAVE" -> "기온이 많이 낮아요.";
      default -> "오늘도 반려견과 좋은 하루 보내세요.";
    };
  }
}
