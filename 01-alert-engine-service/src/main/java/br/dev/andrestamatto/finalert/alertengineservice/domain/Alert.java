package br.dev.andrestamatto.finalert.alertengineservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

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

    public boolean isSatisfiedBy(MarketPrice marketPrice) {
        Objects.requireNonNull(marketPrice, "marketPrice must not be null");
        return symbol.equals(marketPrice.symbol())
                && triggerOperator.isSatisfiedBy(
                marketPrice.currentPrice(),
                price
        );
    }

    public Alert markAsTriggered(Instant triggeredAt) {
        Objects.requireNonNull(triggeredAt, "triggeredAt must not be null");
        if (status != AlertStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending alerts can be triggered"
            );
        }

        return new Alert(
                alertId,
                userEmail,
                symbol,
                price,
                triggerOperator,
                AlertStatus.TRIGGERED,
                createdAt,
                triggeredAt,
                triggeredAt,
                cancelledAt,
                version
        );
    }

}
