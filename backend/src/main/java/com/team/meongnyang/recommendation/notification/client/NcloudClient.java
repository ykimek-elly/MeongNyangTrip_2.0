package com.team.meongnyang.recommendation.notification.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryResult;
import com.team.meongnyang.recommendation.notification.dto.NotificationDeliveryListResponse;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 카카오 알림 외부 연동 전용 클라이언트.
 *
 * 현재는 실제 API 연동 전 단계이므로 mock 형태로 동작한다.
 * 이후 RestClient 또는 WebClient를 사용해 실제 카카오 API 호출로 교체한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NcloudClient {

    private static final String TIMESTAMP_HEADER = "x-ncp-apigw-timestamp";
    private static final String ACCESS_KEY_HEADER = "x-ncp-iam-access-key";
    private static final String SIGNATURE_HEADER = "x-ncp-apigw-signature-v2";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NcloudSensProperties ncloudSensProperties;

    public NotificationResponse send(NotificationRequest request) {
        if (!isConfigurationReady()) {
            log.warn("[알림 전송] SENS 설정 누락 batchExecutionId={}",
                    RecommendationLogContext.batchExecutionId());
            return NotificationResponse.failure("CONFIG_ERROR", "Ncloud SENS 설정값이 누락되었습니다.");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String requestPath = ncloudSensProperties.getMessagePath();
        String requestUrl = ncloudSensProperties.getMessageUrl();

        try {
            HttpEntity<NotificationRequest> httpEntity = buildHttpEntity(request, timestamp);
            ResponseEntity<NotificationResponse> responseEntity = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    httpEntity,
                    NotificationResponse.class
            );

            NotificationResponse responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("[에러] 알림 API 응답 없음 batchExecutionId={}, path={}, status={}",
                        RecommendationLogContext.batchExecutionId(),
                        requestPath,
                        responseEntity.getStatusCode());
                return NotificationResponse.failure(String.valueOf(responseEntity.getStatusCode().value()), "응답 본문이 비어 있습니다.");
            }

            return NotificationResponse.builder()
                    .success(isSuccessful(responseBody, responseEntity.getStatusCode().is2xxSuccessful()))
                    .requestId(responseBody.getRequestId())
                    .requestTime(responseBody.getRequestTime())
                    .statusCode(responseBody.getStatusCode())
                    .statusName(responseBody.getStatusName())
                    .build();
        } catch (HttpStatusCodeException e) {
            log.error("[에러] 알림 API 호출 실패 batchExecutionId={}, path={}, status={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e.getStatusCode(),
                    e);
            return NotificationResponse.failure(String.valueOf(e.getStatusCode().value()), "알림톡 API 호출에 실패했습니다.");
        } catch (RestClientException e) {
            log.error("[에러] 알림 API 클라이언트 오류 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return NotificationResponse.failure("CLIENT_ERROR", "알림톡 API 클라이언트 오류");
        } catch (Exception e) {
            log.error("[에러] 알림 API 처리 실패 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return NotificationResponse.failure("UNKNOWN_ERROR", "알림톡 요청 처리 중 오류");
        }
    }

    public NotificationDeliveryResult getDeliveryResult(String messageId) {
        if (!isConfigurationReady()) {
            log.warn("[알림 전송] 결과 조회 중단 batchExecutionId={}, reason={}",
                    RecommendationLogContext.batchExecutionId(),
                    "SENS 설정 누락");
            return NotificationDeliveryResult.failure("CONFIG_ERROR", "Ncloud SENS 설정값이 누락되었습니다.");
        }

        if (!StringUtils.hasText(messageId)) {
            return NotificationDeliveryResult.failure("INVALID_MESSAGE_ID", "messageId가 없습니다.");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String requestPath = ncloudSensProperties.getMessageResultPath(messageId);
        String requestUrl = ncloudSensProperties.getMessageResultUrl(messageId);

        try {
            HttpEntity<Void> httpEntity = new HttpEntity<>(createHeaders(HttpMethod.GET, requestPath, timestamp));
            ResponseEntity<NotificationDeliveryResult> responseEntity = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    httpEntity,
                    NotificationDeliveryResult.class
            );

            NotificationDeliveryResult responseBody = responseEntity.getBody();
            if (responseBody == null) {
                return NotificationDeliveryResult.failure(
                        String.valueOf(responseEntity.getStatusCode().value()),
                        "결과 조회 응답 본문이 비어 있습니다."
                );
            }

            return NotificationDeliveryResult.builder()
                    .success(responseEntity.getStatusCode().is2xxSuccessful())
                    .requestId(responseBody.getRequestId())
                    .messageId(responseBody.getMessageId())
                    .requestTime(responseBody.getRequestTime())
                    .completeTime(responseBody.getCompleteTime())
                    .plusFriendId(responseBody.getPlusFriendId())
                    .templateCode(responseBody.getTemplateCode())
                    .to(responseBody.getTo())
                    .content(responseBody.getContent())
                    .requestStatusCode(responseBody.getRequestStatusCode())
                    .requestStatusName(responseBody.getRequestStatusName())
                    .requestStatusDesc(responseBody.getRequestStatusDesc())
                    .messageStatusCode(responseBody.getMessageStatusCode())
                    .messageStatusName(responseBody.getMessageStatusName())
                    .messageStatusDesc(responseBody.getMessageStatusDesc())
                    .build();
        } catch (HttpStatusCodeException e) {
            log.error("[에러] 알림 결과 조회 실패 batchExecutionId={}, path={}, status={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e.getStatusCode(),
                    e);
            return NotificationDeliveryResult.failure(String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("[에러] 알림 결과 조회 클라이언트 오류 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return NotificationDeliveryResult.failure("CLIENT_ERROR", "결과 조회 중 클라이언트 오류");
        } catch (Exception e) {
            log.error("[에러] 알림 결과 조회 실패 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return NotificationDeliveryResult.failure("UNKNOWN_ERROR", "결과 조회 중 예외");
        }
    }

    public NotificationDeliveryResult getDeliveryResultByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return NotificationDeliveryResult.failure("INVALID_REQUEST_ID", "requestId가 없습니다.");
        }

        String messageId = findMessageIdByRequestId(requestId);
        if (!StringUtils.hasText(messageId)) {
            return NotificationDeliveryResult.failure("MESSAGE_ID_NOT_FOUND", "requestId에 해당하는 messageId를 찾지 못했습니다.");
        }

        return getDeliveryResult(messageId);
    }

    public String findMessageIdByRequestId(String requestId) {
        if (!isConfigurationReady()) {
            log.warn("[알림 전송] 목록 조회 중단 batchExecutionId={}, reason={}",
                    RecommendationLogContext.batchExecutionId(),
                    "SENS 설정 누락");
            return null;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String requestPath = ncloudSensProperties.getMessagePath() + "?requestId=" + requestId;
        String requestUrl = ncloudSensProperties.getMessageListUrl(requestId);

        try {
            HttpEntity<Void> httpEntity = new HttpEntity<>(createHeaders(HttpMethod.GET, requestPath, timestamp));
            ResponseEntity<NotificationDeliveryListResponse> responseEntity = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    httpEntity,
                    NotificationDeliveryListResponse.class
            );

            NotificationDeliveryListResponse responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.warn("[알림 전송] 메시지 목록 비어 있음 batchExecutionId={}, requestId={}",
                        RecommendationLogContext.batchExecutionId(),
                        requestId);
                return null;
            }

            List<NotificationDeliveryListResponse.DeliveryMessage> messages = responseBody.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.warn("[알림 전송] 메시지 목록 없음 batchExecutionId={}, requestId={}",
                        RecommendationLogContext.batchExecutionId(),
                        requestId);
                return null;
            }

            String messageId = messages.get(0).getMessageId();
            return messageId;
        } catch (HttpStatusCodeException e) {
            log.error("[에러] 알림 목록 조회 실패 batchExecutionId={}, path={}, status={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e.getStatusCode(),
                    e);
            return null;
        } catch (RestClientException e) {
            log.error("[에러] 알림 목록 조회 클라이언트 오류 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return null;
        } catch (Exception e) {
            log.error("[에러] 알림 목록 조회 실패 batchExecutionId={}, path={}",
                    RecommendationLogContext.batchExecutionId(),
                    requestPath,
                    e);
            return null;
        }
    }

    HttpEntity<NotificationRequest> buildHttpEntity(NotificationRequest request, String timestamp) {
        HttpHeaders headers = createHeaders(HttpMethod.POST, ncloudSensProperties.getMessagePath(), timestamp);
        return new HttpEntity<>(request, headers);
    }

    HttpHeaders createHeaders(HttpMethod method, String requestPath, String timestamp) {
        String signature = createSignature(
                method.name(),
                requestPath,
                timestamp,
                ncloudSensProperties.getAccessKey(),
                ncloudSensProperties.getSecretKey()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TIMESTAMP_HEADER, timestamp);
        headers.set(ACCESS_KEY_HEADER, ncloudSensProperties.getAccessKey());
        headers.set(SIGNATURE_HEADER, signature);
        return headers;
    }

    private boolean isSuccessful(NotificationResponse responseBody, boolean httpSuccess) {
        if (!httpSuccess || responseBody == null || responseBody.getStatusCode() == null) {
            return false;
        }
        return responseBody.getStatusCode().startsWith("20");
    }

    private boolean isConfigurationReady() {
        return StringUtils.hasText(ncloudSensProperties.getBaseUrl())
                && StringUtils.hasText(ncloudSensProperties.getServiceId())
                && StringUtils.hasText(ncloudSensProperties.getAccessKey())
                && StringUtils.hasText(ncloudSensProperties.getSecretKey());
    }

    public String createSignature(String method, String urlPath, String timestamp, String accessKey, String secretKey) {
        try {
            String space = " ";
            String newLine = "\n";
            String message = method + space + urlPath + newLine + timestamp + newLine + accessKey;

            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Ncloud SENS 서명 생성에 실패했습니다.", e);
        }
    }
}
