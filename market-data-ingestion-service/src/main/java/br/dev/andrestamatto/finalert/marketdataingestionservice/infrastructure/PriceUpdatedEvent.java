package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.Price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceUpdatedEvent(
        UUID eventId,
        String symbol,
        BigDecimal currentPrice,
        Instant occurredAt
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static PriceUpdatedEvent  fromDomain(String symbol, Price price) {
        return new PriceUpdatedEvent(
                generateUUID(),
                symbol,
                price.bid(),
                price.quotedAt()
        );
    }

}
