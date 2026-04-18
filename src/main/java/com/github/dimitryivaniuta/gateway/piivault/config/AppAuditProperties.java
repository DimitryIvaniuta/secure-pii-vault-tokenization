package com.github.dimitryivaniuta.gateway.piivault.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for audit streaming and integrity protection.
 */
@Validated
@ConfigurationProperties(prefix = "app.audit")
public class AppAuditProperties {

    @NotBlank
    private String topicName;

    /**
     * Base64-encoded HMAC key used to sign persisted audit events.
     */
    @NotBlank
    private String integrityKey;

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getIntegrityKey() {
        return integrityKey;
    }

    public void setIntegrityKey(String integrityKey) {
        this.integrityKey = integrityKey;
    }
}
