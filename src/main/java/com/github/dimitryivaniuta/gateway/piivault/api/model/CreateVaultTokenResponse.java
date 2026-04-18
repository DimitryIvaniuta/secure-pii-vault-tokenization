package com.github.dimitryivaniuta.gateway.piivault.api.model;

import com.github.dimitryivaniuta.gateway.piivault.domain.PiiClassification;
import java.time.Instant;

/**
 * Response after PII was stored in the vault.
 *
 * @param token opaque token
 * @param classification data classification
 * @param createdAt creation timestamp
 * @param idempotentReplay whether the request was replayed from an earlier successful create
 */
public record CreateVaultTokenResponse(
        String token,
        PiiClassification classification,
        Instant createdAt,
        boolean idempotentReplay
) {
}
