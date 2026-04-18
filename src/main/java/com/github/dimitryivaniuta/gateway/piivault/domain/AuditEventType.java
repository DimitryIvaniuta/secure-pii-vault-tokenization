package com.github.dimitryivaniuta.gateway.piivault.domain;

/**
 * Audit event types emitted by the service.
 */
public enum AuditEventType {
    PII_CREATED,
    PII_READ,
    PII_DELETED,
    PII_REWRAPPED,
    CUSTOMER_CREATED
}
