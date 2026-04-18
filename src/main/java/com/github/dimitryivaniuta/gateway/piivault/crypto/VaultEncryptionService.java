package com.github.dimitryivaniuta.gateway.piivault.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.piivault.config.VaultCryptoProperties;
import com.github.dimitryivaniuta.gateway.piivault.exception.CryptoOperationException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Service responsible for envelope encrypting and decrypting PII payloads with AES/GCM.
 */
@Component
public class VaultEncryptionService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final int TAG_LENGTH = 128;
    private static final int IV_SIZE = 12;
    private static final int DEK_SIZE = 32;

    private final VaultCryptoProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public VaultEncryptionService(VaultCryptoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates configured key ring on startup.
     */
    @PostConstruct
    public void validateKeyRing() {
        if (!properties.getKeys().containsKey(properties.getActiveKeyId())) {
            throw new CryptoOperationException("Active wrapping key is not present in the configured key ring: " + properties.getActiveKeyId());
        }
        properties.getKeys().forEach((keyId, value) -> {
            byte[] decoded = decodeBase64(value, "Invalid base64 key for key id: " + keyId);
            if (decoded.length != DEK_SIZE) {
                throw new CryptoOperationException("Vault key must be exactly 32 bytes (256-bit AES key). keyId=" + keyId);
            }
        });
    }

    /**
     * Encrypts a payload object using a random data-encryption key that is wrapped by the active key-encryption key.
     *
     * @param value payload object
     * @param aadContext stable associated-data context
     * @return encrypted payload
     */
    public EncryptedPayload encrypt(Object value, AadContext aadContext) {
        try {
            String keyId = properties.getActiveKeyId();
            byte[] kekBytes = resolveKey(keyId);
            byte[] dekBytes = randomBytes(DEK_SIZE);
            byte[] payloadIv = randomBytes(IV_SIZE);
            byte[] wrappedDekIv = randomBytes(IV_SIZE);
            byte[] plaintext = objectMapper.writeValueAsBytes(value);
            byte[] aad = aadContext.toBytes();

            byte[] ciphertext = gcmEncrypt(dekBytes, payloadIv, plaintext, aad);
            byte[] wrappedDekCiphertext = gcmEncrypt(kekBytes, wrappedDekIv, dekBytes, keyId.getBytes(StandardCharsets.UTF_8));
            zeroize(dekBytes);
            return new EncryptedPayload(keyId, payloadIv, ciphertext, TAG_LENGTH, wrappedDekIv, wrappedDekCiphertext, TAG_LENGTH);
        } catch (Exception exception) {
            throw new CryptoOperationException("Failed to encrypt PII payload", exception);
        }
    }

    /**
     * Decrypts a previously encrypted payload to the requested type.
     * Supports both legacy direct-key rows and new envelope-encrypted rows.
     *
     * @param payload encrypted payload
     * @param aadContext stable associated-data context
     * @param type target type
     * @param <T> payload type
     * @return decrypted object
     */
    public <T> T decrypt(EncryptedPayload payload, AadContext aadContext, Class<T> type) {
        try {
            byte[] plaintext;
            if (payload.envelopeMode()) {
                byte[] kekBytes = resolveKey(payload.keyId());
                byte[] dekBytes = gcmDecrypt(kekBytes, payload.wrappedDekIv(), payload.wrappedDekCiphertext(), payload.wrappedDekTagLength(), payload.keyId().getBytes(StandardCharsets.UTF_8));
                plaintext = gcmDecrypt(dekBytes, payload.iv(), payload.ciphertext(), payload.tagLength(), aadContext.toBytes());
                zeroize(dekBytes);
            } else {
                byte[] keyBytes = resolveKey(payload.keyId());
                plaintext = gcmDecrypt(keyBytes, payload.iv(), payload.ciphertext(), payload.tagLength(), null);
            }
            return objectMapper.readValue(plaintext, type);
        } catch (Exception exception) {
            throw new CryptoOperationException("Failed to decrypt PII payload", exception);
        }
    }

    /**
     * Re-wraps an existing envelope-encrypted payload DEK to the currently active key.
     * Legacy rows are upgraded by decrypting and re-encrypting the supplied plaintext with envelope encryption.
     *
     * @param existing existing encrypted payload
     * @param aadContext stable associated-data context
     * @param plaintextValue decrypted value when legacy upgrade is needed
     * @return re-wrapped encrypted payload
     */
    public EncryptedPayload rewrapToActiveKey(EncryptedPayload existing, AadContext aadContext, Object plaintextValue) {
        if (!existing.envelopeMode()) {
            return encrypt(plaintextValue, aadContext);
        }
        try {
            String targetKeyId = properties.getActiveKeyId();
            if (targetKeyId.equals(existing.keyId())) {
                return existing;
            }
            byte[] currentKekBytes = resolveKey(existing.keyId());
            byte[] dekBytes = gcmDecrypt(
                    currentKekBytes,
                    existing.wrappedDekIv(),
                    existing.wrappedDekCiphertext(),
                    existing.wrappedDekTagLength(),
                    existing.keyId().getBytes(StandardCharsets.UTF_8)
            );
            byte[] targetKekBytes = resolveKey(targetKeyId);
            byte[] newWrappedDekIv = randomBytes(IV_SIZE);
            byte[] newWrappedDekCiphertext = gcmEncrypt(targetKekBytes, newWrappedDekIv, dekBytes, targetKeyId.getBytes(StandardCharsets.UTF_8));
            zeroize(dekBytes);
            return new EncryptedPayload(
                    targetKeyId,
                    existing.iv(),
                    existing.ciphertext(),
                    existing.tagLength(),
                    newWrappedDekIv,
                    newWrappedDekCiphertext,
                    TAG_LENGTH
            );
        } catch (Exception exception) {
            throw new CryptoOperationException("Failed to re-wrap vault payload", exception);
        }
    }

    /**
     * Returns safe metadata about the configured key ring.
     *
     * @return key metadata map keyed by key identifier
     */
    public Map<String, Integer> keyLengths() {
        Map<String, Integer> lengths = new LinkedHashMap<>();
        properties.getKeys().forEach((keyId, value) -> lengths.put(keyId, resolveKey(keyId).length * 8));
        return lengths;
    }

    /**
     * Returns the active key id.
     *
     * @return active key id
     */
    public String activeKeyId() {
        return properties.getActiveKeyId();
    }

    /**
     * Returns all configured key ids.
     *
     * @return configured key ids
     */
    public Set<String> availableKeyIds() {
        return properties.getKeys().keySet();
    }

    /**
     * Produces a stable HMAC-SHA256-based fingerprint for a subject reference without storing the raw value in indexes.
     *
     * @param subjectRef raw subject reference
     * @return url-safe fingerprint
     */
    public String subjectRefFingerprint(String subjectRef) {
        try {
            byte[] keyBytes = resolveKey(properties.getActiveKeyId());
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(keyBytes, HMAC_SHA_256));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(subjectRef.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new CryptoOperationException("Failed to fingerprint subject reference", exception);
        }
    }

    private byte[] gcmEncrypt(byte[] keyBytes, byte[] iv, byte[] plaintext, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, AES), new GCMParameterSpec(TAG_LENGTH, iv));
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(plaintext);
    }

    private byte[] gcmDecrypt(byte[] keyBytes, byte[] iv, byte[] ciphertext, Integer tagLength, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, AES), new GCMParameterSpec(tagLength, iv));
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(ciphertext);
    }

    private byte[] resolveKey(String keyId) {
        String base64Key = properties.getKeys().get(keyId);
        if (base64Key == null || base64Key.isBlank()) {
            throw new CryptoOperationException("Missing encryption key for key id: " + keyId);
        }
        return decodeBase64(base64Key, "Invalid base64 key for key id: " + keyId);
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private byte[] decodeBase64(String value, String errorMessage) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new CryptoOperationException(errorMessage, exception);
        }
    }

    private void zeroize(byte[] value) {
        if (value == null) {
            return;
        }
        java.util.Arrays.fill(value, (byte) 0);
    }
}
