package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.client.NcloudClient;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryResult;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
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

            log.info("[KakaoNotification] send attempt userId={}, phone={}, placeId={}, weatherType={}, templateCode={}, templateParameter={}, buttonIncluded={}",
                    user.getUserId(),
                    payload.getTo(),
                    place != null ? place.getId() : null,
                    weatherType,
                    request.getTemplateCode(),
                    payload.getTemplateParameter(),
                    payload.getButtons() != null && !payload.getButtons().isEmpty());

            log.info("[KakaoNotification] contentPreview={}",
                    payload.getContent() == null ? null : payload.getContent().replace("\r", "\\r").replace("\n", "\\n"));

            NotificationResponse response = ncloudClient.send(request);

            log.info("[KakaoNotification] request result success={}, requestId={}, statusCode={}",
                    response.isSuccess(),
                    response.getRequestId(),
                    response.getStatusCode());

            return applyFinalDeliveryStatus(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("[KakaoNotification] request build failed userId={}, reason={}", user.getUserId(), e.getMessage(), e);
            return NotificationResponse.failure("INVALID_REQUEST", e.getMessage());
        }
    }

    private NotificationResponse applyFinalDeliveryStatus(NotificationResponse response) {
        if (response == null || !response.isSuccess()) {
            return response;
        }

        if (response.getRequestId() == null || response.getRequestId().isBlank()) {
            log.warn("[KakaoNotification] requestId missing, skip delivery tracking");
            return NotificationResponse.failure("DELIVERY_TRACKING_UNAVAILABLE", "requestId missing");
        }

        NotificationDeliveryResult deliveryResult = notificationDeliveryTracker.trackByRequestId(response.getRequestId());
        log.info("[KakaoNotification] delivery result requestId={}, messageId={}, requestStatusCode={}, requestStatusDesc={}, messageStatusCode={}, messageStatusDesc={}",
                deliveryResult.getRequestId(),
                deliveryResult.getMessageId(),
                deliveryResult.getRequestStatusCode(),
                deliveryResult.getRequestStatusDesc(),
                deliveryResult.getMessageStatusCode(),
                deliveryResult.getMessageStatusDesc());

        if (!deliveryResult.isAccepted()) {
            return NotificationResponse.failure(
                    deliveryResult.getRequestStatusCode(),
                    deliveryResult.getRequestStatusDesc()
            );
        }

        if (!deliveryResult.hasFinalMessageStatus()) {
            log.warn("[KakaoNotification] final delivery status is still pending requestId={}, messageId={}",
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
