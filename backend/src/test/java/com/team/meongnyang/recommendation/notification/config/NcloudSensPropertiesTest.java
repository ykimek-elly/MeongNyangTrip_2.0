package com.team.meongnyang.recommendation.notification.config;

import com.team.meongnyang.recommendation.notification.template.KakaoWeatherTemplateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NcloudSensPropertiesTest {

    @Test
    @DisplayName("날씨별 template code는 정확히 매핑되고 다른 날씨 코드로 fallback하지 않는다")
    void resolveTemplateCodeUsesExactWeatherMapping() {
        NcloudSensProperties properties = new NcloudSensProperties();
        NcloudSensProperties.Template template = new NcloudSensProperties.Template();
        template.setSunny("TPL_SUNNY");
        template.setCloudy("TPL_CLOUDY");
        template.setRain("TPL_RAIN");
        template.setHot("TPL_HOT");
        template.setCold("TPL_COLD");
        template.setDefaultCode("TPL_DEFAULT");
        properties.setTemplate(template);

        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.SUNNY)).isEqualTo("TPL_SUNNY");
        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.CLOUDY)).isEqualTo("TPL_CLOUDY");
        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.RAIN)).isEqualTo("TPL_RAIN");
        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.HOT)).isEqualTo("TPL_HOT");
        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.COLD)).isEqualTo("TPL_COLD");
    }

    @Test
    @DisplayName("특정 날씨 template code가 비어 있으면 빈 값으로 남겨 상위 계층에서 fail-fast 할 수 있다")
    void resolveTemplateCodeDoesNotFallbackAcrossWeatherTypes() {
        NcloudSensProperties properties = new NcloudSensProperties();
        NcloudSensProperties.Template template = new NcloudSensProperties.Template();
        template.setCloudy("TPL_CLOUDY");
        template.setDefaultCode("TPL_DEFAULT");
        properties.setTemplate(template);

        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.SUNNY)).isNull();
        assertThat(properties.resolveTemplateCode(KakaoWeatherTemplateType.CLOUDY)).isEqualTo("TPL_CLOUDY");
    }
}
