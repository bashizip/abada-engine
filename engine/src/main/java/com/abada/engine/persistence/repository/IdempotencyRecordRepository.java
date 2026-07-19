package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, String> {
    @Modifying
    @Query(value = "delete from idempotency_records where idempotency_key = :key and expires_at <= :now",
            nativeQuery = true)
    int deleteExpired(@Param("key") String key, @Param("now") Instant now);

    @Modifying
    @Query(value = "insert into idempotency_records "
            + "(idempotency_key, operation, request_hash, response_status, response_body, created_at, expires_at) "
            + "values (:key, :operation, :requestHash, 0, '{}', :now, :expiresAt) "
            + "on conflict (idempotency_key) do nothing", nativeQuery = true)
    int reserve(@Param("key") String key, @Param("operation") String operation,
            @Param("requestHash") String requestHash, @Param("now") Instant now,
            @Param("expiresAt") Instant expiresAt);
}
