package com.github.dimitryivaniuta.gateway.piivault.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.piivault.config.AppAuditProperties;
import com.github.dimitryivaniuta.gateway.piivault.entity.AuditEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for audit-event HMAC signing and verification.
 */
class AuditIntegrityServiceTest {

    @Test
    void shouldDetectTampering() {
        AppAuditProperties properties = new AppAuditProperties();
        properties.setTopicName("pii-vault.audit.v1");
        properties.setIntegrityKey("ZFA9WHPcP5IOKyQ5HP3haFFkuT9p4JymQ+0Wi6+N6cs=");
        AuditIntegrityService service = new AuditIntegrityService(properties);

        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventType("PII_READ");
        entity.setOutcome("SUCCESS");
        entity.setToken("tok_123");
        entity.setActor("vault-reader");
        entity.setActorRoles("PII_READ");
        entity.setPurpose("customer-support");
        entity.setCorrelationId("corr-1");
        entity.setDetailsJson("{\"breakGlass\":false}");
        entity.setCreatedAt(Instant.parse("2026-04-18T10:15:30Z"));
        entity.setEventSignature(service.sign(entity));

        assertThat(service.verify(entity)).isTrue();

        entity.setPurpose("tampered-purpose");
        assertThat(service.verify(entity)).isFalse();
    }
}
