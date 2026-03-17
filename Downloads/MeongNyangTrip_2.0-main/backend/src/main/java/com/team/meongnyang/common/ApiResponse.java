package com.team.meongnyang.common;

/**
 * 모든 REST API의 공통 응답 규격.
 * 프론트엔드(React)와 백엔드(Spring Boot) 간 통신 포맷을 통일한다.
 *
 * @see docs/specs/core-setup.md 1장 참조
 */
public record ApiResponse<T>(
    int status,
    String message,
    T data
) {
    /** 성공 응답 (200 OK) */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 에러 응답 (GlobalExceptionHandler 연동용) */
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
