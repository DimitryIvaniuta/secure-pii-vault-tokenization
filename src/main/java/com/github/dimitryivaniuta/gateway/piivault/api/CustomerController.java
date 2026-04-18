package com.github.dimitryivaniuta.gateway.piivault.api;

import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateCustomerRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CustomerResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.VaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.repository.CustomerProfileRepository;
import com.github.dimitryivaniuta.gateway.piivault.security.AuthenticationFacade;
import com.github.dimitryivaniuta.gateway.piivault.service.CustomerService;
import com.github.dimitryivaniuta.gateway.piivault.service.VaultService;
import com.github.dimitryivaniuta.gateway.piivault.web.CorrelationIdWebFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
 * REST API demonstrating that downstream services store only vault tokens.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerProfileRepository customerProfileRepository;
    private final VaultService vaultService;
    private final AuthenticationFacade authenticationFacade;

    public CustomerController(
            CustomerService customerService,
            CustomerProfileRepository customerProfileRepository,
            VaultService vaultService,
            AuthenticationFacade authenticationFacade
    ) {
        this.customerService = customerService;
        this.customerProfileRepository = customerProfileRepository;
        this.vaultService = vaultService;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Creates a customer record that stores a token reference only.
     *
     * @param request request body
     * @param exchange current exchange
     * @return customer response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CustomerResponse> create(
            @Valid @RequestBody CreateCustomerRequest request,
            ServerWebExchange exchange
    ) {
        return authenticationFacade.currentActor()
                .flatMap(actor -> customerService.create(request, actor, CorrelationIdWebFilter.correlationId(exchange)));
    }

    /**
     * Returns the downstream token-only customer record.
     *
     * @param customerId customer id
     * @return customer response
     */
    @GetMapping("/{customerId}")
    public Mono<CustomerResponse> get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    /**
     * Resolves PII for a customer by dereferencing the stored token through the vault.
     *
     * @param customerId customer id
     * @param purpose purpose of use
     * @param breakGlassJustification optional break-glass justification
     * @param exchange current exchange
     * @return resolved PII payload
     */
    @GetMapping("/{customerId}/pii")
    public Mono<VaultTokenResponse> getPii(
            @PathVariable UUID customerId,
            @RequestHeader("X-Purpose-Of-Use") @NotBlank String purpose,
            @RequestHeader(name = "X-Break-Glass-Justification", required = false) String breakGlassJustification,
            ServerWebExchange exchange
    ) {
        return customerProfileRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new com.github.dimitryivaniuta.gateway.piivault.exception.RecordNotFoundException(
                        "Customer not found: " + customerId)))
                .flatMap(profile -> authenticationFacade.currentActor()
                        .flatMap(actor -> vaultService.resolve(
                                profile.getPiiToken(),
                                purpose,
                                breakGlassJustification,
                                actor,
                                CorrelationIdWebFilter.correlationId(exchange)
                        )));
    }
}
