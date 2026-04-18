package com.github.dimitryivaniuta.gateway.piivault.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for non-sensitive Redis cache entries.
 */
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    @NotNull
    private Duration tokenStateTtl = Duration.ofHours(24);

    public Duration getTokenStateTtl() {
        return tokenStateTtl;
    }

    public void setTokenStateTtl(Duration tokenStateTtl) {
        this.tokenStateTtl = tokenStateTtl;
    }
}
