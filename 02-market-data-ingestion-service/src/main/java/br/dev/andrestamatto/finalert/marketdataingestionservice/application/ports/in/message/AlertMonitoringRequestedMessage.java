package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.message;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.TriggerOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertMonitoringRequestedMessage(
        @NotBlank
        String eventId,

        @NotBlank
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
    public static MarketPriceAlert toDomain(AlertMonitoringRequestedMessage event) {
        return new MarketPriceAlert(
            UUID.fromString(event.alertId()),
            event.symbol(),
            event.targetValue(),
            Enum.valueOf(TriggerOperator.class, event.triggerOperator()),
            event.occurredAt()
        );
    }
}
