package com.github.dimitryivaniuta.gateway.piivault.crypto;

/**
 * Envelope-encrypted payload state persisted in the vault.
 *
 * @param keyId wrapping key identifier
 * @param iv payload cipher IV
 * @param ciphertext payload cipher text with authentication tag appended by JCE
 * @param tagLength payload authentication tag length in bits
 * @param wrappedDekIv wrapped data-encryption-key IV
 * @param wrappedDekCiphertext wrapped data-encryption-key cipher text
 * @param wrappedDekTagLength wrapped data-encryption-key authentication tag length in bits
 */
public record EncryptedPayload(
        String keyId,
        byte[] iv,
        byte[] ciphertext,
        int tagLength,
        byte[] wrappedDekIv,
        byte[] wrappedDekCiphertext,
        Integer wrappedDekTagLength
) {

    /**
     * Returns true when the payload uses per-record envelope encryption.
     *
     * @return true for envelope mode
     */
    public boolean envelopeMode() {
        return wrappedDekIv != null && wrappedDekCiphertext != null && wrappedDekTagLength != null;
    }
}
