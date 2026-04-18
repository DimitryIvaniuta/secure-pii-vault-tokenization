package com.github.dimitryivaniuta.gateway.piivault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security configuration with local role-based users for repeatable local execution.
 */
@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Shared password encoder.
     *
     * @return delegating password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Creates local users for all demo roles.
     *
     * @param properties configured usernames and raw passwords
     * @param passwordEncoder password encoder
     * @return reactive user store
     */
    @Bean
    MapReactiveUserDetailsService mapReactiveUserDetailsService(
            AppSecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        return new MapReactiveUserDetailsService(
                User.withUsername(properties.getUsers().getWriter().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getWriter().getPassword()))
                        .roles("PII_WRITE")
                        .build(),
                User.withUsername(properties.getUsers().getReader().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getReader().getPassword()))
                        .roles("PII_READ")
                        .build(),
                User.withUsername(properties.getUsers().getDelete().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getDelete().getPassword()))
                        .roles("PII_DELETE", "PII_READ")
                        .build(),
                User.withUsername(properties.getUsers().getAuditor().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getAuditor().getPassword()))
                        .roles("AUDIT_READ")
                        .build(),
                User.withUsername(properties.getUsers().getTokenClient().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getTokenClient().getPassword()))
                        .roles("TOKEN_CLIENT")
                        .build(),
                User.withUsername(properties.getUsers().getOperations().getUsername())
                        .password(passwordEncoder.encode(properties.getUsers().getOperations().getPassword()))
                        .roles("PII_OPERATIONS", "PII_READ")
                        .build()
        );
    }

    /**
     * Configures route-level authorization for the API.
     *
     * @param http server security DSL
     * @return filter chain
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/vault/tokens").hasRole("PII_WRITE")
                        .pathMatchers(HttpMethod.POST, "/api/v1/vault/admin/tokens/*/rewrap").hasRole("PII_OPERATIONS")
                        .pathMatchers(HttpMethod.GET, "/api/v1/vault/admin/keys").hasRole("PII_OPERATIONS")
                        .pathMatchers(HttpMethod.GET, "/api/v1/vault/tokens/**").hasRole("PII_READ")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/vault/tokens/**").hasRole("PII_DELETE")
                        .pathMatchers(HttpMethod.GET, "/api/v1/audit/verify/**").hasRole("AUDIT_READ")
                        .pathMatchers("/api/v1/audit/events/**").hasRole("AUDIT_READ")
                        .pathMatchers(HttpMethod.POST, "/api/v1/customers").hasRole("TOKEN_CLIENT")
                        .pathMatchers(HttpMethod.GET, "/api/v1/customers/*/pii").hasRole("PII_READ")
                        .pathMatchers(HttpMethod.GET, "/api/v1/customers/**").hasAnyRole("TOKEN_CLIENT", "PII_READ")
                        .anyExchange().authenticated()
                )
                .build();
    }
}
