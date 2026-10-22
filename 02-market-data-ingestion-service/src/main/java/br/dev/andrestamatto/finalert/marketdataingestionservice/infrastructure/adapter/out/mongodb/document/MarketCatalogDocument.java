package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Document(collection = "market_catalog")
public record MarketCatalogDocument(
        @Id
        String id,

        @Indexed(name = "uk_market_catalog_version", unique = true)
        long catalogVersion,

        Instant updatedAt,

        Set<MarketPairDocument> pairs
) {
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public MarketCatalog toDomain() {
        Set<MarketPair> marketPairs = this.pairs().stream()
                .map(MarketPairDocument::toDomain)
                .collect(Collectors.toUnmodifiableSet());

        return new MarketCatalog(marketPairs, catalogVersion, updatedAt);
    }
}
