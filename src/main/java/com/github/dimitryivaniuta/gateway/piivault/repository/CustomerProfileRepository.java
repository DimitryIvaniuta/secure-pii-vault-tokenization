package com.github.dimitryivaniuta.gateway.piivault.repository;

import com.github.dimitryivaniuta.gateway.piivault.entity.CustomerProfileEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for token-only customer profiles.
 */
public interface CustomerProfileRepository extends ReactiveCrudRepository<CustomerProfileEntity, UUID> {

    /**
     * Finds a customer by business external reference.
     *
     * @param externalRef external reference
     * @return matching customer or empty
     */
    Mono<CustomerProfileEntity> findByExternalRef(String externalRef);
}
