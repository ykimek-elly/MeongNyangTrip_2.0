package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.recommendation.notification.client.NcloudClient;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryTrackerTest {

    @Test
    @DisplayName("초기 조회에서 messageStatusCode가 없어도 재조회 후 최종 상태를 반환한다")
    void trackByRequestIdPollsUntilMessageStatusIsAvailable() {
        NcloudClient ncloudClient = mock(NcloudClient.class);
        NcloudSensProperties properties = new NcloudSensProperties();
        properties.getDelivery().setPollDelaysMs(List.of(1L, 1L));

        NotificationDeliveryResult pending = NotificationDeliveryResult.builder()
                .requestId("request-1")
                .messageId("message-1")
                .requestStatusCode("A000")
                .requestStatusDesc("성공")
                .build();

        NotificationDeliveryResult delivered = NotificationDeliveryResult.builder()
                .requestId("request-1")
                .messageId("message-1")
                .requestStatusCode("A000")
                .requestStatusDesc("성공")
                .messageStatusCode("0000")
                .messageStatusDesc("전송 완료")
                .build();

        when(ncloudClient.getDeliveryResultByRequestId("request-1"))
                .thenReturn(pending, delivered);

        NotificationDeliveryTracker tracker = new NotificationDeliveryTracker(ncloudClient, properties) {
            @Override
            void sleep(Long delayMs) {
            }
        };

        NotificationDeliveryResult result = tracker.trackByRequestId("request-1");

        assertThat(result.getMessageStatusCode()).isEqualTo("0000");
        assertThat(result.getMessageStatusDesc()).isEqualTo("전송 완료");
        verify(ncloudClient, times(2)).getDeliveryResultByRequestId("request-1");
    }

    @Test
    @DisplayName("재조회가 끝나도 최종 상태가 없으면 마지막 결과를 그대로 반환한다")
    void trackByRequestIdReturnsLastPendingResultWhenMessageStatusIsStillMissing() {
        NcloudClient ncloudClient = mock(NcloudClient.class);
        NcloudSensProperties properties = new NcloudSensProperties();
        properties.getDelivery().setPollDelaysMs(List.of(1L, 1L, 1L));

        NotificationDeliveryResult pending = NotificationDeliveryResult.builder()
                .requestId("request-2")
                .messageId("message-2")
                .requestStatusCode("A000")
                .requestStatusDesc("성공")
                .build();

        when(ncloudClient.getDeliveryResultByRequestId("request-2"))
                .thenReturn(pending, pending, pending, pending);

        NotificationDeliveryTracker tracker = new NotificationDeliveryTracker(ncloudClient, properties) {
            @Override
            void sleep(Long delayMs) {
            }
        };

        NotificationDeliveryResult result = tracker.trackByRequestId("request-2");

        assertThat(result.getMessageStatusCode()).isNull();
        assertThat(result.getRequestStatusCode()).isEqualTo("A000");
        verify(ncloudClient, times(4)).getDeliveryResultByRequestId("request-2");
    }
}
