package com.github.dimitryivaniuta.gateway.piivault.exception;

/**
 * Raised when a requested token or token-only record does not exist.
 */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String message) {
        super(message);
    }
}
