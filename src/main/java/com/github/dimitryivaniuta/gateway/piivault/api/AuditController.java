package com.github.dimitryivaniuta.gateway.piivault.api;

import com.github.dimitryivaniuta.gateway.piivault.api.model.AuditEventResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.AuditVerificationResponse;
import com.github.dimitryivaniuta.gateway.piivault.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read-only compliance audit API.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Returns recent audit events.
     *
     * @return audit events
     */
    @GetMapping("/events")
    public Flux<AuditEventResponse> list() {
        return auditService.findAll();
    }

    /**
     * Returns recent audit events for a given token.
     *
     * @param token token
     * @return audit events
     */
    @GetMapping("/events/{token}")
    public Flux<AuditEventResponse> listByToken(@PathVariable String token) {
        return auditService.findByToken(token);
    }

    /**
     * Verifies signatures of all persisted audit events.
     *
     * @return verification result
     */
    @GetMapping("/verify")
    public Mono<AuditVerificationResponse> verifyAll() {
        return auditService.verifyAll();
    }

    /**
     * Verifies signatures of audit events for a single token.
     *
     * @param token token
     * @return verification result
     */
    @GetMapping("/verify/{token}")
    public Mono<AuditVerificationResponse> verifyByToken(@PathVariable String token) {
        return auditService.verifyByToken(token);
    }
}
