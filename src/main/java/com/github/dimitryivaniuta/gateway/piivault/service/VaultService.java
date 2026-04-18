package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.PiiPayload;
import com.github.dimitryivaniuta.gateway.piivault.api.model.RewrapTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.VaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.crypto.AadContext;
import com.github.dimitryivaniuta.gateway.piivault.crypto.EncryptedPayload;
import com.github.dimitryivaniuta.gateway.piivault.crypto.VaultEncryptionService;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditEventType;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditOutcome;
import com.github.dimitryivaniuta.gateway.piivault.domain.PiiClassification;
import com.github.dimitryivaniuta.gateway.piivault.domain.VaultRecordStatus;
import com.github.dimitryivaniuta.gateway.piivault.entity.IdempotencyRequestEntity;
import com.github.dimitryivaniuta.gateway.piivault.entity.PiiVaultRecordEntity;
import com.github.dimitryivaniuta.gateway.piivault.exception.RecordDeletedException;
import com.github.dimitryivaniuta.gateway.piivault.exception.RecordNotFoundException;
import com.github.dimitryivaniuta.gateway.piivault.repository.PiiVaultRecordRepository;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticatedActor;
import com.github.dimitryivaniuta.gateway.piivault.util.PiiRedactor;
import com.github.dimitryivaniuta.gateway.piivault.util.TokenGenerator;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Main vault service that manages encrypted PII lifecycle and privileged access.
 */
@Service
public class VaultService {

    private final PiiVaultRecordRepository repository;
    private final VaultEncryptionService encryptionService;
    private final TokenGenerator tokenGenerator;
    private final AuditService auditService;
    private final TokenStateCacheService tokenStateCacheService;
    private final PiiRedactor piiRedactor;
    private final IdempotencyService idempotencyService;
    private final PurposePolicyService purposePolicyService;

