package com.github.dimitryivaniuta.gateway.piivault.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.piivault.api.model.PiiPayload;
import com.github.dimitryivaniuta.gateway.piivault.config.VaultCryptoProperties;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for envelope encryption and re-wrap behavior.
 */
class VaultEncryptionServiceTest {

    @Test
    void shouldEncryptDecryptAndRewrapPayload() {
        VaultCryptoProperties properties = new VaultCryptoProperties();
        properties.setActiveKeyId("k1");
        properties.setKeys(Map.of(
                "k1", "FMHj1sgQSUQ+Ymh16NfMptLxIIDc5/M4cSLAXF2N+4k=",
                "k2", "5oGzgVSB2OWbEFxnasmRjWaN/vg6sCRAHnh1+oqKLW0="
        ));
        VaultEncryptionService service = new VaultEncryptionService(properties, new ObjectMapper().findAndRegisterModules());
        service.validateKeyRing();

        PiiPayload payload = new PiiPayload(
                "Alice Doe",
                "alice@example.com",
                "+48111222333",
                "PL1234567",
                "Street 1",
                LocalDate.of(1990, 1, 1)
        );
        AadContext aadContext = new AadContext("tok_123", "customer-1", "RESTRICTED");

        EncryptedPayload encryptedPayload = service.encrypt(payload, aadContext);
        PiiPayload decrypted = service.decrypt(encryptedPayload, aadContext, PiiPayload.class);

        assertThat(encryptedPayload.ciphertext()).isNotEmpty();
        assertThat(encryptedPayload.wrappedDekCiphertext()).isNotEmpty();
        assertThat(decrypted).isEqualTo(payload);

        properties.setActiveKeyId("k2");
        EncryptedPayload rewrapped = service.rewrapToActiveKey(encryptedPayload, aadContext, payload);
        PiiPayload decryptedAfterRewrap = service.decrypt(rewrapped, aadContext, PiiPayload.class);

        assertThat(rewrapped.keyId()).isEqualTo("k2");
        assertThat(decryptedAfterRewrap).isEqualTo(payload);
    }
}
