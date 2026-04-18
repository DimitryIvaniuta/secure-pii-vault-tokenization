package com.github.dimitryivaniuta.gateway.piivault.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Declarative purpose-of-use policy configuration.
 */
@Validated
@ConfigurationProperties(prefix = "app.purpose-policy")
public class AppPurposePolicyProperties {

    @NotEmpty
    private Set<String> readAllowedPurposes = new LinkedHashSet<>();

    @NotEmpty
    private Set<String> deleteAllowedPurposes = new LinkedHashSet<>();

    private Set<String> breakGlassPurposes = new LinkedHashSet<>();

    public Set<String> getReadAllowedPurposes() {
        return readAllowedPurposes;
    }

    public void setReadAllowedPurposes(Set<String> readAllowedPurposes) {
        this.readAllowedPurposes = readAllowedPurposes;
    }

    public Set<String> getDeleteAllowedPurposes() {
        return deleteAllowedPurposes;
    }

    public void setDeleteAllowedPurposes(Set<String> deleteAllowedPurposes) {
        this.deleteAllowedPurposes = deleteAllowedPurposes;
    }

    public Set<String> getBreakGlassPurposes() {
        return breakGlassPurposes;
    }

    public void setBreakGlassPurposes(Set<String> breakGlassPurposes) {
        this.breakGlassPurposes = breakGlassPurposes;
    }
}
