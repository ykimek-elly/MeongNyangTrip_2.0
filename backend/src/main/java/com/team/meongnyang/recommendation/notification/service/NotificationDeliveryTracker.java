package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.recommendation.notification.client.NcloudClient;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryResult;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryTracker {

    private final NcloudClient ncloudClient;
    private final NcloudSensProperties ncloudSensProperties;

    public NotificationDeliveryResult trackByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            log.warn("[알림 전송] 전달 추적 건너뜀 batchExecutionId={}, reason={}",
                    RecommendationLogContext.batchExecutionId(),
                    "requestId 없음");
            return NotificationDeliveryResult.failure("INVALID_REQUEST_ID", "requestId가 비어 있습니다.");
        }

        NotificationDeliveryResult latestResult = ncloudClient.getDeliveryResultByRequestId(requestId);
        logAttempt(requestId, 0, latestResult);

        if (latestResult.hasFinalMessageStatus() || !latestResult.isAccepted()) {
            return latestResult;
        }

        int attempt = 1;
        for (Long delayMs : ncloudSensProperties.getDelivery().getPollDelaysMs()) {
            sleep(delayMs);
            latestResult = ncloudClient.getDeliveryResultByRequestId(requestId);
            logAttempt(requestId, attempt, latestResult);

            if (latestResult.hasFinalMessageStatus() || !latestResult.isAccepted()) {
                return latestResult;
            }
            attempt++;
        }

        log.warn("[알림 전송] 최종 상태 미확정 batchExecutionId={}, requestId={}, requestStatusCode={}, messageId={}",
                RecommendationLogContext.batchExecutionId(),
                latestResult.getRequestId(),
                latestResult.getRequestStatusCode(),
                latestResult.getMessageId());
        return latestResult;
    }

    void sleep(Long delayMs) {
        if (delayMs == null || delayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("알림톡 전달 상태 재조회 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private void logAttempt(String requestId, int attempt, NotificationDeliveryResult result) {
        log.info("[알림 전송] 전달 상태 조회 batchExecutionId={}, requestId={}, attempt={}, messageId={}, requestStatusCode={}, messageStatusCode={}, messageStatusDesc={}",
                RecommendationLogContext.batchExecutionId(),
                requestId,
                attempt,
                result.getMessageId(),
                result.getRequestStatusCode(),
                result.getMessageStatusCode(),
                result.getMessageStatusDesc());
    }
}
