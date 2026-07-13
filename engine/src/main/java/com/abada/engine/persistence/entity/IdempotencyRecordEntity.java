package com.abada.engine.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    private String idempotencyKey;
    @Column(nullable = false)
    private String operation;
    @Column(nullable = false, length = 64)
    private String requestHash;
    @Column(nullable = false)
    private int responseStatus;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseBody;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public String getOperation() { return operation; }
    public void setOperation(String value) { operation = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash = value; }
    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int value) { responseStatus = value; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String value) { responseBody = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
}
