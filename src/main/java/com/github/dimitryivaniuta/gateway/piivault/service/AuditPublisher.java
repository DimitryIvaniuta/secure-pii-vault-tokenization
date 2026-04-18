package com.github.dimitryivaniuta.gateway.piivault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.piivault.config.AppAuditProperties;
import com.github.dimitryivaniuta.gateway.piivault.entity.AuditEventEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Streams persisted audit events to Kafka for external compliance consumers.
 */
@Service
public class AuditPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AppAuditProperties properties;

    public AuditPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, AppAuditProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Publishes an audit event asynchronously.
     *
     * @param event persisted event
     * @return completion signal
     */
    public Mono<Void> publish(AuditEventEntity event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            return Mono.fromFuture(kafkaTemplate.send(properties.getTopicName(), event.getCorrelationId(), payload))
                    .then();
        } catch (JsonProcessingException exception) {
            return Mono.error(exception);
        }
    }
}
