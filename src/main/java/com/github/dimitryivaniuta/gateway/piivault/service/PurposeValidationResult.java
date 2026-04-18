package com.github.dimitryivaniuta.gateway.piivault.service;

/**
 * Result of validating an access purpose.
 *
 * @param normalizedPurpose normalized purpose string
 * @param breakGlass whether this access used a break-glass purpose
 * @param justification optional operator justification
 */
public record PurposeValidationResult(
        String normalizedPurpose,
        boolean breakGlass,
        String justification
) {
}
