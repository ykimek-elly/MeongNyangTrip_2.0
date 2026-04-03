package com.team.meongnyang.recommendation.batch;

/**
 * 추천 알림 배치 실행 중 발생할 수 있는 실패 사유를 정의한 열거형
 * 사용자 조회, 추천 생성, 알림 전송 등 각 단계에서의 실패 원인을 구분하기 위해 사용된다.
 */
public enum NotificationBatchFailureReason {
  USER_NOT_FOUND,
  PET_NOT_FOUND,
  ALREADY_SENT_TODAY,
  WEATHER_API_ERROR,
  NO_CANDIDATE,
  AI_RESPONSE_ERROR,
  NOTIFICATION_MESSAGE_BUILD_FAIL,
  NOTIFICATION_SEND_FAIL,
  UNKNOWN_ERROR
}
