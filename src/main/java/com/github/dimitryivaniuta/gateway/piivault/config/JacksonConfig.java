package com.github.dimitryivaniuta.gateway.piivault.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration used for encrypted payload serialization and API JSON rendering.
 */
@Configuration
public class JacksonConfig {

    /**
     * Shared object mapper configured for stable JSON processing.
     *
     * @return configured object mapper
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
