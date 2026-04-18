package com.github.dimitryivaniuta.gateway.piivault.web;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Ensures each request has a correlation id for audit traceability.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter {

    /**
     * Request header used for correlation id propagation.
     */
    public static final String HEADER_NAME = "X-Correlation-Id";

    /**
     * Exchange attribute name used internally.
     */
    public static final String ATTRIBUTE_NAME = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        exchange.getAttributes().put(ATTRIBUTE_NAME, correlationId);
        exchange.getResponse().getHeaders().add(HEADER_NAME, correlationId);
        return chain.filter(exchange);
    }

    /**
     * Resolves a request correlation id from the exchange.
     *
     * @param exchange web exchange
     * @return correlation id
     */
    public static String correlationId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(ATTRIBUTE_NAME);
        return value == null ? "unknown" : value.toString();
    }
}
