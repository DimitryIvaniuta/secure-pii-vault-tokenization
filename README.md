# secure-pii-vault-tokenization

Production-style **Secure PII Vault + Tokenization** service built with **Java 21**, **Spring Boot 4.0.5**, **Gradle**, **WebFlux**, **R2DBC**, **PostgreSQL**, **Redis**, **Kafka (KRaft)**, **Flyway**, and **Lombok**.

## GitHub repository suggestion

- **Repository name:** `secure-pii-vault-tokenization`
- **Description:** `Secure PII vault and tokenization service with envelope encryption, audited access, purpose-based control, idempotent tokenization, GDPR delete, Redis metadata cache, and Kafka audit events.`

## What this project now implements

### Core security model
- PII is stored only in the vault table and encrypted using **per-record DEKs wrapped by active KEKs**.
- Downstream business-facing services store only **opaque tokens** such as `tok_xxx`.
- Reads, writes, deletes, and key re-wrap operations are **audited** in PostgreSQL and also published to Kafka.
- Persisted audit events are **HMAC-signed** so the system can verify tamper evidence later.
- Redis stores only **non-sensitive token state** such as ACTIVE or DELETED.
- GDPR delete scrubs ciphertext, wrapped DEKs, and key references so the token remains harmless while the actual PII is gone.

### Production-grade improvements added in this revision
- **True envelope encryption** instead of encrypting payloads directly with the master key.
- **Purpose-of-use policy enforcement** for read and delete paths.
- **Break-glass support** requiring explicit justification.
- **Idempotent token creation** with `X-Idempotency-Key` and request hashing.
- **Online key re-wrap endpoint** for safe KEK rotation.
- **Audit signature verification endpoints** for integrity checking.
- **Subject reference fingerprinting** for safer indexing without storing more sensitive lookup material.
- **Prometheus registry dependency** so the exposed endpoint is backed by a real registry.

## Role model

| Role | Purpose |
|---|---|
| `PII_WRITE` | create tokenized PII records |
| `PII_READ` | reveal PII for approved business purpose |
| `PII_DELETE` | execute GDPR delete |
| `AUDIT_READ` | inspect audit trail and verify integrity |
| `TOKEN_CLIENT` | create/read token-only downstream records |
| `PII_OPERATIONS` | inspect key metadata and perform token re-wrap |

## Default demo users

| Username | Roles |
|---|---|
| `vault-writer` | `PII_WRITE` |
| `vault-reader` | `PII_READ` |
| `privacy-admin` | `PII_DELETE`, `PII_READ` |
| `vault-auditor` | `AUDIT_READ` |
| `token-client` | `TOKEN_CLIENT` |
| `vault-ops` | `PII_OPERATIONS`, `PII_READ` |

Passwords are loaded from environment variables and encoded at startup.

## API summary

### Vault API
- `POST /api/v1/vault/tokens` – store PII and return token
  - optional header: `X-Idempotency-Key`
- `GET /api/v1/vault/tokens/{token}` – resolve token to PII
  - required header: `X-Purpose-Of-Use`
  - optional header: `X-Break-Glass-Justification`
- `DELETE /api/v1/vault/tokens/{token}` – GDPR delete
  - required header: `X-Purpose-Of-Use`
  - optional header: `X-Break-Glass-Justification`

### Vault operations API
- `GET /api/v1/vault/admin/keys` – safe key metadata
- `POST /api/v1/vault/admin/tokens/{token}/rewrap` – re-wrap token payload to current active key

### Token-only customer API
- `POST /api/v1/customers` – create business record with token only
- `GET /api/v1/customers/{id}` – read token-only customer record
- `GET /api/v1/customers/{id}/pii` – resolve linked token to PII through vault security

### Audit API
- `GET /api/v1/audit/events`
- `GET /api/v1/audit/events/{token}`
- `GET /api/v1/audit/verify`
- `GET /api/v1/audit/verify/{token}`

## Data model

### `pii_vault_record`
Encrypted payload and minimal metadata.
- `token`
- `status`
- `classification`
- `subject_ref`
- `subject_ref_fingerprint`
- `payload_ciphertext`
- `payload_iv`
- `payload_tag_length`
- `encryption_key_id`
- `wrapped_dek_iv`
- `wrapped_dek_ciphertext`
- `wrapped_dek_tag_length`
- `created_at`
- `deleted_at`

### `customer_profile`
Token-only downstream record.
- `external_ref`
- `pii_token`
- `customer_status`

