package com.github.dimitryivaniuta.gateway.piivault.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Utility that produces safe redacted views of sensitive payloads for diagnostics and audit metadata.
 */
@Component
public class PiiRedactor {

    /**
     * Redacts a value by preserving only the outermost characters when possible.
     *
     * @param value raw value
     * @return redacted value
     */
    public String redactValue(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * Redacts a long free-text string to avoid storing detailed operator notes in plain audit metadata.
     *
     * @param value raw value
     * @return redacted text
     */
    public String redactLongText(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 12) {
            return redactValue(trimmed);
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * Creates a safe metadata map for audit details.
     *
     * @param subjectRef subject reference
     * @param classification classification
     * @return redacted audit details
     */
    public Map<String, Object> safeAuditDetails(String subjectRef, String classification) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("subjectRef", redactValue(subjectRef));
        details.put("classification", classification);
        return details;
    }
}
