package com.github.dimitryivaniuta.gateway.piivault.repository;

import com.github.dimitryivaniuta.gateway.piivault.entity.IdempotencyRequestEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repository for create-token idempotency entries.
 */
public interface IdempotencyRequestRepository extends ReactiveCrudRepository<IdempotencyRequestEntity, UUID> {

    /**
     * Finds an entry by actor and idempotency key.
     *
     * @param actor actor name
     * @param idempotencyKey idempotency key
     * @return matching entry or empty
     */
    Mono<IdempotencyRequestEntity> findByActorAndIdempotencyKey(String actor, String idempotencyKey);
}
