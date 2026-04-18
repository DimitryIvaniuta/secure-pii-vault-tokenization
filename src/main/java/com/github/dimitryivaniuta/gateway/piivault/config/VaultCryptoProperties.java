package com.github.dimitryivaniuta.gateway.piivault.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for vault encryption keys.
 */
@Validated
@ConfigurationProperties(prefix = "app.vault.crypto")
public class VaultCryptoProperties {

    /**
     * Identifier of the key currently used for new encrypt operations.
     */
    @NotBlank
    private String activeKeyId;

    /**
     * Map of available key ids to base64-encoded AES key material.
     */
    @NotEmpty
    private Map<String, String> keys = new LinkedHashMap<>();

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void setActiveKeyId(String activeKeyId) {
        this.activeKeyId = activeKeyId;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}
