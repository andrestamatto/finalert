package br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox;

import java.time.Instant;
import java.util.Optional;

public record MarketCatalogOutboxSummary(
        long pending,
        long processing,
        long published,
        long failed,
        Optional<Instant> oldestPendingCreatedAt,
        Optional<Instant> oldestProcessingCreatedAt
) {
}
