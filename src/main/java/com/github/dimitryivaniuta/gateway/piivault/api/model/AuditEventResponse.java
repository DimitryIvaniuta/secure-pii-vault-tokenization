package com.github.dimitryivaniuta.gateway.piivault.api.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * API projection for compliance audit events.
 *
 * @param id event id
 * @param eventType event type
 * @param outcome event outcome
 * @param token token if applicable
 * @param actor authenticated actor
 * @param actorRoles actor roles
 * @param purpose purpose of use
 * @param correlationId correlation id
 * @param details details map
 * @param createdAt creation time
 */
public record AuditEventResponse(
        UUID id,
        String eventType,
        String outcome,
        String token,
        String actor,
        String actorRoles,
        String purpose,
        String correlationId,
        Map<String, Object> details,
        Instant createdAt
) {
}
