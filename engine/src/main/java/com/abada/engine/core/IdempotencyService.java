package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.persistence.entity.IdempotencyRecordEntity;
import com.abada.engine.persistence.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final boolean postgres;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper,
            Environment environment) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.postgres = environment.getProperty("spring.datasource.url", "").startsWith("jdbc:postgresql:");
    }

    @AtomicRuntimeCommand
    public Map<String, Object> execute(String key, String operation, Object request, Supplier<Map<String, Object>> command) {
        return execute(key, operation, request, new TypeReference<>() {}, command);
    }

    @AtomicRuntimeCommand
    public <T> T execute(String key, String operation, Object request, TypeReference<T> responseType,
            Supplier<T> command) {
        if (key == null || key.isBlank()) return command.get();
        String hash = hash(request);
        Instant now = Instant.now();
        repository.deleteExpired(key, now);
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);
        int reserved;
        IdempotencyRecordEntity existing;
        if (postgres) {
            reserved = repository.reserve(key, operation, hash, now, expiresAt);
            existing = repository.findById(key)
                    .orElseThrow(() -> new IllegalStateException("Idempotency reservation disappeared: " + key));
        } else {
            existing = repository.findById(key).orElse(null);
            reserved = existing == null ? 1 : 0;
            if (existing == null) {
                existing = newReservation(key, operation, hash, now, expiresAt);
                repository.saveAndFlush(existing);
            }
        }
        if (reserved == 0) {
            if (!existing.getOperation().equals(operation) || !existing.getRequestHash().equals(hash)) {
                throw new ProcessEngineException("Idempotency-Key was already used for a different request");
            }
            try {
                return objectMapper.readValue(existing.getResponseBody(), responseType);
            } catch (Exception ex) {
                throw new IllegalStateException("Stored idempotent response is invalid", ex);
            }
        }

        T response = command.get();
        try {
            existing.setResponseStatus(200);
            existing.setResponseBody(objectMapper.writeValueAsString(response));
            repository.save(existing);
            return response;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not store idempotent response", ex);
        }
    }

    private IdempotencyRecordEntity newReservation(String key, String operation, String hash,
            Instant now, Instant expiresAt) {
        IdempotencyRecordEntity reservation = new IdempotencyRecordEntity();
        reservation.setIdempotencyKey(key);
        reservation.setOperation(operation);
        reservation.setRequestHash(hash);
        reservation.setResponseStatus(0);
        reservation.setResponseBody("{}");
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(expiresAt);
        return reservation;
    }

    private String hash(Object request) {
        try {
            byte[] json = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash idempotent request", ex);
        }
    }
}
