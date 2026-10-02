package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document(collection = "market_catalog")
public record MarketCatalogDocument(
        @Id
        String id,

        long version,

        Instant updatedAt,

        Set<MarketPairDocument> pairs
) {
    public static final String CURRENT_CATALOG_ID = "current";
}