package com.github.dimitryivaniuta.gateway.piivault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.dimitryivaniuta.gateway.piivault.config.AppPurposePolicyProperties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests for declarative purpose-of-use policy enforcement.
 */
class PurposePolicyServiceTest {

    @Test
    void shouldRequireBreakGlassJustification() {
        AppPurposePolicyProperties properties = new AppPurposePolicyProperties();
        properties.setReadAllowedPurposes(Set.of("customer-support", "break-glass-emergency"));
        properties.setDeleteAllowedPurposes(Set.of("gdpr-delete-request"));
        properties.setBreakGlassPurposes(Set.of("break-glass-emergency"));
        PurposePolicyService service = new PurposePolicyService(properties);

        assertThatThrownBy(() -> service.validateReadPurpose("break-glass-emergency", null))
                .isInstanceOf(IllegalArgumentException.class);

        PurposeValidationResult result = service.validateReadPurpose("break-glass-emergency", "fraud escalation after customer lockout");
        assertThat(result.breakGlass()).isTrue();
        assertThat(result.normalizedPurpose()).isEqualTo("break-glass-emergency");
    }

    @Test
    void shouldRejectUnknownPurpose() {
        AppPurposePolicyProperties properties = new AppPurposePolicyProperties();
        properties.setReadAllowedPurposes(Set.of("customer-support"));
        properties.setDeleteAllowedPurposes(Set.of("gdpr-delete-request"));
        PurposePolicyService service = new PurposePolicyService(properties);

        assertThatThrownBy(() -> service.validateReadPurpose("marketing", null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
