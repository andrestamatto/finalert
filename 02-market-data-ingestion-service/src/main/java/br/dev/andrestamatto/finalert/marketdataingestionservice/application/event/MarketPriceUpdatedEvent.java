package br.dev.andrestamatto.finalert.marketdataingestionservice.application.event;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketPriceUpdatedEvent(
        UUID eventId,
        String symbol,
        BigDecimal currentPrice,
        Instant occurredAt
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static MarketPriceUpdatedEvent fromDomain(String symbol, MarketPrice price) {
        return new MarketPriceUpdatedEvent(
                generateUUID(),
                symbol,
                price.bid(),
                price.quotedAt()
        );
    }

}
