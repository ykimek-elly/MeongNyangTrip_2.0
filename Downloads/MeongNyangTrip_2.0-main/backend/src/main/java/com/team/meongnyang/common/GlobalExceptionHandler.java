package com.team.meongnyang.common;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;

/**
 * 전역 예외 처리 핸들러.
 * 모든 예외를 ApiResponse 규격으로 변환하여 프론트엔드에 일관된 응답을 제공한다.
 *
 * @see docs/specs/core-setup.md 4.2장 참조
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 로직 예외 (BusinessException) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity
            .status(ec.getStatus())
            .body(ApiResponse.error(ec.getStatus(), e.getMessage()));
    }

    /** DB 관련 예외 (DataAccessException) */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "데이터베이스 오류 발생"));
    }

    /** 리소스를 찾을 수 없는 경우 */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NoSuchElementException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, e.getMessage()));
    }

    /** 유효성 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("입력값 검증에 실패했습니다.");

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, message));
    }

    /** 잘못된 인수 (IllegalArgumentException) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, e.getMessage()));
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."));
    }
}
