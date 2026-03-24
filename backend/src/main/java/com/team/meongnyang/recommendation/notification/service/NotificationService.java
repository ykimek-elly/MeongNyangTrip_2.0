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

            log.info("[카카오 알림톡] 발송 시도 userId={}, phone={}, placeId={}, weatherType={}, templateCode={}, templateParameter={}, buttonIncluded={}",
                    user.getUserId(),
                    payload.getTo(),
                    place != null ? place.getId() : null,
                    weatherType,
                    request.getTemplateCode(),
                    payload.getTemplateParameter(),
                    payload.getButtons() != null && !payload.getButtons().isEmpty());

            NotificationResponse response = ncloudClient.send(request);

            log.info("[카카오 알림톡] 발송 결과 success={}, requestId={}, statusCode={}",
                    response.isSuccess(),
                    response.getRequestId(),
                    response.getStatusCode());

            logDeliveryResult(response);
            return response;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("[카카오 알림톡] 요청 생성 실패 userId={}, reason={}", user.getUserId(), e.getMessage(), e);
            return NotificationResponse.failure("INVALID_REQUEST", e.getMessage());
        }
    }

    private void logDeliveryResult(NotificationResponse response) {
        if (response == null || response.getRequestId() == null || response.getRequestId().isBlank()) {
            log.warn("[카카오 알림톡] requestId가 없어 최종 결과 조회를 건너뜁니다.");
            return;
        }

        NotificationDeliveryResult deliveryResult = notificationDeliveryTracker.trackByRequestId(response.getRequestId());
        log.info("[카카오 알림톡] 최종 결과 requestId={}, messageId={}, requestStatusCode={}, requestStatusDesc={}, messageStatusCode={}, messageStatusDesc={}",
                deliveryResult.getRequestId(),
                deliveryResult.getMessageId(),
                deliveryResult.getRequestStatusCode(),
                deliveryResult.getRequestStatusDesc(),
                deliveryResult.getMessageStatusCode(),
                deliveryResult.getMessageStatusDesc());

        if (deliveryResult.isAccepted() && !deliveryResult.hasFinalMessageStatus()) {
            log.warn("[카카오 알림톡] SENS 접수는 성공했지만 카카오 최종 상태는 아직 미확정입니다. requestId={}, messageId={}",
                    deliveryResult.getRequestId(),
                    deliveryResult.getMessageId());
        }
    }
}
