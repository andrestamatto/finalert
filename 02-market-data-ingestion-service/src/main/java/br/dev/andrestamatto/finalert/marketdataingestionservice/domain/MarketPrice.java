package br.dev.andrestamatto.finalert.marketdataingestionservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record MarketPrice(
        String baseCurrency,
        String quoteCurrency,
        String name,
        BigDecimal high,
        BigDecimal low,
        BigDecimal variation,
        BigDecimal percentChange,
        BigDecimal bid,
        BigDecimal ask,
        Instant quotedAt,
        LocalDateTime createdAt
) {
}
