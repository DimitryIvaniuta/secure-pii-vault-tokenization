package com.github.dimitryivaniuta.gateway.piivault.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.piivault.api.model.AuditVerificationResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateCustomerRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenRequest;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CreateVaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.CustomerResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.PiiPayload;
import com.github.dimitryivaniuta.gateway.piivault.api.model.RewrapTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.api.model.VaultTokenResponse;
import com.github.dimitryivaniuta.gateway.piivault.domain.PiiClassification;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Full integration flow proving tokenization, privileged read, idempotent create, re-wrap, and GDPR delete.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class VaultTokenFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("piivault")
            .withUsername("piivault")
            .withPassword("piivault");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4")).withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%s/%s".formatted(
                postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.vault.crypto.active-key-id", () -> "k1");
        registry.add("app.vault.crypto.keys.k1", () -> "FMHj1sgQSUQ+Ymh16NfMptLxIIDc5/M4cSLAXF2N+4k=");
        registry.add("app.vault.crypto.keys.k2", () -> "5oGzgVSB2OWbEFxnasmRjWaN/vg6sCRAHnh1+oqKLW0=");
        registry.add("app.audit.integrity-key", () -> "ZFA9WHPcP5IOKyQ5HP3haFFkuT9p4JymQ+0Wi6+N6cs=");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateResolveRewrapAndDeleteTokenizedPii() {
        CreateVaultTokenRequest createVaultTokenRequest = new CreateVaultTokenRequest(
                "customer-1001",
                PiiClassification.RESTRICTED,
                new PiiPayload(
                        "Alice Doe",
                        "alice@example.com",
                        "+48111222333",
                        "PL12345678",
                        "Gdansk Street 1",
                        LocalDate.of(1990, 1, 1)
                )
        );

        CreateVaultTokenResponse tokenResponse = webTestClient.post()
                .uri("/api/v1/vault/tokens")
                .header("X-Idempotency-Key", "create-customer-1001")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth("vault-writer", "changeit-writer"))
                .bodyValue(createVaultTokenRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateVaultTokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.token()).startsWith("tok_");
        assertThat(tokenResponse.idempotentReplay()).isFalse();

        CreateVaultTokenResponse replayResponse = webTestClient.post()
                .uri("/api/v1/vault/tokens")
                .header("X-Idempotency-Key", "create-customer-1001")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth("vault-writer", "changeit-writer"))
                .bodyValue(createVaultTokenRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateVaultTokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(replayResponse).isNotNull();
        assertThat(replayResponse.token()).isEqualTo(tokenResponse.token());
        assertThat(replayResponse.idempotentReplay()).isTrue();

        CustomerResponse customerResponse = webTestClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth("token-client", "changeit-client"))
                .bodyValue(new CreateCustomerRequest("ext-1", tokenResponse.token(), "ACTIVE"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(customerResponse).isNotNull();
        assertThat(customerResponse.piiToken()).isEqualTo(tokenResponse.token());

        VaultTokenResponse resolved = webTestClient.get()
                .uri("/api/v1/vault/tokens/{token}", tokenResponse.token())
                .header("X-Purpose-Of-Use", "customer-support")
                .headers(headers -> headers.setBasicAuth("vault-reader", "changeit-reader"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(VaultTokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(resolved).isNotNull();
        assertThat(resolved.pii().email()).isEqualTo("alice@example.com");

        RewrapTokenResponse rewrapResponse = webTestClient.post()
                .uri("/api/v1/vault/admin/tokens/{token}/rewrap", tokenResponse.token())
                .headers(headers -> headers.setBasicAuth("vault-ops", "changeit-ops"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RewrapTokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(rewrapResponse).isNotNull();
        assertThat(rewrapResponse.currentKeyId()).isEqualTo("k1");

        AuditVerificationResponse auditVerificationResponse = webTestClient.get()
                .uri("/api/v1/audit/verify/{token}", tokenResponse.token())
                .headers(headers -> headers.setBasicAuth("vault-auditor", "changeit-auditor"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuditVerificationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(auditVerificationResponse).isNotNull();
        assertThat(auditVerificationResponse.invalidEvents()).isZero();

        webTestClient.delete()
                .uri("/api/v1/vault/tokens/{token}", tokenResponse.token())
                .header("X-Purpose-Of-Use", "gdpr-delete-request")
                .headers(headers -> headers.setBasicAuth("privacy-admin", "changeit-delete"))
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/v1/vault/tokens/{token}", tokenResponse.token())
                .header("X-Purpose-Of-Use", "customer-support")
                .headers(headers -> headers.setBasicAuth("vault-reader", "changeit-reader"))
                .exchange()
                .expectStatus().isEqualTo(410);
    }
}
