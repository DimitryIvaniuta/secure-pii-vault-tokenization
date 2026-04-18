package com.github.dimitryivaniuta.gateway.piivault.api.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Token-only customer response without raw PII fields.
 *
 * @param id customer id
 * @param externalRef business external reference
 * @param piiToken opaque token only
 * @param customerStatus status
 * @param createdAt creation time
 */
public record CustomerResponse(UUID id, String externalRef, String piiToken, String customerStatus, Instant createdAt) {
}
