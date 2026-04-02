package com.team.meongnyang.recommendation.weather.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherRuleServiceTest {

    private final WeatherRuleService weatherRuleService = new WeatherRuleService();

    @Test
    @DisplayName("비와 강풍이 함께 있으면 DANGEROUS를 반환한다")
    void evaluateWalkLevel_dangerous() {
        String walkLevel = weatherRuleService.evaluateWalkLevel(true, false, false, true);

        assertThat(walkLevel).isEqualTo("DANGEROUS");
    }

    @Test
    @DisplayName("강풍만 있으면 CAUTION을 반환한다")
    void evaluateWalkLevel_caution() {
        String walkLevel = weatherRuleService.evaluateWalkLevel(false, false, false, true);

        assertThat(walkLevel).isEqualTo("CAUTION");
    }
}
