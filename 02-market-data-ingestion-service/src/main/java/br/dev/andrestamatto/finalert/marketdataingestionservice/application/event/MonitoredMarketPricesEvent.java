package br.dev.andrestamatto.finalert.marketdataingestionservice.application.event;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.TriggerOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MonitoredMarketPricesEvent(
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
    public static MarketPriceAlert toDomain(MonitoredMarketPricesEvent event) {
        return new MarketPriceAlert(
            UUID.fromString(event.alertId()),
            event.symbol(),
            event.targetValue(),
            Enum.valueOf(TriggerOperator.class, event.triggerOperator()),
            event.occurredAt()
        );
    }
}
