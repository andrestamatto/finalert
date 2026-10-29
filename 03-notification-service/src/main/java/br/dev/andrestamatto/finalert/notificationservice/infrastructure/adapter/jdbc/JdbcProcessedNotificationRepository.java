package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.jdbc;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.ProcessedNotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcProcessedNotificationRepository implements ProcessedNotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryClaim(UUID eventId, UUID alertId, Instant processedAt) {
        return jdbcTemplate.update("""
                INSERT INTO tb_processed_notifications (event_id, alert_id, processed_at)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """, eventId, alertId, Timestamp.from(processedAt)) == 1;
    }
}
