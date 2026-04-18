package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CryptoKeyMetadataResponse;
import com.github.dimitryivaniuta.gateway.piivault.crypto.VaultEncryptionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Exposes safe key-ring metadata for operations teams.
 */
@Service
public class CryptoAdminService {

    private final VaultEncryptionService vaultEncryptionService;

    public CryptoAdminService(VaultEncryptionService vaultEncryptionService) {
        this.vaultEncryptionService = vaultEncryptionService;
    }

    /**
     * Returns current key-ring metadata without exposing raw key material.
     *
     * @return key metadata response
     */
    public Mono<CryptoKeyMetadataResponse> keyMetadata() {
        return Mono.just(new CryptoKeyMetadataResponse(
                vaultEncryptionService.activeKeyId(),
                vaultEncryptionService.availableKeyIds(),
                vaultEncryptionService.keyLengths(),
                true
        ));
    }
}
