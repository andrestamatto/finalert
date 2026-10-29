package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketPrice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketPriceUpdatedMessage(
        @NotNull UUID eventId,
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal currentPrice,
        @NotNull Instant occurredAt
) {
    public MarketPrice toDomain() {
        return new MarketPrice(
                symbol,
                currentPrice,
                occurredAt
        );
    }
}
