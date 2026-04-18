package com.github.dimitryivaniuta.gateway.piivault.api.model;

import java.time.Instant;

/**
 * Response after a token payload has been re-wrapped to the current active key.
 *
 * @param token token
 * @param previousKeyId previous wrapping key id
 * @param currentKeyId current wrapping key id
 * @param rewrappedAt operation timestamp
 */
public record RewrapTokenResponse(
        String token,
        String previousKeyId,
        String currentKeyId,
        Instant rewrappedAt
) {
}
