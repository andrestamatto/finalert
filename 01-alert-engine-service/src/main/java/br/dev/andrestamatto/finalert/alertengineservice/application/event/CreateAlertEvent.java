package br.dev.andrestamatto.finalert.alertengineservice.application.event;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAlertEvent(
        @NotNull
        String eventId,
        @NotNull
        String alertId,
        @NotBlank
        String userEmail,
        @NotBlank
        String symbol,
        @NotNull
        BigDecimal price,
        @NotBlank
        String operator,
        @NotBlank
        String status,
        @NotNull
        Instant createdAt,
        Instant updatedAt,
        Instant cancelledAt,
        long version
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static CreateAlertEvent fromDomain(@NotNull Alert alert) {
        return new CreateAlertEvent(
                String.valueOf(generateUUID()),
                String.valueOf(alert.alertId()),
                alert.userEmail(),
                alert.symbol(),
                alert.price(),
                alert.triggerOperator().name(),
                alert.status().name(),
                alert.createdAt(),
                alert.updatedAt(),
                alert.cancelledAt(),
                alert.version()
        );
    }

}
