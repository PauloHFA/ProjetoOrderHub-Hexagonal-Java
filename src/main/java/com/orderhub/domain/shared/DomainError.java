package com.orderhub.domain.shared;

public class DomainError extends RuntimeException {
    private final String code;

    public DomainError(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static class InvalidArgumentException extends DomainError {
        public InvalidArgumentException(String message) {
            super(message, "INVALID_ARGUMENT");
        }
    }

    public static class BusinessRuleViolationException extends DomainError {
        public BusinessRuleViolationException(String message) {
            super(message, "BUSINESS_RULE_VIOLATION");
        }
    }

    public static class NotFoundException extends DomainError {
        public NotFoundException(String message) {
            super(message, "NOT_FOUND");
        }

        public static NotFoundException paraRecurso(String recurso, String id) {
            return new NotFoundException(recurso + " não encontrado: " + id);
        }
    }

    public static class ConcurrencyException extends DomainError {
        public ConcurrencyException(String message) {
            super(message, "CONCURRENCY_ERROR");
        }
    }

    public static class IdempotencyConflictException extends DomainError {
        public IdempotencyConflictException(String message) {
            super(message, "IDEMPOTENCY_CONFLICT");
        }
    }
}

