package br.dev.andrestamatto.finalert.marketdataingestionservice.domain;

import java.time.Instant;
import java.util.Set;

public record MarketCatalog(
        Set<MarketPair> pairs,
        long version,
        Instant updatedAt
) {
}
