package com.github.dimitryivaniuta.gateway.piivault;

import com.github.dimitryivaniuta.gateway.piivault.config.AppAuditProperties;
import com.github.dimitryivaniuta.gateway.piivault.config.AppCacheProperties;
import com.github.dimitryivaniuta.gateway.piivault.config.AppPurposePolicyProperties;
import com.github.dimitryivaniuta.gateway.piivault.config.AppSecurityProperties;
import com.github.dimitryivaniuta.gateway.piivault.config.VaultCryptoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Application entry point for the secure PII vault and tokenization service.
 */
@SpringBootApplication
@EnableConfigurationProperties({
        VaultCryptoProperties.class,
        AppSecurityProperties.class,
        AppAuditProperties.class,
        AppCacheProperties.class,
        AppPurposePolicyProperties.class
})
public class SecurePiiVaultApplication {

    /**
     * Boots the Spring application.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SecurePiiVaultApplication.class, args);
    }
}
