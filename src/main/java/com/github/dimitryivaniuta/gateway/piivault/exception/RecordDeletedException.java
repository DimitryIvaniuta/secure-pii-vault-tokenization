package com.github.dimitryivaniuta.gateway.piivault.exception;

/**
 * Raised when a request targets a token whose PII payload has already been deleted.
 */
public class RecordDeletedException extends RuntimeException {

    public RecordDeletedException(String message) {
        super(message);
    }
}
