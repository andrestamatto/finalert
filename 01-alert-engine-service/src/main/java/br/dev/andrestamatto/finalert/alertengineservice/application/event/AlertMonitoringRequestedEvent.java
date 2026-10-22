package br.dev.andrestamatto.finalert.alertengineservice.application.event;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertMonitoringRequestedEvent(
        @NotNull
        String eventId,
        @NotNull
        String alertId,
        @NotBlank
        String symbol,
        @NotNull
        BigDecimal targetValue,
        @NotBlank
        String triggerOperator,
        @NotNull
        Instant occurredAt
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static AlertMonitoringRequestedEvent fromDomain(@NotNull Alert alert) {
        return new AlertMonitoringRequestedEvent(
                String.valueOf(generateUUID()),
                alert.alertId().value().toString(),
                alert.symbol(),
                alert.price(),
                alert.triggerOperator().name(),
                alert.createdAt()
        );
    }

}
