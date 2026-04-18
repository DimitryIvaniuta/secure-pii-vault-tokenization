package com.github.dimitryivaniuta.gateway.piivault.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Example downstream record that stores only a PII token and business metadata.
 */
@Table("customer_profile")
public class CustomerProfileEntity {

    @Id
    private UUID id;
    private String externalRef;
    private String piiToken;
    private String customerStatus;
    private Instant createdAt;
    private String createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getPiiToken() {
        return piiToken;
    }

    public void setPiiToken(String piiToken) {
        this.piiToken = piiToken;
    }

    public String getCustomerStatus() {
        return customerStatus;
    }

    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
