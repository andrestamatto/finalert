package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
            SELECT *
              FROM tb_outbox_events
             WHERE status = 'PENDING'
               AND next_attempt_at <= :now
             ORDER BY next_attempt_at, created_at
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEventEntity> lockNextDue(@Param("now") Instant now);
}
