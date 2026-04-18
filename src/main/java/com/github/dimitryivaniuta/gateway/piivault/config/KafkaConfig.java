package com.github.dimitryivaniuta.gateway.piivault.config;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka producer configuration for audit event streaming.
 */
@Configuration
public class KafkaConfig {

    /**
     * Creates a string Kafka template for JSON audit messages.
     *
     * @param bootstrapServers configured Kafka bootstrap servers
     * @return Kafka template
     */
    @Bean
    KafkaTemplate<String, String> kafkaTemplate(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
