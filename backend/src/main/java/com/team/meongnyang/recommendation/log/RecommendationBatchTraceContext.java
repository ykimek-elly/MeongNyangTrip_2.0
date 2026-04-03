package com.team.meongnyang.recommendation.log;

import org.slf4j.MDC;

import java.util.Map;

/**
 * 배치 실행 중 로그 추적을 위한 MDC 컨텍스트를 관리하는 유틸 클래스
 * batchExecutionId, userId, petId를 MDC에 설정하여 로그 추적성을 높인다.
 */
public final class RecommendationBatchTraceContext {

    private RecommendationBatchTraceContext() {
    }

    /**
     * MDC에 배치 실행 컨텍스트를 설정하고 기존 상태를 보존한다.
     */
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

    /**
     * MDC 컨텍스트를 복원하는 스코프 객체
     * try-with-resources 구문에서 사용되어 자동으로 이전 상태로 복구된다.
     */
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
