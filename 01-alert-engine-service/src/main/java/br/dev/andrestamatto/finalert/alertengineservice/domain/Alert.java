package br.dev.andrestamatto.finalert.alertengineservice.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Alert(
    AlertId alertId,
    String userEmail,
    String symbol,
    BigDecimal price,
    TriggerOperator triggerOperator,
    AlertStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant triggeredAt,
    Instant cancelledAt,
    Long version
) {
}
