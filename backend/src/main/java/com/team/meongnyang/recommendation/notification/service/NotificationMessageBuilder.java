package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.template.KakaoTemplateManager;
import com.team.meongnyang.recommendation.notification.template.KakaoWeatherTemplateType;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMessageBuilder {

    private final KakaoTemplateManager kakaoTemplateManager;
    private final NcloudSensProperties ncloudSensProperties;

    public NotificationRequest buildRequest(User user, Pet pet, Place place, String comment, String weatherType) {
        KakaoWeatherTemplateType resolvedWeatherType = kakaoTemplateManager.resolveWeatherType(weatherType);
        String plusFriendId = requireText(ncloudSensProperties.getPlusFriendId(), "plusFriendId");
        String phoneNumber = normalizeMobileNumber(user.getPhoneNumber());

        // 템플릿 변수와 최종 본문은 모두 매니저에서 일관되게 만든다.
        Map<String, String> templateParameter = kakaoTemplateManager.createTemplateParameter(pet, place, comment);
        String finalContent = kakaoTemplateManager.createContent(resolvedWeatherType, templateParameter);
        String templateCode = kakaoTemplateManager.getTemplateCode(resolvedWeatherType);

        NotificationRequest.Message message = NotificationRequest.Message.builder()
                .to(phoneNumber)
                .content(finalContent)
                .templateParameter(templateParameter)
                .build();

        return NotificationRequest.builder()
                .templateCode(templateCode)
                .plusFriendId(plusFriendId.trim())
                .messages(List.of(message))
                .build();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Ncloud SENS 설정값이 비어 있습니다. field=" + fieldName);
        }
        return value;
    }

    // 알림톡 전송 직전에 수신 번호를 숫자 형태로 정규화한다.
    public String normalizeMobileNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("휴대폰 번호가 비어 있습니다.");
        }

        String digits = rawPhoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }

        if (digits.length() == 10 && digits.startsWith("10")) {
            digits = "0" + digits;
        }

        if (!digits.matches("^01[016789]\\d{7,8}$")) {
            throw new IllegalArgumentException("유효하지 않은 휴대폰 번호 형식입니다. phone=" + rawPhoneNumber);
        }
        return digits;
    }
}
