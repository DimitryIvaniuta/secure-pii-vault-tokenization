package com.github.dimitryivaniuta.gateway.piivault.crypto;

import java.nio.charset.StandardCharsets;

/**
 * Stable associated-data context used to bind encrypted payloads to safe vault metadata.
 *
 * @param token vault token
 * @param subjectRef subject reference
 * @param classification classification name
 */
public record AadContext(String token, String subjectRef, String classification) {

    /**
     * Returns canonical associated data bytes.
     *
     * @return UTF-8 bytes
     */
    public byte[] toBytes() {
        return String.join("|", token, subjectRef, classification).getBytes(StandardCharsets.UTF_8);
    }
}
