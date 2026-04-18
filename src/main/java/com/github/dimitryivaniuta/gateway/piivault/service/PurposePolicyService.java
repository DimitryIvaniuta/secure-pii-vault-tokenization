package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.config.AppPurposePolicyProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Validates purpose-of-use headers against an allow-list and break-glass rules.
 */
@Service
public class PurposePolicyService {

    private final AppPurposePolicyProperties properties;

    public PurposePolicyService(AppPurposePolicyProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates a read purpose.
     *
     * @param purpose read purpose
     * @param breakGlassJustification optional break-glass justification
     * @return validation result
     */
    public PurposeValidationResult validateReadPurpose(String purpose, String breakGlassJustification) {
        return validate("read", purpose, breakGlassJustification, properties.getReadAllowedPurposes());
    }

    /**
     * Validates a delete purpose.
     *
     * @param purpose delete purpose
     * @param breakGlassJustification optional break-glass justification
     * @return validation result
     */
    public PurposeValidationResult validateDeletePurpose(String purpose, String breakGlassJustification) {
        return validate("delete", purpose, breakGlassJustification, properties.getDeleteAllowedPurposes());
    }

    private PurposeValidationResult validate(String operation, String purpose, String breakGlassJustification, java.util.Set<String> allowedPurposes) {
        String normalizedPurpose = purpose == null ? "" : purpose.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedPurpose.isBlank()) {
            throw new IllegalArgumentException("Purpose-of-use is required");
        }
        if (!allowedPurposes.contains(normalizedPurpose)) {
            throw new AccessDeniedException("Purpose is not allowed for " + operation + ": " + normalizedPurpose);
        }
        boolean breakGlass = properties.getBreakGlassPurposes().contains(normalizedPurpose);
        String normalizedJustification = breakGlassJustification == null ? null : breakGlassJustification.trim();
        if (breakGlass && (normalizedJustification == null || normalizedJustification.isBlank())) {
            throw new IllegalArgumentException("Break-glass justification header is required for purpose: " + normalizedPurpose);
        }
        return new PurposeValidationResult(normalizedPurpose, breakGlass, normalizedJustification);
    }
}
