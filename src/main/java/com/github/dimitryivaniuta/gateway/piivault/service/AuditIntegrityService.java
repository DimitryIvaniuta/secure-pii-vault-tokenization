package com.github.dimitryivaniuta.gateway.piivault.service;

import com.github.dimitryivaniuta.gateway.piivault.config.AppAuditProperties;
import com.github.dimitryivaniuta.gateway.piivault.entity.AuditEventEntity;
import com.github.dimitryivaniuta.gateway.piivault.exception.CryptoOperationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Signs audit events so persisted records can be checked for tampering.
 */
@Service
public class AuditIntegrityService {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SIGNATURE_VERSION = "hmac-sha256-v1";

    private final AppAuditProperties properties;

    public AuditIntegrityService(AppAuditProperties properties) {
        this.properties = properties;
    }

    /**
     * Signs the supplied audit event.
     *
     * @param entity audit event
     * @return base64url signature
     */
    public String sign(AuditEventEntity entity) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(properties.getIntegrityKey()), HMAC_SHA_256));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(canonical(entity).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new CryptoOperationException("Failed to sign audit event", exception);
        }
    }

    /**
     * Checks whether the stored signature is still valid.
     *
     * @param entity audit event
     * @return true when the signature matches
     */
    public boolean verify(AuditEventEntity entity) {
        return sign(entity).equals(entity.getEventSignature());
    }

    /**
     * Returns the signature version identifier.
     *
     * @return signature version
     */
    public String signatureVersion() {
        return SIGNATURE_VERSION;
    }

    private String canonical(AuditEventEntity entity) {
        return String.join("|",
                nullSafe(entity.getId()),
                nullSafe(entity.getEventType()),
                nullSafe(entity.getOutcome()),
                nullSafe(entity.getToken()),
                nullSafe(entity.getActor()),
                nullSafe(entity.getActorRoles()),
                nullSafe(entity.getPurpose()),
                nullSafe(entity.getCorrelationId()),
                nullSafe(entity.getDetailsJson()),
                nullSafe(entity.getCreatedAt()));
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
