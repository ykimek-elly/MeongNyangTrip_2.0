package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.client.KakaoNotificationClient;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 추천 결과를 카카오 알림 발송 요청으로 변환해 외부 알림 채널에 전달하는 서비스이다.
 *
 * <p>추천 문장 생성 이후 별도 알림 단계에서 호출되며,
 * 사용자, 추천 장소, AI 코멘트를 조합한 메시지를 만들고 실제 발송 클라이언트 호출을 담당한다.
 * 결과는 알림 성공 여부 확인과 후속 운영 모니터링에 활용된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoNotificationService {
    private final KakaoNotificationClient kakaoNotificationClient;
    private final NotificationMessageBuilder messageBuilder;

    /**
     * 추천 결과를 카카오 알림 요청으로 조합해 발송한다.
     *
     * @param user 알림 수신 대상 사용자 정보
     * @param place 알림에 포함할 추천 장소 정보
     * @param message 추천 근거가 담긴 AI 코멘트
     * @return 외부 알림 발송 결과 코드와 성공 여부
     */
    public NotificationResponse send (User user, Place place, String message) {
        String title = "오늘의 추천";
        String phone = user.getPhoneNumber();

        NotificationRequest req = NotificationRequest.builder()
                .phoneNumber(phone)
                .templateCode("T0001")
                .senderKey("sender_key")
                .title(title)
                .message(message)
                .build();

        log.info("[카카오 알림] 발송 시도 userId={}, placeTitle={}",
              user.getUserId(),
              place.getTitle());

        NotificationResponse resp = kakaoNotificationClient.send(req);

        log.info("[카카오 알림] 발송 결과  success={}, code={}, message={}",
                resp.isSuccess(),
                resp.getCode(),
                resp.getMessage());
        return resp;
    }
}
