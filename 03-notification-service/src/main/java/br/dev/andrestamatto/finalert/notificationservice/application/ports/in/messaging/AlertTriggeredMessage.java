package br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertTriggeredMessage(
        @NotBlank String eventId,
        @NotBlank String alertId,
        @Email @NotBlank String userEmail,
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal targetPrice,
        @NotBlank String triggerOperator,
        @NotNull @Positive BigDecimal currentPrice,
        @NotNull Instant quotedAt,
        @NotNull Instant occurredAt
) {
    public UUID eventUuid() {
        return UUID.fromString(eventId);
    }

    public UUID alertUuid() {
        return UUID.fromString(alertId);
    }
}
