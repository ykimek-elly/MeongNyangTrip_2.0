package com.team.meongnyang.recommendation.log;

import org.slf4j.MDC;

public final class RecommendationLogContext {

    private RecommendationLogContext() {
    }

    public static String batchExecutionId() {
        return MDC.get("batchExecutionId");
    }

    public static String userId() {
        return MDC.get("userId");
    }

    public static String petId() {
        return MDC.get("petId");
    }
}
