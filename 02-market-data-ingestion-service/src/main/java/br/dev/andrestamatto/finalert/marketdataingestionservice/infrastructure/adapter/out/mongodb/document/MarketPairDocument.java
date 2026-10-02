package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document;

public record MarketPairDocument(
        String pair,
        String description
) {
}
