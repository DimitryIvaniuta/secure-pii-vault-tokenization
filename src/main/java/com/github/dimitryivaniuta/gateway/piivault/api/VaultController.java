package com.github.dimitryivaniuta.gateway.piivault.api;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.VaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticationFacade;
import com.github.dimitryivaniuta.gateway.piivault.service.VaultService;
import com.github.dimitryivaniuta.gateway.piivault.web.CorrelationIdWebFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST API for secure PII vault operations.
 */
@RestController
@RequestMapping("/api/v1/vault/tokens")
@Validated
public class VaultController {

    private final VaultService vaultService;
    private final AuthenticationFacade authenticationFacade;

    public VaultController(VaultService vaultService, AuthenticationFacade authenticationFacade) {
        this.vaultService = vaultService;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Stores PII in the encrypted vault and returns an opaque token.
     *
     * @param request create request
     * @param idempotencyKey optional idempotency key
     * @param exchange current exchange
     * @return create response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateVaultTokenResponse> create(
            @Valid @RequestBody CreateVaultTokenRequest request,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            ServerWebExchange exchange
    ) {
        return authenticationFacade.currentActor()
                .flatMap(actor -> vaultService.create(request, idempotencyKey, actor, CorrelationIdWebFilter.correlationId(exchange)));
    }

    /**
     * Resolves a token to decrypted PII for privileged actors.
     *
     * @param token token
     * @param purpose purpose of use
     * @param breakGlassJustification optional break-glass justification
     * @param exchange current exchange
     * @return resolved payload
     */
    @GetMapping("/{token}")
    public Mono<VaultTokenResponse> resolve(
            @PathVariable String token,
            @RequestHeader("X-Purpose-Of-Use") @NotBlank String purpose,
            @RequestHeader(name = "X-Break-Glass-Justification", required = false) String breakGlassJustification,
            ServerWebExchange exchange
    ) {
        return authenticationFacade.currentActor()
                .flatMap(actor -> vaultService.resolve(
                        token,
                        purpose,
                        breakGlassJustification,
                        actor,
                        CorrelationIdWebFilter.correlationId(exchange)
                ));
    }

    /**
     * Deletes PII payload bytes for a token to satisfy GDPR erase requirements.
     *
     * @param token token
     * @param purpose purpose of delete
     * @param breakGlassJustification optional break-glass justification
     * @param exchange current exchange
     * @return completion signal
     */
    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable String token,
            @RequestHeader("X-Purpose-Of-Use") @NotBlank String purpose,
            @RequestHeader(name = "X-Break-Glass-Justification", required = false) String breakGlassJustification,
            ServerWebExchange exchange
    ) {
        return authenticationFacade.currentActor()
                .flatMap(actor -> vaultService.delete(
                        token,
                        purpose,
                        breakGlassJustification,
                        actor,
                        CorrelationIdWebFilter.correlationId(exchange)
                ));
    }
}
