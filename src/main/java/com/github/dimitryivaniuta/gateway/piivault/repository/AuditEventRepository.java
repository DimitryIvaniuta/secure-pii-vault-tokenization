package com.github.dimitryivaniuta.gateway.piivault.repository;

import com.github.dimitryivaniuta.gateway.piivault.entity.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Reactive repository for persisted audit events.
 */
public interface AuditEventRepository extends ReactiveCrudRepository<AuditEventEntity, UUID> {

    /**
     * Returns audit events for a given token ordered by creation time descending.
     *
     * @param token token reference
     * @return audit event stream
     */
    Flux<AuditEventEntity> findByTokenOrderByCreatedAtDesc(String token);

    /**
     * Returns recent audit events ordered by creation time descending.
     *
     * @return audit event stream
     */
    Flux<AuditEventEntity> findAllByOrderByCreatedAtDesc();

    /**
     * Returns audit events ordered by creation time ascending for integrity verification.
     *
     * @return audit event stream
     */
    Flux<AuditEventEntity> findAllByOrderByCreatedAtAsc();

    /**
     * Returns token audit events ordered by creation time ascending for integrity verification.
     *
     * @param token token reference
     * @return audit event stream
     */
    Flux<AuditEventEntity> findByTokenOrderByCreatedAtAsc(String token);
}
