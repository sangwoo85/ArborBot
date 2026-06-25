package com.tossai.trading.api.web;

import com.tossai.trading.common.error.DomainException;
import com.tossai.trading.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/** 전역 예외 처리. 응답에 내부 스택/비밀정보를 노출하지 않는다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_FAILED, ILLEGAL_STATE_TRANSITION, DUPLICATE -> HttpStatus.BAD_REQUEST;
            case RISK_REJECTED, KILL_SWITCH_ACTIVE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case BROKER_ERROR -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(body(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body(ErrorCode.VALIDATION_FAILED, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        return ResponseEntity.internalServerError()
                .body(body(null, "내부 오류가 발생했습니다."));
    }

    private Map<String, Object> body(ErrorCode code, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "code", code == null ? "INTERNAL_ERROR" : code.name(),
                "message", message == null ? "" : message
        );
    }
}
