package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

public record MarketPairDocument(
        String pair,
        String description
) {
    public static MarketPairDocument fromDomain(MarketPair pair) {
        return new MarketPairDocument(pair.name(), pair.description());
    }

    public MarketPair toDomain() {
        return new MarketPair(pair, description);
    }
}
