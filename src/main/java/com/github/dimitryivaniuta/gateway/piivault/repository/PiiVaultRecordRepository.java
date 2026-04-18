package com.github.dimitryivaniuta.gateway.piivault.repository;

import com.github.dimitryivaniuta.gateway.piivault.entity.PiiVaultRecordEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for encrypted vault records.
 */
public interface PiiVaultRecordRepository extends ReactiveCrudRepository<PiiVaultRecordEntity, UUID> {

    /**
     * Finds a vault record by opaque token.
     *
     * @param token token reference
     * @return matching vault record or empty
     */
    Mono<PiiVaultRecordEntity> findByToken(String token);
}
