package com.github.dimitryivaniuta.gateway.piivault.exception;

/**
 * Raised when a reused idempotency key is sent with a different request body.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
