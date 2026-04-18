package com.github.dimitryivaniuta.gateway.piivault.api.model;

import com.github.dimitryivaniuta.gateway.piivault.domain.PiiClassification;
import java.time.Instant;

/**
 * Privileged response that reveals decrypted PII.
 *
 * @param token token
 * @param classification classification
 * @param subjectRef subject reference
 * @param pii decrypted sensitive payload
 * @param createdAt creation time
 * @param lastAccessedAt last access time
 */
public record VaultTokenResponse(
        String token,
        PiiClassification classification,
        String subjectRef,
        PiiPayload pii,
        Instant createdAt,
        Instant lastAccessedAt
) {
}