    public VaultService(
            PiiVaultRecordRepository repository,
            VaultEncryptionService encryptionService,
            TokenGenerator tokenGenerator,
            AuditService auditService,
            TokenStateCacheService tokenStateCacheService,
            PiiRedactor piiRedactor,
            IdempotencyService idempotencyService,
            PurposePolicyService purposePolicyService
    ) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.tokenGenerator = tokenGenerator;
        this.auditService = auditService;
        this.tokenStateCacheService = tokenStateCacheService;
        this.piiRedactor = piiRedactor;
        this.idempotencyService = idempotencyService;
        this.purposePolicyService = purposePolicyService;
    }

    /**
     * Stores encrypted PII and returns the new opaque token.
     * When an idempotency key is provided, identical retries replay the original token.
     *
     * @param request incoming create request
     * @param idempotencyKey optional idempotency key header
     * @param actor authenticated actor
     * @param correlationId correlation id
     * @return create response
     */
    @Transactional
    public Mono<CreateVaultTokenResponse> create(
            CreateVaultTokenRequest request,
            String idempotencyKey,
            AuthenticatedActor actor,
            String correlationId
    ) {
        String requestHash = idempotencyService.requestHash(request);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createFresh(request, actor, correlationId)
                    .map(saved -> new CreateVaultTokenResponse(saved.getToken(), request.classification(), saved.getCreatedAt(), false));
        }
        return idempotencyService.find(actor.username(), idempotencyKey.trim())
                .flatMap(existing -> replayExisting(existing, requestHash, request.classification()))
                .switchIfEmpty(createFresh(request, actor, correlationId)
                        .flatMap(saved -> idempotencyService.save(actor.username(), idempotencyKey.trim(), requestHash, saved.getToken())
                                .thenReturn(new CreateVaultTokenResponse(saved.getToken(), request.classification(), saved.getCreatedAt(), false))));
    }

    /**
     * Resolves a token and returns decrypted PII for privileged callers.
     *
     * @param token opaque token
     * @param purpose purpose of use
     * @param breakGlassJustification optional break-glass justification
     * @param actor authenticated actor
     * @param correlationId correlation id
     * @return decrypted response
     */
    @Transactional
    public Mono<VaultTokenResponse> resolve(
            String token,
            String purpose,
            String breakGlassJustification,
            AuthenticatedActor actor,
            String correlationId
    ) {
        PurposeValidationResult policy = purposePolicyService.validateReadPurpose(purpose, breakGlassJustification);
        return ensureNotDeleted(token)
                .then(repository.findByToken(token))
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Token not found: " + token)))
                .flatMap(entity -> {
                    if (VaultRecordStatus.DELETED.name().equals(entity.getStatus())) {
                        return Mono.error(new RecordDeletedException("Token already deleted: " + token));
                    }
                    PiiPayload payload = encryptionService.decrypt(toEncryptedPayload(entity), aadContext(entity), PiiPayload.class);
                    entity.setLastAccessedAt(Instant.now());
                    return repository.save(entity)
                            .flatMap(saved -> auditService.record(
                                    AuditEventType.PII_READ,
                                    AuditOutcome.SUCCESS,
                                    token,
                                    actor,
                                    policy.normalizedPurpose(),
                                    correlationId,
                                    auditDetails(saved, policy)
                            ).thenReturn(new VaultTokenResponse(
                                    saved.getToken(),
                                    PiiClassification.valueOf(saved.getClassification()),
                                    saved.getSubjectRef(),
                                    payload,
                                    saved.getCreatedAt(),
                                    saved.getLastAccessedAt()
                            )));
                });
    }

    /**
     * Executes GDPR-style delete by scrubbing encrypted payload bytes and marking the record deleted.
     *
     * @param token opaque token
     * @param purpose delete purpose
     * @param breakGlassJustification optional break-glass justification
     * @param actor authenticated actor
     * @param correlationId correlation id
     * @return completion signal
     */
    @Transactional
    public Mono<Void> delete(
            String token,
            String purpose,
            String breakGlassJustification,
            AuthenticatedActor actor,
            String correlationId
    ) {
        PurposeValidationResult policy = purposePolicyService.validateDeletePurpose(purpose, breakGlassJustification);
        return repository.findByToken(token)
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Token not found: " + token)))
                .flatMap(entity -> {
                    if (VaultRecordStatus.DELETED.name().equals(entity.getStatus())) {
                        return Mono.error(new RecordDeletedException("Token already deleted: " + token));
                    }
                    entity.setStatus(VaultRecordStatus.DELETED.name());
                    entity.setPayloadCiphertext(null);
                    entity.setPayloadIv(null);
                    entity.setPayloadTagLength(null);
                    entity.setEncryptionKeyId(null);
                    entity.setWrappedDekIv(null);
                    entity.setWrappedDekCiphertext(null);
                    entity.setWrappedDekTagLength(null);
                    entity.setDeletedAt(Instant.now());
                    return repository.save(entity);
                })
                .flatMap(saved -> tokenStateCacheService.putState(saved.getToken(), saved.getStatus()).thenReturn(saved))
                .flatMap(saved -> auditService.record(
                        AuditEventType.PII_DELETED,
                        AuditOutcome.SUCCESS,
                        token,
                        actor,
                        policy.normalizedPurpose(),
                        correlationId,
                        auditDetails(saved, policy)
                ).then())
                .then();
    }

    /**
     * Re-wraps an existing token payload to the currently active KEK.
     * Legacy non-envelope rows are upgraded during the same operation.
     *
     * @param token token
     * @param actor authenticated actor
     * @param correlationId correlation id
     * @return rewrap response
     */
    @Transactional
    public Mono<RewrapTokenResponse> rewrapToActiveKey(String token, AuthenticatedActor actor, String correlationId) {
        return repository.findByToken(token)
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Token not found: " + token)))
                .flatMap(entity -> {
                    if (VaultRecordStatus.DELETED.name().equals(entity.getStatus())) {
                        return Mono.error(new RecordDeletedException("Token already deleted: " + token));
                    }
                    String previousKeyId = entity.getEncryptionKeyId();
                    PiiPayload payload = encryptionService.decrypt(toEncryptedPayload(entity), aadContext(entity), PiiPayload.class);
                    EncryptedPayload rewrapped = encryptionService.rewrapToActiveKey(toEncryptedPayload(entity), aadContext(entity), payload);
                    entity.setEncryptionKeyId(rewrapped.keyId());
                    entity.setPayloadIv(rewrapped.iv());
                    entity.setPayloadCiphertext(rewrapped.ciphertext());
                    entity.setPayloadTagLength(rewrapped.tagLength());
                    entity.setWrappedDekIv(rewrapped.wrappedDekIv());
                    entity.setWrappedDekCiphertext(rewrapped.wrappedDekCiphertext());
                    entity.setWrappedDekTagLength(rewrapped.wrappedDekTagLength());
                    return repository.save(entity)
                            .flatMap(saved -> auditService.record(
                                    AuditEventType.PII_REWRAPPED,
                                    AuditOutcome.SUCCESS,
                                    token,
                                    actor,
                                    "key-rewrap",
                                    correlationId,
                                    Map.of("previousKeyId", previousKeyId, "currentKeyId", saved.getEncryptionKeyId())
                            ).thenReturn(new RewrapTokenResponse(
                                    saved.getToken(),
                                    previousKeyId,
                                    saved.getEncryptionKeyId(),
                                    Instant.now()
                            )));
                });
    }

    /**
     * Verifies that the token is not marked as deleted in Redis.
     *
     * @param token token
     * @return completion signal
     */
    public Mono<Void> ensureTokenExistsForReference(String token) {
        return ensureNotDeleted(token)
                .then(repository.findByToken(token))
                .filter(entity -> VaultRecordStatus.ACTIVE.name().equals(entity.getStatus()))
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Active token not found: " + token)))
                .then();
    }

    private Mono<CreateVaultTokenResponse> replayExisting(IdempotencyRequestEntity existing, String requestHash, PiiClassification classification) {
        idempotencyService.ensureSameRequest(existing, requestHash);
        return repository.findByToken(existing.getToken())
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Idempotent token replay target not found: " + existing.getToken())))
                .map(entity -> new CreateVaultTokenResponse(entity.getToken(), classification, entity.getCreatedAt(), true));
    }

    private Mono<PiiVaultRecordEntity> createFresh(CreateVaultTokenRequest request, AuthenticatedActor actor, String correlationId) {
        Instant now = Instant.now();
        String token = tokenGenerator.nextToken();
        AadContext aadContext = new AadContext(token, request.subjectRef(), request.classification().name());
        EncryptedPayload encryptedPayload = encryptionService.encrypt(request.pii(), aadContext);

        PiiVaultRecordEntity entity = new PiiVaultRecordEntity();
        entity.setId(UUID.randomUUID());
        entity.setToken(token);
        entity.setStatus(VaultRecordStatus.ACTIVE.name());
        entity.setClassification(request.classification().name());
        entity.setSubjectRef(request.subjectRef());
        entity.setSubjectRefFingerprint(encryptionService.subjectRefFingerprint(request.subjectRef()));
        entity.setPayloadCiphertext(encryptedPayload.ciphertext());
        entity.setPayloadIv(encryptedPayload.iv());
        entity.setPayloadTagLength(encryptedPayload.tagLength());
        entity.setEncryptionKeyId(encryptedPayload.keyId());
        entity.setWrappedDekIv(encryptedPayload.wrappedDekIv());
        entity.setWrappedDekCiphertext(encryptedPayload.wrappedDekCiphertext());
        entity.setWrappedDekTagLength(encryptedPayload.wrappedDekTagLength());
        entity.setCreatedAt(now);
        entity.setCreatedBy(actor.username());

        return repository.save(entity)
                .flatMap(saved -> tokenStateCacheService.putState(saved.getToken(), saved.getStatus()).thenReturn(saved))
                .flatMap(saved -> auditService.record(
                        AuditEventType.PII_CREATED,
                        AuditOutcome.SUCCESS,
                        saved.getToken(),
                        actor,
                        "vault-create",
                        correlationId,
                        piiRedactor.safeAuditDetails(saved.getSubjectRef(), saved.getClassification())
                ).thenReturn(saved));
    }

    private Mono<Void> ensureNotDeleted(String token) {
        return tokenStateCacheService.getState(token)
                .flatMap(state -> VaultRecordStatus.DELETED.name().equals(state)
                        ? Mono.error(new RecordDeletedException("Token already deleted: " + token))
                        : Mono.empty())
                .switchIfEmpty(Mono.empty());
    }

    private EncryptedPayload toEncryptedPayload(PiiVaultRecordEntity entity) {
        return new EncryptedPayload(
                entity.getEncryptionKeyId(),
                entity.getPayloadIv(),
                entity.getPayloadCiphertext(),
                entity.getPayloadTagLength(),
                entity.getWrappedDekIv(),
                entity.getWrappedDekCiphertext(),
                entity.getWrappedDekTagLength()
        );
    }

    private AadContext aadContext(PiiVaultRecordEntity entity) {
        return new AadContext(entity.getToken(), entity.getSubjectRef(), entity.getClassification());
    }

    private Map<String, Object> auditDetails(PiiVaultRecordEntity entity, PurposeValidationResult policy) {
        Map<String, Object> details = new LinkedHashMap<>(piiRedactor.safeAuditDetails(entity.getSubjectRef(), entity.getClassification()));
        details.put("breakGlass", policy.breakGlass());
        if (policy.breakGlass()) {
            details.put("justification", piiRedactor.redactLongText(policy.justification()));
        }
        return details;
    }
}
