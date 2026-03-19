package com.team.meongnyang.recommendation.log;

import org.slf4j.MDC;

import java.util.Map;

public final class RecommendationBatchTraceContext {

    private RecommendationBatchTraceContext() {
    }

    public static TraceScope open(String batchExecutionId, Long userId, Long petId) {
        Map<String, String> previous = MDC.getCopyOfContextMap();

        if (batchExecutionId != null && !batchExecutionId.isBlank()) {
            MDC.put("batchExecutionId", batchExecutionId);
        }
        if (userId != null) {
            MDC.put("userId", String.valueOf(userId));
        }
        if (petId != null) {
            MDC.put("petId", String.valueOf(petId));
        }

        return new TraceScope(previous);
    }

    public static final class TraceScope implements AutoCloseable {
        private final Map<String, String> previous;

        private TraceScope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            MDC.clear();
            if (previous != null && !previous.isEmpty()) {
                MDC.setContextMap(previous);
            }
        }
    }
}
