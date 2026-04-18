package com.github.dimitryivaniuta.gateway.piivault.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reads the current reactive authentication and exposes a compact actor model.
 */
@Component
public class AuthenticationFacade {

    /**
     * Returns the current authenticated actor.
     *
     * @return authenticated actor
     */
    public Mono<AuthenticatedActor> currentActor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> toActor(context.getAuthentication()));
    }

    private AuthenticatedActor toActor(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new AuthenticatedActor(authentication.getName(), roles);
    }
}
