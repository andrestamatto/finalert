package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.ClaimedMarketCatalogOutboxEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MarketCatalogOutboxRepository {
    Optional<ClaimedMarketCatalogOutboxEvent> claimNextPending();

    void markAsPublished(UUID eventId, Instant publishedAt);

    void reschedule(
            UUID eventId,
            Instant nextAttemptAt,
            String error
    );

    void markAsFailed(
            UUID eventId,
            Instant failedAt,
            String error
    );

    void recoverStaleProcessing(
            Instant lockedBefore,
            Instant nextAttemptAt,
            int maxAttempts
    );

    boolean resubmitFailed(UUID eventId, Instant resubmittedAt);

    long deletePublishedBefore(Instant publishedBefore);

    MarketCatalogOutboxSummary summarize();
}
