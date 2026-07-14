package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.persistence.entity.IdempotencyRecordEntity;
import com.abada.engine.persistence.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @AtomicRuntimeCommand
    public Map<String, Object> execute(String key, String operation, Object request, Supplier<Map<String, Object>> command) {
        if (key == null || key.isBlank()) return command.get();
        String hash = hash(request);
        IdempotencyRecordEntity existing = repository.findById(key).orElse(null);
        if (existing != null && existing.getExpiresAt().isAfter(Instant.now())) {
            if (!existing.getOperation().equals(operation) || !existing.getRequestHash().equals(hash)) {
                throw new ProcessEngineException("Idempotency-Key was already used for a different request");
            }
            try {
                return objectMapper.readValue(existing.getResponseBody(), new TypeReference<>() {});
            } catch (Exception ex) {
                throw new IllegalStateException("Stored idempotent response is invalid", ex);
            }
        }

        Map<String, Object> response = command.get();
        try {
            IdempotencyRecordEntity record = new IdempotencyRecordEntity();
            record.setIdempotencyKey(key);
            record.setOperation(operation);
            record.setRequestHash(hash);
            record.setResponseStatus(200);
            record.setResponseBody(objectMapper.writeValueAsString(response));
            record.setCreatedAt(Instant.now());
            record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            repository.save(record);
            return response;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not store idempotent response", ex);
        }
    }

    private String hash(Object request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash idempotent request", ex);
        }
    }
}
