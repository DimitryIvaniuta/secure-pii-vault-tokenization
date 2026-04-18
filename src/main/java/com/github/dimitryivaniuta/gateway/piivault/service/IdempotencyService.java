package com.github.dimitryivaniuta.gateway.piivault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.piivault.entity.IdempotencyRequestEntity;
import com.github.dimitryivaniuta.gateway.piivault.exception.IdempotencyConflictException;
import com.github.dimitryivaniuta.gateway.piivault.repository.IdempotencyRequestRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Persists request fingerprints so token creation can be retried safely.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRequestRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRequestRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Finds an earlier request by actor and key.
     *
     * @param actor actor name
     * @param idempotencyKey header value
     * @return matching entry or empty
     */
    public Mono<IdempotencyRequestEntity> find(String actor, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }
        return repository.findByActorAndIdempotencyKey(actor, idempotencyKey);
    }

    /**
     * Computes a stable request hash without storing raw PII.
     *
     * @param request request object
     * @return hex encoded hash
     */
    public String requestHash(Object request) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to hash idempotent request", exception);
        }
    }

    /**
     * Ensures that a replay uses the same request body.
     *
     * @param existing existing entry
     * @param requestHash current request hash
     */
    public void ensureSameRequest(IdempotencyRequestEntity existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Idempotency key was already used with a different request body");
        }
    }

    /**
     * Saves a new idempotency entry.
     *
     * @param actor actor name
     * @param idempotencyKey header value
     * @param requestHash request hash
     * @param token created token
     * @return saved entry or existing matching entry when a concurrent replay already won
     */
    public Mono<IdempotencyRequestEntity> save(String actor, String idempotencyKey, String requestHash, String token) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }
        IdempotencyRequestEntity entity = new IdempotencyRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setActor(actor);
        entity.setIdempotencyKey(idempotencyKey.trim());
        entity.setRequestHash(requestHash);
        entity.setToken(token);
        entity.setCreatedAt(Instant.now());
        return repository.save(entity)
                .onErrorResume(DuplicateKeyException.class, exception -> repository.findByActorAndIdempotencyKey(actor, idempotencyKey)
                        .switchIfEmpty(Mono.error(exception))
                        .flatMap(existing -> {
                            ensureSameRequest(existing, requestHash);
                            return Mono.just(existing);
                        }));
    }
}
