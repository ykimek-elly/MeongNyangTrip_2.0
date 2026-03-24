package com.team.meongnyang.recommendation.batch;

public enum NotificationBatchFailureReason {
  USER_NOT_FOUND,
  PET_NOT_FOUND,
  WEATHER_API_ERROR,
  NO_CANDIDATE,
  AI_RESPONSE_ERROR,
  NOTIFICATION_MESSAGE_BUILD_FAIL,
  NOTIFICATION_SEND_FAIL,
  UNKNOWN_ERROR
}
