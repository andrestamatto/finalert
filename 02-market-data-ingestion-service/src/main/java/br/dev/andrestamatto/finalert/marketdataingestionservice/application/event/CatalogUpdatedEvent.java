package br.dev.andrestamatto.finalert.marketdataingestionservice.application.event;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record CatalogUpdatedEvent(
        UUID eventId,
        Map<String, String> catalog,
        Instant updatedAt,
        long version
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static CatalogUpdatedEvent of(Set<MarketPair> marketPairs, long version) {
        var now  = Instant.now();
        return new CatalogUpdatedEvent(
                generateUUID(),
                marketPairs.stream().collect(Collectors.toMap(
                    MarketPair::pair,
                        MarketPair::description
                )),
                now,
                version
        );
    }
}
