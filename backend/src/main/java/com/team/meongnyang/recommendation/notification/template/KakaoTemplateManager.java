package com.team.meongnyang.recommendation.notification.template;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.user.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoTemplateManager {

    private static final String PET_NAME_KEY = "petName";
    private static final String PLACE_NAME_KEY = "placeName";
    private static final String COMMENT_KEY = "comment";

    private static final String PLACEHOLDER_PET_NAME = "#{petName}";
    private static final String PLACEHOLDER_PLACE_NAME = "#{placeName}";
    private static final String PLACEHOLDER_COMMENT = "#{comment}";

    private static final String DEFAULT_PET_NAME = "반려동물";
    private static final String DEFAULT_PLACE_NAME = "추천 장소";
    private static final String DEFAULT_COMMENT = "멍냥트립이 오늘의 추천 장소를 준비했어요.";


    private final NcloudSensProperties ncloudSensProperties;

    // 외부 weatherType 문자열을 내부 템플릿 타입으로 정규화한다.
    public KakaoWeatherTemplateType resolveWeatherType(String weatherType) {
        return KakaoWeatherTemplateType.from(weatherType);
    }

    // 날씨 타입에 맞는 SENS templateCode를 조회한다.
    public String getTemplateCode(KakaoWeatherTemplateType weatherType) {
        String templateCode = ncloudSensProperties.resolveTemplateCode(weatherType);
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalStateException("Ncloud SENS templateCode가 비어 있습니다. weatherType=" + weatherType);
        }
        return templateCode;
    }

    // SENS 치환 변수는 petName, placeName, comment 세 개만 사용한다.
    public Map<String, String> createTemplateParameter(Pet pet, Place place, String comment) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(PET_NAME_KEY, defaultIfBlank(pet != null ? pet.getPetName() : null, DEFAULT_PET_NAME));
        parameters.put(PLACE_NAME_KEY, defaultIfBlank(place != null ? place.getTitle() : null, DEFAULT_PLACE_NAME));
        parameters.put(COMMENT_KEY, sanitizeComment(comment));
        return parameters;
    }

    // 승인된 원문 템플릿에 실제 값을 치환해 content를 만든다.
    public String createContent(KakaoWeatherTemplateType weatherType, Map<String, String> templateParameter) {
        return getTemplateBody(weatherType)
                .replace(PLACEHOLDER_PET_NAME, templateParameter.get(PET_NAME_KEY))
                .replace(PLACEHOLDER_PLACE_NAME, templateParameter.get(PLACE_NAME_KEY))
                .replace(PLACEHOLDER_COMMENT, templateParameter.get(COMMENT_KEY))
                .trim();
    }

    // 템플릿 원문은 한 곳에서만 관리한다.
    public String getTemplateBody(KakaoWeatherTemplateType weatherType) {
        return switch (weatherType) {
            case SUNNY -> KakaoWeatherTemplateType.SUNNY_TEMPLATE;
            case CLOUDY -> KakaoWeatherTemplateType.CLOUDY_TEMPLATE;
            case RAIN -> KakaoWeatherTemplateType.RAIN_TEMPLATE;
            case HOT -> KakaoWeatherTemplateType.HOT_TEMPLATE;
            case COLD -> KakaoWeatherTemplateType.COLD_TEMPLATE;
        };
    }

    // comment는 줄바꿈을 공백으로 정리해 템플릿 한 줄 문장으로 맞춘다.
    private String sanitizeComment(String comment) {
        String normalized = defaultIfBlank(comment, DEFAULT_COMMENT)
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return normalized.replaceAll("\\s{2,}", " ");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