### `audit_event`
Audit trail for create/read/delete/re-wrap actions.
- `event_type`
- `outcome`
- `token`
- `actor`
- `purpose`
- `correlation_id`
- `details_json`
- `signature_version`
- `event_signature`

### `vault_idempotency_request`
Replay-safe request registry.
- `actor`
- `idempotency_key`
- `request_hash`
- `token`
- `created_at`

## Purpose policy defaults

### Read purposes
- `customer-support`
- `fraud-investigation`
- `regulatory-response`
- `break-glass-emergency`

### Delete purposes
- `gdpr-delete-request`
- `gdpr-data-subject-request`
- `retention-expiry`

### Break-glass purpose
- `break-glass-emergency`
  - requires `X-Break-Glass-Justification`

## Local run

### 1. Start infrastructure
```bash
cp .env.example .env
docker compose up -d
```

### 2. Start the application
```bash
./gradlew bootRun
# or, if the wrapper JAR has not been generated yet
# gradle wrapper --gradle-version 8.14.4
# ./gradlew bootRun
```

### 3. Useful environment variables
```bash
export SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/piivault
export SPRING_R2DBC_USERNAME=piivault
export SPRING_R2DBC_PASSWORD=piivault

export SPRING_FLYWAY_URL=jdbc:postgresql://localhost:5432/piivault
export SPRING_FLYWAY_USER=piivault
export SPRING_FLYWAY_PASSWORD=piivault

export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

export APP_SECURITY_USERS_WRITER_PASSWORD=changeit-writer
export APP_SECURITY_USERS_READER_PASSWORD=changeit-reader
export APP_SECURITY_USERS_DELETE_PASSWORD=changeit-delete
export APP_SECURITY_USERS_AUDITOR_PASSWORD=changeit-auditor
export APP_SECURITY_USERS_TOKEN_CLIENT_PASSWORD=changeit-client
export APP_SECURITY_USERS_OPERATIONS_PASSWORD=changeit-ops

export APP_VAULT_CRYPTO_ACTIVE_KEY_ID=k1
export APP_VAULT_CRYPTO_KEYS_K1=FMHj1sgQSUQ+Ymh16NfMptLxIIDc5/M4cSLAXF2N+4k=
export APP_VAULT_CRYPTO_KEYS_K2=5oGzgVSB2OWbEFxnasmRjWaN/vg6sCRAHnh1+oqKLW0=
export APP_AUDIT_INTEGRITY_KEY=ZFA9WHPcP5IOKyQ5HP3haFFkuT9p4JymQ+0Wi6+N6cs=
```

## Example flow

### Create tokenized PII
```bash
curl -u vault-writer:changeit-writer \
  -H 'Content-Type: application/json' \
  -H 'X-Idempotency-Key: create-customer-1001' \
  -d '{
    "subjectRef": "customer-1001",
    "classification": "RESTRICTED",
    "pii": {
      "fullName": "Alice Doe",
      "email": "alice@example.com",
      "phoneNumber": "+48111222333",
      "nationalId": "PL12345678",
      "addressLine1": "Gdansk Street 1",
      "dateOfBirth": "1990-01-01"
    }
  }' \
  http://localhost:8080/api/v1/vault/tokens
```

### Break-glass read
```bash
curl -u vault-reader:changeit-reader \
  -H 'X-Purpose-Of-Use: break-glass-emergency' \
  -H 'X-Break-Glass-Justification: Critical fraud investigation after customer lockout' \
  http://localhost:8080/api/v1/vault/tokens/<TOKEN>
```

### Verify audit integrity
```bash
curl -u vault-auditor:changeit-auditor \
  http://localhost:8080/api/v1/audit/verify
```

### Re-wrap to current active key
```bash
curl -u vault-ops:changeit-ops \
  -X POST \
  http://localhost:8080/api/v1/vault/admin/tokens/<TOKEN>/rewrap
```

## Test scope

The project includes:
- envelope encryption unit tests
- audit signing tests
- purpose policy tests
- redaction tests
- full integration flow tests with Testcontainers for PostgreSQL, Redis, and Kafka

## Postman

See:
- `postman/Secure-PII-Vault.postman_collection.json`

## Notes

- This project intentionally uses local basic-auth demo users so the sample can run standalone.
- In a real production deployment, replace local users with OIDC/JWT and store KEKs in an HSM or cloud KMS.
- The source bundle was hardened for production-style design, but full Gradle execution was not possible in this container because Gradle is not installed here.
