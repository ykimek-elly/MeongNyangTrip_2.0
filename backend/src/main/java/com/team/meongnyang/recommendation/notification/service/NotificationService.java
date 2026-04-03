package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.client.NcloudClient;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryResult;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NcloudClient ncloudClient;
    private final NotificationMessageBuilder notificationMessageBuilder;
    private final NotificationDeliveryTracker notificationDeliveryTracker;

    public NotificationResponse send(User user, Place place, String comment) {
        return send(user, null, place, comment, null);
    }

    public NotificationResponse send(User user, Pet pet, Place place, String comment, String weatherType) {
        try {
            NotificationRequest request = notificationMessageBuilder.buildRequest(user, pet, place, comment, weatherType);
            NotificationRequest.Message payload = request.getMessages().get(0);

            log.info("[알림 전송] 요청 시작 userId={}, petId={}, batchExecutionId={}, placeId={}, weatherType={}, templateCode={}, buttonIncluded={}",
                    user.getUserId(),
                    pet == null ? null : pet.getPetId(),
                    RecommendationLogContext.batchExecutionId(),
                    place != null ? place.getId() : null,
                    weatherType,
                    request.getTemplateCode(),
                    payload.getButtons() != null && !payload.getButtons().isEmpty());

            NotificationResponse response = ncloudClient.send(request);

            log.info("[알림 전송] 요청 완료 userId={}, petId={}, batchExecutionId={}, placeId={}, requestId={}, success={}, statusCode={}",
                    user.getUserId(),
                    pet == null ? null : pet.getPetId(),
                    RecommendationLogContext.batchExecutionId(),
                    place != null ? place.getId() : null,
                    response.getRequestId(),
                    response.isSuccess(),
                    response.getStatusCode());

            return applyFinalDeliveryStatus(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("[알림 전송] 요청 생성 실패 userId={}, petId={}, batchExecutionId={}, placeId={}, reason={}",
                    user.getUserId(),
                    pet == null ? null : pet.getPetId(),
                    RecommendationLogContext.batchExecutionId(),
                    place != null ? place.getId() : null,
                    e.getMessage(),
                    e);
            return NotificationResponse.failure("INVALID_REQUEST", e.getMessage());
        }
    }

    private NotificationResponse applyFinalDeliveryStatus(NotificationResponse response) {
        if (response == null || !response.isSuccess()) {
            return response;
        }

        if (response.getRequestId() == null || response.getRequestId().isBlank()) {
            log.warn("[알림 전송] 전달 추적 건너뜀 batchExecutionId={}, reason={}",
                    RecommendationLogContext.batchExecutionId(),
                    "requestId 없음");
            return NotificationResponse.failure("DELIVERY_TRACKING_UNAVAILABLE", "requestId missing");
        }

        NotificationDeliveryResult deliveryResult = notificationDeliveryTracker.trackByRequestId(response.getRequestId());
        log.info("[알림 전송] 전달 결과 batchExecutionId={}, requestId={}, messageId={}, requestStatusCode={}, messageStatusCode={}, messageStatusDesc={}",
                RecommendationLogContext.batchExecutionId(),
                deliveryResult.getRequestId(),
                deliveryResult.getMessageId(),
                deliveryResult.getRequestStatusCode(),
                deliveryResult.getMessageStatusCode(),
                deliveryResult.getMessageStatusDesc());

        if (!deliveryResult.isAccepted()) {
            return NotificationResponse.failure(
                    deliveryResult.getRequestStatusCode(),
                    deliveryResult.getRequestStatusDesc()
            );
        }

        if (!deliveryResult.hasFinalMessageStatus()) {
            log.warn("[알림 전송] 최종 상태 미확정 batchExecutionId={}, requestId={}, messageId={}",
                    RecommendationLogContext.batchExecutionId(),
                    deliveryResult.getRequestId(),
                    deliveryResult.getMessageId());
            return NotificationResponse.failure("DELIVERY_PENDING", "final message status missing");
        }

        if (!"0000".equals(deliveryResult.getMessageStatusCode())) {
            return NotificationResponse.failure(
                    deliveryResult.getMessageStatusCode(),
                    deliveryResult.getMessageStatusDesc()
            );
        }

        return NotificationResponse.builder()
                .success(true)
                .requestId(response.getRequestId())
                .requestTime(response.getRequestTime())
                .statusCode(deliveryResult.getMessageStatusCode())
                .statusName(deliveryResult.getMessageStatusDesc())
                .build();
    }
}
