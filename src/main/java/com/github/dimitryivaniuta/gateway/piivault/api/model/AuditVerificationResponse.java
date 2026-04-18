package com.github.dimitryivaniuta.gateway.piivault.api.model;

import java.util.List;
import java.util.UUID;

/**
 * Result of verifying persisted audit signatures.
 *
 * @param checkedEvents total checked events
 * @param validEvents valid signature count
 * @param invalidEvents invalid signature count
 * @param invalidEventIds invalid event ids
 */
public record AuditVerificationResponse(
        long checkedEvents,
        long validEvents,
        long invalidEvents,
        List<UUID> invalidEventIds
) {
}
