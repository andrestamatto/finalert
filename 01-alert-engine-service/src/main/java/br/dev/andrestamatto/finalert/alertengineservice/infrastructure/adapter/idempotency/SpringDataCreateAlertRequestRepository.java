package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SpringDataCreateAlertRequestRepository
        extends JpaRepository<CreateAlertRequestEntity, String> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO tb_create_alert_requests (
                idempotency_key,
                request_hash,
                created_at
            ) VALUES (
                :idempotencyKey,
                :requestHash,
                :createdAt
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int tryAcquire(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE tb_create_alert_requests
               SET alert_id = :alertId,
                   completed_at = :completedAt
             WHERE idempotency_key = :idempotencyKey
               AND alert_id IS NULL
            """, nativeQuery = true)
    int complete(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("alertId") java.util.UUID alertId,
            @Param("completedAt") Instant completedAt
    );
}
