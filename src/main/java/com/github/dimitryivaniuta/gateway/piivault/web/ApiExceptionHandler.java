package com.github.dimitryivaniuta.gateway.piivault.web;

import com.github.dimitryivaniuta.gateway.piivault.exception.IdempotencyConflictException;
import com.github.dimitryivaniuta.gateway.piivault.exception.RecordDeletedException;
import com.github.dimitryivaniuta.gateway.piivault.exception.RecordNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central API error translation to ProblemDetail responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps not-found conditions.
     *
     * @param exception domain exception
     * @return problem detail
     */
    @ExceptionHandler(RecordNotFoundException.class)
    ProblemDetail handleNotFound(RecordNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), "https://example.local/problems/not-found");
    }

    /**
     * Maps deleted-record conditions.
     *
     * @param exception domain exception
     * @return problem detail
     */
    @ExceptionHandler(RecordDeletedException.class)
    ProblemDetail handleDeleted(RecordDeletedException exception) {
        return problem(HttpStatus.GONE, exception.getMessage(), "https://example.local/problems/deleted");
    }

    /**
     * Maps idempotency conflicts.
     *
     * @param exception conflict exception
     * @return problem detail
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleConflict(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), "https://example.local/problems/idempotency-conflict");
    }

    /**
     * Maps validation errors.
     *
     * @param exception validation exception
     * @return problem detail
     */
    @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ProblemDetail handleBadRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), "https://example.local/problems/bad-request");
    }

    /**
     * Maps authorization denials.
     *
     * @param exception access denied exception
     * @return problem detail
     */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "https://example.local/problems/forbidden");
    }

    /**
     * Maps unexpected errors.
     *
     * @param exception unhandled exception
     * @return problem detail
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "https://example.local/problems/internal");
    }

    private ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        return problemDetail;
    }
}
