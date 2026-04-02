package com.team.meongnyang.recommendation.notification.dto;

public enum BatchFailureReason {
  USER_NOT_FOUND,
  PET_NOT_FOUND,
  WEATHER_API_ERROR,
  NO_CANDIDATE,
  AI_RESPONSE_ERROR,
  NOTIFICATION_MESSAGE_BUILD_FAIL,
  NOTIFICATION_SEND_FAIL,
  UNKNOWN_ERROR
}
