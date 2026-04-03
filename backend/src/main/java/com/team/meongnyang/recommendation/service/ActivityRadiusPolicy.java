package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ActivityRadiusPolicy {

    static final int MIN_RADIUS_KM = 3;
    static final int DEFAULT_RADIUS_KM = 15;
    static final int WIDE_RADIUS_KM = 30;
    static final int MAX_RADIUS_KM = 50;
    private static final int METERS_PER_KM = 1000;

    /**
     * 사용자 activityRadius를 추천 조회 반경 정책으로 정규화한다.
     *
     * <p>null 또는 비정상 값은 기본 반경으로 보정하고,
     * 후보가 없을 때 사용할 확장 반경 단계도 함께 계산한다.
     *
     * @param user 추천 대상 사용자
     * @return 요청값, 실제 적용값, meter 변환값, fallback 단계가 포함된 반경 계획
     */
    public SearchRadiusPlan resolve(User user) {
        Integer requestedRadiusKm = user == null ? null : user.getActivityRadius();
        int appliedRadiusKm = normalize(requestedRadiusKm);

        Set<Integer> fallbackSteps = new LinkedHashSet<>();
        fallbackSteps.add(appliedRadiusKm);
        if (appliedRadiusKm < DEFAULT_RADIUS_KM) {
            fallbackSteps.add(DEFAULT_RADIUS_KM);
        }
        if (appliedRadiusKm < WIDE_RADIUS_KM) {
            fallbackSteps.add(WIDE_RADIUS_KM);
        }
        if (appliedRadiusKm < MAX_RADIUS_KM) {
            fallbackSteps.add(MAX_RADIUS_KM);
        }

        return new SearchRadiusPlan(
                requestedRadiusKm,
                appliedRadiusKm,
                toMeters(appliedRadiusKm),
                new ArrayList<>(fallbackSteps)
        );
    }

    /**
     * 사용자 activityRadius를 최소/최대 허용 범위 내 값으로 정규화한다.
     *
     * @param requestedRadiusKm 사용자 원본 반경(km)
     * @return 실제 적용할 반경(km)
     */
    int normalize(Integer requestedRadiusKm) {
        if (requestedRadiusKm == null) {
            return DEFAULT_RADIUS_KM;
        }
        return Math.max(MIN_RADIUS_KM, Math.min(MAX_RADIUS_KM, requestedRadiusKm));
    }

    /**
     * km 단위 반경을 PostGIS ST_DWithin/ST_Distance에서 사용하는 meter 단위로 변환한다.
     *
     * @param radiusKm 반경(km)
     * @return 반경(m)
     */
    int toMeters(int radiusKm) {
        return radiusKm * METERS_PER_KM;
    }

    public record SearchRadiusPlan(
            Integer requestedRadiusKm,
            int appliedRadiusKm,
            int appliedRadiusMeters,
            List<Integer> fallbackRadiusStepsKm
    ) {
    }
}
