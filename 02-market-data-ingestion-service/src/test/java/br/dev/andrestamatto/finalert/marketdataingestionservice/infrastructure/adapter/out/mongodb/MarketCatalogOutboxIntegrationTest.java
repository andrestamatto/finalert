package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.MongoMarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.SpringDataMarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document.MarketCatalogOutboxDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.repository.MongoMarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.repository.SpringDataMarketCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "market-data.outbox.publish-fixed-delay=PT1H",
        "market-data.outbox.cleanup-fixed-delay=PT1H"
})
@Testcontainers(disabledWithoutDocker = true)
class MarketCatalogOutboxIntegrationTest {

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0.17")
            .withReplicaSet();

    @DynamicPropertySource
    static void mongodbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
    }

    @Autowired
    private MongoMarketCatalogRepository catalogRepository;

    @Autowired
    private MongoMarketCatalogOutboxRepository outboxRepository;

    @Autowired
    private SpringDataMarketCatalogRepository springDataCatalogRepository;

    @Autowired
    private SpringDataMarketCatalogOutboxRepository springDataOutboxRepository;

    @BeforeEach
    void cleanCollections() {
        springDataCatalogRepository.deleteAll();
        springDataOutboxRepository.deleteAll();
    }

    @Test
    void persistsTheCatalogAndItsOutboxEventInTheSameTransaction() {
        var event = event();

        catalogRepository.saveWithOutbox(catalog(), event);

        assertThat(springDataCatalogRepository.count()).isOne();
        assertThat(springDataOutboxRepository.findById(event.eventId())).isPresent();
    }

    @Test
    void rollsBackTheCatalogWhenTheOutboxInsertFails() {
        var event = event();
        springDataOutboxRepository.save(MarketCatalogOutboxDocument.createFrom(
                event,
                Instant.parse("2026-10-21T11:59:00Z")
        ));

        assertThatThrownBy(() -> catalogRepository.saveWithOutbox(catalog(), event))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(springDataCatalogRepository.count()).isZero();
        assertThat(springDataOutboxRepository.count()).isOne();
    }

    @Test
    void claimsAPendingEventOnlyOnce() {
        var event = event();
        springDataOutboxRepository.save(MarketCatalogOutboxDocument.createFrom(
                event,
                Instant.parse("2026-10-21T12:00:00Z")
        ));

        assertThat(outboxRepository.claimNextPending()).isPresent();
        assertThat(outboxRepository.claimNextPending()).isEmpty();
    }

    @Test
    void resubmitsAFailedEventAndResetsItsAttempts() {
        var event = event();
        var failedAt = Instant.parse("2026-10-21T12:01:00Z");
        springDataOutboxRepository.save(MarketCatalogOutboxDocument.createFrom(
                event,
                Instant.parse("2026-10-21T12:00:00Z")
        ));
        var claimed = outboxRepository.claimNextPending().orElseThrow();
        outboxRepository.markAsFailed(claimed.eventId(), failedAt, "RabbitMQ unavailable");

        assertThat(outboxRepository.resubmitFailed(event.eventId(), failedAt.plusSeconds(60)))
                .isTrue();

        var redriven = outboxRepository.claimNextPending().orElseThrow();
        var document = springDataOutboxRepository.findById(event.eventId()).orElseThrow();
        assertThat(redriven.attempt()).isOne();
        assertThat(document.redriveCount()).isOne();
        assertThat(document.failedAt()).isNull();
        assertThat(document.lastError()).isNull();
    }

    @Test
    void purgesOnlyPublishedEventsOlderThanTheRetentionCutoff() {
        var oldEvent = event();
        springDataOutboxRepository.save(MarketCatalogOutboxDocument.createFrom(
                oldEvent,
                Instant.parse("2026-10-01T12:00:00Z")
        ));
        var claimed = outboxRepository.claimNextPending().orElseThrow();
        outboxRepository.markAsPublished(
                claimed.eventId(),
                Instant.parse("2026-10-02T12:00:00Z")
        );
        var pendingEvent = event();
        springDataOutboxRepository.save(MarketCatalogOutboxDocument.createFrom(
                pendingEvent,
                Instant.parse("2026-10-20T12:00:00Z")
        ));

        assertThat(outboxRepository.deletePublishedBefore(
                Instant.parse("2026-10-10T12:00:00Z")
        )).isOne();
        assertThat(springDataOutboxRepository.findById(oldEvent.eventId())).isEmpty();
        assertThat(springDataOutboxRepository.findById(pendingEvent.eventId())).isPresent();
    }

    private MarketCatalog catalog() {
        return new MarketCatalog(
                Set.of(new MarketPair("USD-BRL", "Dolar/Real")),
                1L,
                Instant.parse("2026-10-21T12:00:00Z")
        );
    }

    private MarketCatalogUpdatedEvent event() {
        return new MarketCatalogUpdatedEvent(
                UUID.randomUUID(),
                Map.of("USD-BRL", "Dolar/Real"),
                Instant.parse("2026-10-21T12:00:00Z"),
                1L
        );
    }
}
