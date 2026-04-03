package com.team.meongnyang.recommendation.cache;

import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 캐시 종류별 TTL 정책을 한 곳에서 관리한다.
 *
 * 운영 단계에서는 데이터 특성에 따라 TTL이 달라져야 하므로
 * Weather, Recommendation, Gemini, 알림 sent marker의 수명을 이 클래스에서 계산한다.
 */
@Component
public class RecommendationCachePolicy {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Value("${redis.weather-cache-ttl-stable:30m}")
    private Duration stableWeatherTtl;

    @Value("${redis.weather-cache-ttl-severe:10m}")
    private Duration severeWeatherTtl;

    @Value("${redis.recommendation-cache-ttl:10m}")
    private Duration recommendationCacheTtl;

    @Value("${redis.recommendation-request-lock-ttl:2m}")
    private Duration recommendationRequestLockTtl;

    @Value("${redis.recommendation-history-ttl:72h}")
    private Duration recommendationHistoryTtl;

    @Value("${redis.gemini-context-cache-ttl:12h}")
    private Duration geminiContextCacheTtl;

    /**
     * 날씨 상태에 따라 날씨 캐시 TTL을 계산한다.
     *
     * @param weatherContext 정규화된 날씨 정보
     * @return 적용할 TTL
     */
    public Duration weatherTtl(WeatherContext weatherContext) {
        if (weatherContext == null) {
            return stableWeatherTtl;
        }
        if (weatherContext.isRaining() || weatherContext.isWindy() || weatherContext.isHot() || weatherContext.isCold()) {
            return severeWeatherTtl;
        }
        return stableWeatherTtl;
    }

    /**
     * 추천 결과 캐시 TTL을 반환한다.
     *
     * @return 추천 결과 TTL
     */
    public Duration recommendationResultTtl() {
        return recommendationCacheTtl;
    }

    /**
     * 짧은 시간 내 중복 요청 방지용 락 TTL을 반환한다.
     *
     * @return 요청 락 TTL
     */
    public Duration recommendationRequestLockTtl() {
        return recommendationRequestLockTtl;
    }

    /**
     * 최근 추천 이력 TTL을 반환한다.
     *
     * @return 추천 이력 TTL
     */
    public Duration recommendationHistoryTtl() {
        return recommendationHistoryTtl;
    }

    /**
     * Gemini 컨텍스트 재사용 캐시 TTL을 반환한다.
     *
     * @return 컨텍스트 캐시 TTL
     */
    public Duration geminiContextCacheTtl() {
        return geminiContextCacheTtl;
    }

    /**
     * 다음날 자정 이후 1시간까지 유지되는 TTL을 계산한다.
     *
     * @param now 기준 시각
     * @return 오늘자 데이터용 TTL
     */
    public Duration ttlUntilTomorrow(LocalDateTime now) {
        LocalDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now.atZone(SEOUL_ZONE), nextDay.atZone(SEOUL_ZONE)).plusHours(1);
    }
}
