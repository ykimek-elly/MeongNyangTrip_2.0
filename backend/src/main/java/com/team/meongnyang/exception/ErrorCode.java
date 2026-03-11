package com.team.meongnyang.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 공통 에러 코드 정의
 * 멍냥트립 2.0의 ApiResponse 규격({status, message, data})에 맞춰 status와 message를 관리한다.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 400 Bad Request
    BAD_REQUEST(400, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(400, "입력값이 올바르지 않습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(401, "인증이 필요합니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),

    // 403 Forbidden
    FORBIDDEN(403, "권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    PLACE_NOT_FOUND(404, "장소를 찾을 수 없습니다."),
    PET_NOT_FOUND(404, "반려동물을 찾을 수 없습니다."),
    
    // 409 Conflict
    DUPLICATE_EMAIL(409, "이미 존재하는 이메일입니다."),
    DUPLICATE_NICKNAME(409, "이미 존재하는 닉네임입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
