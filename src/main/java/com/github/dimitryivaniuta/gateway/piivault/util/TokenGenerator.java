package com.github.dimitryivaniuta.gateway.piivault.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Generates opaque token references suitable for downstream storage.
 */
@Component
public class TokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a token with a stable prefix and high-entropy random body.
     *
     * @return opaque token
     */
    public String nextToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "tok_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
