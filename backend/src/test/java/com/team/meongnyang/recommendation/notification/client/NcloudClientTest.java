package com.team.meongnyang.recommendation.notification.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.util.NcloudSignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withAccepted;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class NcloudClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private NcloudClient ncloudClient;
    private NcloudSensProperties properties;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        properties = new NcloudSensProperties();
        properties.setBaseUrl("https://sens.apigw.ntruss.com");
        properties.setServiceId("test-service");
        properties.setAccessKey("test-access-key");
        properties.setSecretKey("test-secret-key");
        properties.setPlusFriendId("@meongnyangtrip");
        properties.setTemplateCode("PET_WEATHER_RECO_001");

        ncloudClient = new NcloudClient(restTemplate, new ObjectMapper(), properties);
    }

    @Test
    @DisplayName("알림톡 요청을 승인 템플릿 최종 본문과 templateParameter로 전송한다")
    void send() {
        NotificationRequest request = NotificationRequest.builder()
                .templateCode(properties.getTemplateCode())
                .plusFriendId(properties.getPlusFriendId())
                .messages(List.of(
                        NotificationRequest.Message.builder()
                                .to("01012345678")
                                .content("""
                                        [멍냥트립]
                                        고객님께서 설정하신 반려동물 맞춤 알림에 따라,
                                        몽실이의 오늘 반려생활을 안내드립니다.

                                        🌥 오늘 날씨 : 흐림
                                        강한 햇빛이 없어 몽실이이 비교적 편안하게 활동할 수 있습니다.

                                        📍 추천 장소 : 수원 산책공원
                                        💬 몽실이가 편하게 산책할 수 있는 장소입니다.

                                        ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
                                        등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
                                        """.trim())
                                .templateParameter(Map.of(
                                        "petName", "몽실이",
                                        "placeName", "수원 산책공원",
                                        "comment", "몽실이가 편하게 산책할 수 있는 장소입니다."
                                ))
                                .build()
                ))
                .build();

        server.expect(requestTo("https://sens.apigw.ntruss.com/alimtalk/v2/services/test-service/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("x-ncp-iam-access-key", "test-access-key"))
                .andExpect(clientHttpRequest -> {
                    String timestamp = clientHttpRequest.getHeaders().getFirst("x-ncp-apigw-timestamp");
                    String signature = clientHttpRequest.getHeaders().getFirst("x-ncp-apigw-signature-v2");

                    assertThat(timestamp).isNotBlank();
                    assertThat(signature).isEqualTo(NcloudSignatureUtil.createSignature(
                            "POST",
                            "/alimtalk/v2/services/test-service/messages",
                            timestamp,
                            "test-access-key",
                            "test-secret-key"
                    ));
                })
                .andExpect(content().json("""
                        {
                          "templateCode": "PET_WEATHER_RECO_001",
                          "plusFriendId": "@meongnyangtrip",
                          "messages": [
                            {
                              "to": "01012345678",
                              "content": "[멍냥트립]\n고객님께서 설정하신 반려동물 맞춤 알림에 따라,\n몽실이의 오늘 반려생활을 안내드립니다.\n\n🌥 오늘 날씨 : 흐림\n강한 햇빛이 없어 몽실이이 비교적 편안하게 활동할 수 있습니다.\n\n📍 추천 장소 : 수원 산책공원\n💬 몽실이가 편하게 산책할 수 있는 장소입니다.\n\n※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,\n등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.",
                              "templateParameter": {
                                "petName": "몽실이",
                                "placeName": "수원 산책공원",
                                "comment": "몽실이가 편하게 산책할 수 있는 장소입니다."
                              }
                            }
                          ]
                        }
                        """))
                .andRespond(withAccepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "requestId": "request-id",
                                  "requestTime": "2026-03-24T09:00:00.000+09:00",
                                  "statusCode": "202",
                                  "statusName": "success"
                                }
                                """));

        NotificationResponse response = ncloudClient.send(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getRequestId()).isEqualTo("request-id");
        assertThat(response.getStatusCode()).isEqualTo("202");
        assertThat(response.getStatusName()).isEqualTo("success");

        server.verify();
    }

    @Test
    @DisplayName("응답 본문이 없으면 실패 응답으로 처리한다")
    void sendWhenResponseBodyIsNull() {
        NotificationRequest request = NotificationRequest.builder()
                .templateCode(properties.getTemplateCode())
                .plusFriendId(properties.getPlusFriendId())
                .messages(List.of())
                .build();

        server.expect(requestTo("https://sens.apigw.ntruss.com/alimtalk/v2/services/test-service/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withAccepted());

        NotificationResponse response = ncloudClient.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo("202");
        assertThat(response.getStatusName()).isNotBlank();

        server.verify();
    }

    @Test
    @DisplayName("외부 API 오류 응답을 받으면 실패로 처리한다")
    void sendWhenExternalApiFails() {
        NotificationRequest request = NotificationRequest.builder()
                .templateCode(properties.getTemplateCode())
                .plusFriendId(properties.getPlusFriendId())
                .messages(List.of())
                .build();

        server.expect(requestTo("https://sens.apigw.ntruss.com/alimtalk/v2/services/test-service/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"500\",\"statusName\":\"error\"}"));

        NotificationResponse response = ncloudClient.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo("502");
        assertThat(response.getStatusName()).isNotBlank();

        server.verify();
    }
}
