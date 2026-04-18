package com.github.dimitryivaniuta.gateway.piivault.security;

import java.util.Set;

/**
 * Value object describing the current authenticated actor.
 *
 * @param username principal name
 * @param roles role names without framework prefixes
 */
public record AuthenticatedActor(String username, Set<String> roles) {
}
