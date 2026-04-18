package com.github.dimitryivaniuta.gateway.piivault.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for redaction logic used in audit-safe metadata.
 */
class PiiRedactorTest {

    @Test
    void shouldRedactValues() {
        PiiRedactor redactor = new PiiRedactor();
        assertThat(redactor.redactValue("ABCDEFGH")).isEqualTo("AB****GH");
        assertThat(redactor.redactValue("AB")).isEqualTo("****");
        assertThat(redactor.redactLongText("Emergency access for critical fraud check")).startsWith("Emer****");
    }
}
