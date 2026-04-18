package com.github.dimitryivaniuta.gateway.piivault.api.model;

import java.util.Map;
import java.util.Set;

/**
 * Safe response describing configured vault wrapping keys.
 *
 * @param activeKeyId active wrapping key id
 * @param availableKeyIds available key ids
 * @param keyLengthsBits key length in bits per key id
 * @param envelopeEncryption whether per-record envelope encryption is enabled
 */
public record CryptoKeyMetadataResponse(
        String activeKeyId,
        Set<String> availableKeyIds,
        Map<String, Integer> keyLengthsBits,
        boolean envelopeEncryption
) {
}
