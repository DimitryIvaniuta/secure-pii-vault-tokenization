package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateCustomerRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CustomerResponse;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditEventType;
import com.github.dimitryivaniuta.gateway.piivault.domain.AuditOutcome;
import com.github.dimitryivaniuta.gateway.piivault.entity.CustomerProfileEntity;
import com.github.dimitryivaniuta.gateway.piivault.exception.RecordNotFoundException;
import com.github.dimitryivaniuta.gateway.piivault.repository.CustomerProfileRepository;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticatedActor;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Example business service that stores only opaque tokens, never decrypted PII fields.
 */
@Service
public class CustomerService {

    private final CustomerProfileRepository repository;
    private final VaultService vaultService;
    private final AuditService auditService;

    public CustomerService(CustomerProfileRepository repository, VaultService vaultService, AuditService auditService) {
        this.repository = repository;
        this.vaultService = vaultService;
        this.auditService = auditService;
    }

    /**
     * Creates a new customer profile that stores only a token reference.
     *
     * @param request customer creation request
     * @param actor authenticated actor
     * @param correlationId correlation id
     * @return token-only customer response
     */
    @Transactional
    public Mono<CustomerResponse> create(CreateCustomerRequest request, AuthenticatedActor actor, String correlationId) {
        return vaultService.ensureTokenExistsForReference(request.piiToken())
                .then(repository.findByExternalRef(request.externalRef())
                        .flatMap(existing -> Mono.error(new IllegalArgumentException(
                                "External reference already exists: " + existing.getExternalRef())))
                        .then())
                .then(Mono.defer(() -> {
                    CustomerProfileEntity entity = new CustomerProfileEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setExternalRef(request.externalRef());
                    entity.setPiiToken(request.piiToken());
                    entity.setCustomerStatus(request.customerStatus());
                    entity.setCreatedAt(Instant.now());
                    entity.setCreatedBy(actor.username());
                    return repository.save(entity);
                }))
                .flatMap(saved -> auditService.record(
                        AuditEventType.CUSTOMER_CREATED,
                        AuditOutcome.SUCCESS,
                        saved.getPiiToken(),
                        actor,
                        "customer-create",
                        correlationId,
                        Map.of("externalRef", saved.getExternalRef(), "customerStatus", saved.getCustomerStatus())
                ).thenReturn(toResponse(saved)));
    }

    /**
     * Returns a token-only customer profile.
     *
     * @param customerId customer id
     * @return customer response
     */
    public Mono<CustomerResponse> get(UUID customerId) {
        return repository.findById(customerId)
                .switchIfEmpty(Mono.error(new RecordNotFoundException("Customer not found: " + customerId)))
                .map(this::toResponse);
    }

    private CustomerResponse toResponse(CustomerProfileEntity entity) {
        return new CustomerResponse(entity.getId(), entity.getExternalRef(), entity.getPiiToken(), entity.getCustomerStatus(), entity.getCreatedAt());
    }
}
