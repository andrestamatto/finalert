package br.dev.andrestamatto.finalert.notificationservice.application.ports.out;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedNotificationRepository {
    boolean tryClaim(UUID eventId, UUID alertId, Instant processedAt);
}
