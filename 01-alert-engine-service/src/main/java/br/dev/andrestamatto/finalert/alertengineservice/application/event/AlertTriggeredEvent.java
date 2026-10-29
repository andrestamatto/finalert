package br.dev.andrestamatto.finalert.alertengineservice.application.event;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketPrice;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertTriggeredEvent(
        @NotBlank
        String eventId,
        @NotBlank
        String alertId,
        @Email @NotBlank
        String userEmail,
        @NotBlank
        String symbol,
        @NotNull @Positive
        BigDecimal targetPrice,
        @NotBlank
        String triggerOperator,
        @NotNull @Positive
        BigDecimal currentPrice,
        @NotNull
        Instant quotedAt,
        @NotNull
        Instant occurredAt
) {

    public static AlertTriggeredEvent fromDomain(
            Alert alert,
            MarketPrice marketPrice,
            Instant occurredAt
    ) {
        return new AlertTriggeredEvent(
                UUID.randomUUID().toString(),
                alert.alertId().value().toString(),
                alert.userEmail(),
                alert.symbol(),
                alert.price(),
                alert.triggerOperator().name(),
                marketPrice.currentPrice(),
                marketPrice.quotedAt(),
                occurredAt
        );
    }
}
