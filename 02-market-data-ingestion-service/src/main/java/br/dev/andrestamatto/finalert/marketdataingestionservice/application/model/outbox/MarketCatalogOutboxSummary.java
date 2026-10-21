package br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox;

public record MarketCatalogOutboxSummary(
        long pending,
        long processing,
        long published,
        long failed
) {
}
