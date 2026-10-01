package br.dev.andrestamatto.finalert.alertengineservice.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPrice(
        String symbol,
        BigDecimal currentPrice,
        Instant quotedAt
) {}
