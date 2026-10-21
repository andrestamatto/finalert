package br.dev.andrestamatto.finalert.marketdataingestionservice.application.event;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record MarketCatalogUpdatedEvent(
        UUID eventId,
        Map<String, String> catalog,
        Instant updatedAt,
        long version
) {

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static MarketCatalogUpdatedEvent fromDomain(MarketCatalog catalog) {
        return new MarketCatalogUpdatedEvent(
                generateUUID(),
                catalog.pairs().stream().collect(Collectors.toMap(
                        MarketPair::name,
                        MarketPair::description
                )),
                catalog.updatedAt(),
                catalog.version()
        );
    }
}
