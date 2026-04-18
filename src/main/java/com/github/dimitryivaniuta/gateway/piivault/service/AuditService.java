package com.github.dimitryivaniuta.gateway.piivault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.piivault.api.model.AuditEventResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.AuditVerificationResponse;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditEventType;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditOutcome;
import com.github.dimitryivaniuta.gateway.piivault.entity.AuditEventEntity;
import com.github.dimitryivaniuta.gateway.piivault.repository.AuditEventRepository;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticatedActor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Persists and exposes audit events for all PII-sensitive operations.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final AuditPublisher publisher;
    private final AuditIntegrityService auditIntegrityService;
    private final ObjectMapper objectMapper;

    public AuditService(
            AuditEventRepository repository,
            AuditPublisher publisher,
            AuditIntegrityService auditIntegrityService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.auditIntegrityService = auditIntegrityService;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists and publishes an auditable event.
     *
     * @param eventType event type
     * @param outcome outcome
     * @param token token if applicable
     * @param actor authenticated actor
     * @param purpose purpose of access
     * @param correlationId correlation id
     * @param details safe detail map
     * @return saved event
     */
    public Mono<AuditEventEntity> record(
            AuditEventType eventType,
            AuditOutcome outcome,
            String token,
            AuthenticatedActor actor,
            String purpose,
            String correlationId,
            Map<String, Object> details
    ) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventType(eventType.name());
        entity.setOutcome(outcome.name());
        entity.setToken(token);
        entity.setActor(actor.username());
        entity.setActorRoles(String.join(",", actor.roles()));
        entity.setPurpose(purpose);
        entity.setCorrelationId(correlationId);
        entity.setDetailsJson(write(details));
        entity.setCreatedAt(Instant.now());
        entity.setSignatureVersion(auditIntegrityService.signatureVersion());
        entity.setEventSignature(auditIntegrityService.sign(entity));

        return repository.save(entity)
                .flatMap(saved -> publisher.publish(saved).onErrorResume(ignored -> Mono.empty()).thenReturn(saved));
    }

    /**
     * Returns recent audit events.
     *
     * @return stream of API audit responses
     */
    public Flux<AuditEventResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().map(this::toResponse);
    }

    /**
     * Returns recent audit events for a token.
     *
     * @param token token
     * @return stream of API audit responses
     */
    public Flux<AuditEventResponse> findByToken(String token) {
        return repository.findByTokenOrderByCreatedAtDesc(token).map(this::toResponse);
    }

    /**
     * Verifies stored audit signatures for all events.
     *
     * @return verification result
     */
    public Mono<AuditVerificationResponse> verifyAll() {
        return verify(repository.findAllByOrderByCreatedAtAsc());
    }

    /**
     * Verifies stored audit signatures for a single token.
     *
     * @param token token
     * @return verification result
     */
    public Mono<AuditVerificationResponse> verifyByToken(String token) {
        return verify(repository.findByTokenOrderByCreatedAtAsc(token));
    }

    private Mono<AuditVerificationResponse> verify(Flux<AuditEventEntity> source) {
        return source.collectList().map(events -> {
            List<UUID> invalidIds = events.stream()
                    .filter(event -> !auditIntegrityService.verify(event))
                    .map(AuditEventEntity::getId)
                    .toList();
            long checked = events.size();
            long invalid = invalidIds.size();
            return new AuditVerificationResponse(checked, checked - invalid, invalid, invalidIds);
        });
    }

    private String write(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize audit details", exception);
        }
    }

    private AuditEventResponse toResponse(AuditEventEntity entity) {
        try {
            return new AuditEventResponse(
                    entity.getId(),
                    entity.getEventType(),
                    entity.getOutcome(),
                    entity.getToken(),
                    entity.getActor(),
                    entity.getActorRoles(),
                    entity.getPurpose(),
                    entity.getCorrelationId(),
                    objectMapper.readValue(entity.getDetailsJson(), new TypeReference<>() {}),
                    entity.getCreatedAt()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize audit details", exception);
        }
    }
}
