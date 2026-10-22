package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.ClaimedMarketCatalogOutboxEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document("market_catalog_outbox")
@CompoundIndexes({
        @CompoundIndex(
                name = "outbox_pending_idx",
                def = "{'status': 1, 'nextAttemptAt': 1, 'createdAt': 1}"
        ),
        @CompoundIndex(
                name = "outbox_processing_lock_idx",
                def = "{'status': 1, 'lockedAt': 1}"
        )
})
public record MarketCatalogOutboxDocument(
        @Id UUID eventId,
        MarketCatalogUpdatedEvent payload,
        OutboxStatus status,
        Instant createdAt,
        Instant lastAttemptAt,
        Instant nextAttemptAt,
        Instant lockedAt,
        Instant publishedAt,
        Instant failedAt,
        Instant lastRedriveAt,
        int attempts,
        int redriveCount,
        String lastError
) {
    public static MarketCatalogOutboxDocument createFrom(
            MarketCatalogUpdatedEvent payload,
            Instant createdAt
    ) {
        return new MarketCatalogOutboxDocument(
                payload.eventId(),
                payload,
                OutboxStatus.PENDING,
                createdAt,
                null,
                createdAt,
                null,
                null,
                null,
                null,
                0,
                0,
                null
        );
    }

    public ClaimedMarketCatalogOutboxEvent toClaimedEvent() {
        return new ClaimedMarketCatalogOutboxEvent(
                eventId,
                payload,
                attempts
        );
    }
}
