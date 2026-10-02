package br.dev.andrestamatto.finalert.marketdataingestionservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketPriceAlert(
        UUID alertId,
        String symbol,
        BigDecimal targetValue,
        TriggerOperator triggerOperator,
        Instant registeredAt
) {
}
