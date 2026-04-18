package com.github.dimitryivaniuta.gateway.piivault.api;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CryptoKeyMetadataResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.RewrapTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticationFacade;
import com.github.dimitryivaniuta.gateway.piivault.service.CryptoAdminService;
import com.github.dimitryivaniuta.gateway.piivault.service.VaultService;
import com.github.dimitryivaniuta.gateway.piivault.web.CorrelationIdWebFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Operations endpoints for key-management visibility and online re-wrap.
 */
@RestController
@RequestMapping("/api/v1/vault/admin")
public class VaultAdminController {

    private final CryptoAdminService cryptoAdminService;
    private final VaultService vaultService;
    private final AuthenticationFacade authenticationFacade;

    public VaultAdminController(
            CryptoAdminService cryptoAdminService,
            VaultService vaultService,
            AuthenticationFacade authenticationFacade
    ) {
        this.cryptoAdminService = cryptoAdminService;
        this.vaultService = vaultService;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Returns safe key-ring metadata.
     *
     * @return key metadata
     */
    @GetMapping("/keys")
    public Mono<CryptoKeyMetadataResponse> keyMetadata() {
        return cryptoAdminService.keyMetadata();
    }

    /**
     * Re-wraps a token payload to the active key.
     *
     * @param token token
     * @param exchange current exchange
     * @return rewrap response
     */
    @PostMapping("/tokens/{token}/rewrap")
    public Mono<RewrapTokenResponse> rewrap(@PathVariable String token, ServerWebExchange exchange) {
        return authenticationFacade.currentActor()
                .flatMap(actor -> vaultService.rewrapToActiveKey(token, actor, CorrelationIdWebFilter.correlationId(exchange)));
    }
}
