package com.github.dimitryivaniuta.gateway.piivault.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Encrypted vault record that stores only ciphertext and safe metadata.
 */
@Table("pii_vault_record")
public class PiiVaultRecordEntity {

    @Id
    private UUID id;
    private String token;
    private String status;
    private String classification;
    private String subjectRef;
    private String subjectRefFingerprint;
    private byte[] payloadCiphertext;
    private byte[] payloadIv;
    private Integer payloadTagLength;
    private String encryptionKeyId;
    private byte[] wrappedDekIv;
    private byte[] wrappedDekCiphertext;
    private Integer wrappedDekTagLength;
    private Instant createdAt;
    private String createdBy;
    private Instant lastAccessedAt;
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public void setSubjectRef(String subjectRef) {
        this.subjectRef = subjectRef;
    }

    public String getSubjectRefFingerprint() {
        return subjectRefFingerprint;
    }

    public void setSubjectRefFingerprint(String subjectRefFingerprint) {
        this.subjectRefFingerprint = subjectRefFingerprint;
    }

    public byte[] getPayloadCiphertext() {
        return payloadCiphertext;
    }

    public void setPayloadCiphertext(byte[] payloadCiphertext) {
        this.payloadCiphertext = payloadCiphertext;
    }

    public byte[] getPayloadIv() {
        return payloadIv;
    }

    public void setPayloadIv(byte[] payloadIv) {
        this.payloadIv = payloadIv;
    }

    public Integer getPayloadTagLength() {
        return payloadTagLength;
    }

    public void setPayloadTagLength(Integer payloadTagLength) {
        this.payloadTagLength = payloadTagLength;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public byte[] getWrappedDekIv() {
        return wrappedDekIv;
    }

    public void setWrappedDekIv(byte[] wrappedDekIv) {
        this.wrappedDekIv = wrappedDekIv;
    }

    public byte[] getWrappedDekCiphertext() {
        return wrappedDekCiphertext;
    }

    public void setWrappedDekCiphertext(byte[] wrappedDekCiphertext) {
        this.wrappedDekCiphertext = wrappedDekCiphertext;
    }

    public Integer getWrappedDekTagLength() {
        return wrappedDekTagLength;
    }

    public void setWrappedDekTagLength(Integer wrappedDekTagLength) {
        this.wrappedDekTagLength = wrappedDekTagLength;
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

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
