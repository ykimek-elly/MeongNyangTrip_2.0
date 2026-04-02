package com.team.meongnyang.recommendation.notification.config;

import com.team.meongnyang.recommendation.notification.template.KakaoWeatherTemplateType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * Ncloud SENS 알림톡 연동 설정 프로퍼티.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ncloud.sens")
public class NcloudSensProperties {

    private String baseUrl;

    private String serviceId;

    private String accessKey;

    private String secretKey;

    private String plusFriendId;

    private String templateCode;

    private String detailLinkPrefix;


    private Template template = new Template();
    private Delivery delivery = new Delivery();

    @Getter
    @Setter
    public static class Template {
        private String sunny;
        private String cloudy;
        private String cold;
        private String hot;
        private String rain;
        private String defaultCode;
    }

    @Getter
    @Setter
    public static class Delivery {
        private List<Long> pollDelaysMs = new ArrayList<>(List.of(1000L, 3000L, 5000L));
    }

    public String getMessagePath() {
        return "/alimtalk/v2/services/" + serviceId + "/messages";
    }

    public String getMessageUrl() {
        return normalizeBaseUrl(baseUrl) + getMessagePath();
    }

    public String getMessageResultPath(String messageId) {
        return getMessagePath() + "/" + messageId;
    }

    public String getMessageResultUrl(String messageId) {
        return normalizeBaseUrl(baseUrl) + getMessageResultPath(messageId);
    }

    public String getMessageListUrl(String requestId) {
        return getMessageUrl() + "?requestId=" + requestId;
    }

    public String buildDetailLink(Long placeId) {
        String normalizedPrefix = normalizeDetailLinkPrefix(detailLinkPrefix);
        if (placeId == null) {
            return normalizedPrefix;
        }
        return normalizedPrefix + placeId;
    }

    public boolean hasUsableDetailLink() {
        if (detailLinkPrefix == null || detailLinkPrefix.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(normalizeDetailLinkPrefix(detailLinkPrefix));
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return false;
            }

            if (!"https".equalsIgnoreCase(scheme)) {
                return false;
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return !"localhost".equals(normalizedHost)
                    && !"127.0.0.1".equals(normalizedHost)
                    && !"0.0.0.0".equals(normalizedHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public String resolveTemplateCode(KakaoWeatherTemplateType weatherType) {
        if (weatherType == null) {
            return resolveDefaultTemplateCode();
        }

        String templateCode = switch (weatherType) {
            case SUNNY -> template.getSunny();
            case RAIN -> template.getRain();
            case CLOUDY -> template.getCloudy();
            case HOT -> template.getHot();
            case COLD -> template.getCold();
        };
        if (templateCode != null && !templateCode.isBlank()) {
            return templateCode;
        }
        return resolveDefaultTemplateCode();
    }

    public String resolveTemplateCode(String weatherType) {
        return resolveTemplateCode(KakaoWeatherTemplateType.from(weatherType));
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeDetailLinkPrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value : value + "/";
    }

    private String resolveDefaultTemplateCode() {
        if (template != null && template.getDefaultCode() != null && !template.getDefaultCode().isBlank()) {
            return template.getDefaultCode();
        }
        return templateCode;
    }
}
