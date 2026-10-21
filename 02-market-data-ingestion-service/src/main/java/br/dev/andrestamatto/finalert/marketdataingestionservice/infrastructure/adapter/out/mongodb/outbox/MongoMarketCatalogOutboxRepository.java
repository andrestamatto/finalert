package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.ClaimedMarketCatalogOutboxEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document.MarketCatalogOutboxDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document.OutboxStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class MongoMarketCatalogOutboxRepository implements MarketCatalogOutboxRepository {

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public MongoMarketCatalogOutboxRepository(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    @Override
    public Optional<ClaimedMarketCatalogOutboxEvent> claimNextPending() {
        var now = Instant.now(clock);
        var query = Query.query(
                Criteria.where("status").is(OutboxStatus.PENDING)
                        .and("nextAttemptAt").lte(now)
        ).with(Sort.by(Sort.Direction.ASC, "createdAt"));

        var update = new Update()
                .set("status", OutboxStatus.PROCESSING)
                .set("lockedAt", now)
                .set("lastAttemptAt", now)
                .inc("attempts", 1);

        var document = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                MarketCatalogOutboxDocument.class
        );

        return Optional.ofNullable(document)
                .map(MarketCatalogOutboxDocument::toClaimedEvent);
    }

    @Override
    public void markAsPublished(UUID eventId, Instant publishedAt) {
        var query = processingEventQuery(eventId);
        var update = new Update()
                .set("status", OutboxStatus.PUBLISHED)
                .set("publishedAt", publishedAt)
                .unset("lockedAt")
                .unset("nextAttemptAt")
                .unset("lastError");

        ensureUpdated(eventId, mongoTemplate.updateFirst(
                query,
                update,
                MarketCatalogOutboxDocument.class
        ).getModifiedCount(), OutboxStatus.PUBLISHED);
    }

    @Override
    public void reschedule(UUID eventId, Instant nextAttemptAt, String error) {
        var query = processingEventQuery(eventId);
        var update = new Update()
                .set("status", OutboxStatus.PENDING)
                .set("nextAttemptAt", nextAttemptAt)
                .set("lastError", error)
                .unset("lockedAt");

        ensureUpdated(eventId, mongoTemplate.updateFirst(
                query,
                update,
                MarketCatalogOutboxDocument.class
        ).getModifiedCount(), OutboxStatus.PENDING);
    }

    @Override
    public void markAsFailed(UUID eventId, Instant failedAt, String error) {
        var query = processingEventQuery(eventId);
        var update = new Update()
                .set("status", OutboxStatus.FAILED)
                .set("failedAt", failedAt)
                .set("lastError", error)
                .unset("lockedAt")
                .unset("nextAttemptAt");

        ensureUpdated(eventId, mongoTemplate.updateFirst(
                query,
                update,
                MarketCatalogOutboxDocument.class
        ).getModifiedCount(), OutboxStatus.FAILED);
    }

    @Override
    public void recoverStaleProcessing(
            Instant lockedBefore,
            Instant nextAttemptAt,
            int maxAttempts
    ) {
        var staleProcessing = Criteria.where("status").is(OutboxStatus.PROCESSING)
                .and("lockedAt").lte(lockedBefore);

        var retryableQuery = Query.query(new Criteria().andOperator(
                staleProcessing,
                Criteria.where("attempts").lt(maxAttempts)
        ));
        var retryableUpdate = new Update()
                .set("status", OutboxStatus.PENDING)
                .set("nextAttemptAt", nextAttemptAt)
                .set("lastError", "Processing lock expired before publication was completed")
                .unset("lockedAt");
        mongoTemplate.updateMulti(
                retryableQuery,
                retryableUpdate,
                MarketCatalogOutboxDocument.class
        );

        var exhaustedQuery = Query.query(new Criteria().andOperator(
                Criteria.where("status").is(OutboxStatus.PROCESSING)
                        .and("lockedAt").lte(lockedBefore),
                Criteria.where("attempts").gte(maxAttempts)
        ));
        var exhaustedUpdate = new Update()
                .set("status", OutboxStatus.FAILED)
                .set("failedAt", nextAttemptAt)
                .set("lastError", "Processing lock expired after the maximum number of attempts")
                .unset("lockedAt")
                .unset("nextAttemptAt");
        mongoTemplate.updateMulti(
                exhaustedQuery,
                exhaustedUpdate,
                MarketCatalogOutboxDocument.class
        );
    }

    @Override
    public boolean resubmitFailed(UUID eventId, Instant resubmittedAt) {
        var query = Query.query(
                Criteria.where("_id").is(eventId)
                        .and("status").is(OutboxStatus.FAILED)
        );
        var update = new Update()
                .set("status", OutboxStatus.PENDING)
                .set("nextAttemptAt", resubmittedAt)
                .set("lastRedriveAt", resubmittedAt)
                .set("attempts", 0)
                .inc("redriveCount", 1)
                .unset("failedAt")
                .unset("lastError")
                .unset("lockedAt");

        return mongoTemplate.updateFirst(
                query,
                update,
                MarketCatalogOutboxDocument.class
        ).getModifiedCount() == 1;
    }

    @Override
    public long deletePublishedBefore(Instant publishedBefore) {
        var query = Query.query(
                Criteria.where("status").is(OutboxStatus.PUBLISHED)
                        .and("publishedAt").lt(publishedBefore)
        );
        return mongoTemplate.remove(
                query,
                MarketCatalogOutboxDocument.class
        ).getDeletedCount();
    }

    @Override
    public MarketCatalogOutboxSummary summarize() {
        return new MarketCatalogOutboxSummary(
                count(OutboxStatus.PENDING),
                count(OutboxStatus.PROCESSING),
                count(OutboxStatus.PUBLISHED),
                count(OutboxStatus.FAILED)
        );
    }

    private Query processingEventQuery(UUID eventId) {
        return Query.query(
                Criteria.where("_id").is(eventId)
                        .and("status").is(OutboxStatus.PROCESSING)
        );
    }

    private void ensureUpdated(
            UUID eventId,
            long modifiedCount,
            OutboxStatus targetStatus
    ) {
        if (modifiedCount == 0) {
            throw new IllegalStateException(
                    "Could not transition outbox event %s to %s"
                            .formatted(eventId, targetStatus)
            );
        }
    }

    private long count(OutboxStatus status) {
        return mongoTemplate.count(
                Query.query(Criteria.where("status").is(status)),
                MarketCatalogOutboxDocument.class
        );
    }
}
