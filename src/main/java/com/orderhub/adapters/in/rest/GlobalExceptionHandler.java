package com.orderhub.adapters.in.rest;

import com.orderhub.domain.shared.DomainError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainError.IdempotencyConflictException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyConflict(DomainError.IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "success", false,
            "error", Map.of(
                "code", "IDEMPOTENCY_CONFLICT",
                "message", e.getMessage()
            )
        ));
    }

    @ExceptionHandler(DomainError.NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(DomainError.NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "success", false,
            "error", Map.of(
                "code", "NOT_FOUND",
                "message", e.getMessage()
            )
        ));
    }

    @ExceptionHandler({DomainError.BusinessRuleViolationException.class, DomainError.InvalidArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessRule(DomainError e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "success", false,
            "error", Map.of(
                "code", e.getCode(),
                "message", e.getMessage()
            )
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "error", Map.of(
                "code", "INTERNAL_ERROR",
                "message", e.getMessage() != null ? e.getMessage() : "Erro interno do servidor"
            )
        ));
    }
}

