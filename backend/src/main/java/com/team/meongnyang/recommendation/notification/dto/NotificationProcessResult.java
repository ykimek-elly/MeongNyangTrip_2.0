package com.team.meongnyang.recommendation.notification.dto;

public record NotificationProcessResult(
        Long userId,
        String email,
        boolean success,
        String message
) {
  public static NotificationProcessResult success(Long userId, String email, String message) {
    return new NotificationProcessResult(userId, email, true, message);
  }

  public static NotificationProcessResult fail(Long userId, String email, String message) {
    return new NotificationProcessResult(userId, email, false, message);
  }
}
