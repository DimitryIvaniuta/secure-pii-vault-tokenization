package com.github.dimitryivaniuta.gateway.piivault.exception;

/**
 * Raised when payload encryption or decryption fails.
 */
public class CryptoOperationException extends RuntimeException {

    public CryptoOperationException(String message) {
        super(message);
    }

    public CryptoOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
