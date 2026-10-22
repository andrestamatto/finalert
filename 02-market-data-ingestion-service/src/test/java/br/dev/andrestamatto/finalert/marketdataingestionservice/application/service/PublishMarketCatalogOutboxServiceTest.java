package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.ClaimedMarketCatalogOutboxEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.config.MarketCatalogOutboxProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishMarketCatalogOutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Mock
    private MarketCatalogOutboxRepository repository;

    @Mock
    private MarketCatalogUpdatedEventPublisher publisher;

    private PublishMarketCatalogOutboxService service;

    @BeforeEach
    void setUp() {
        service = new PublishMarketCatalogOutboxService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                new MarketCatalogOutboxProperties(
                        3,
                        10,
                        Duration.ofSeconds(5),
                        2.0,
                        Duration.ofMinutes(2),
                        Duration.ofDays(7)
                ),
                repository,
                publisher
        );
    }

    @Test
    void publishesAndMarksTheClaimedEventAsPublished() {
        var outbox = claimedEvent(1);
        when(repository.claimNextPending())
                .thenReturn(Optional.of(outbox))
                .thenReturn(Optional.empty());

        service.execute();

        verify(publisher).publish(outbox.payload());
        verify(repository).markAsPublished(outbox.eventId(), NOW);
        verify(repository).recoverStaleProcessing(
                NOW.minusSeconds(120),
                NOW,
                3
        );
    }

    @Test
    void reschedulesARecoverablePublicationFailureWithBackoff() {
        var outbox = claimedEvent(1);
        var failure = new EventPublicationException("RabbitMQ unavailable", new RuntimeException());
        when(repository.claimNextPending())
                .thenReturn(Optional.of(outbox))
                .thenReturn(Optional.empty());
        doThrow(failure).when(publisher).publish(outbox.payload());

        service.execute();

        verify(repository).reschedule(
                outbox.eventId(),
                NOW.plusSeconds(5),
                failure.getMessage()
        );
        verify(repository, never()).markAsFailed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void marksTheEventAsFailedAfterTheMaximumNumberOfAttempts() {
        var outbox = claimedEvent(3);
        var failure = new EventPublicationException("RabbitMQ unavailable", new RuntimeException());
        when(repository.claimNextPending())
                .thenReturn(Optional.of(outbox))
                .thenReturn(Optional.empty());
        doThrow(failure).when(publisher).publish(outbox.payload());

        service.execute();

        verify(repository).markAsFailed(
                outbox.eventId(),
                NOW,
                failure.getMessage()
        );
        verify(repository, never()).reschedule(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private ClaimedMarketCatalogOutboxEvent claimedEvent(int attempt) {
        var eventId = UUID.randomUUID();
        var payload = new MarketCatalogUpdatedEvent(
                eventId,
                Map.of("USD-BRL", "Dolar/Real"),
                NOW.minusSeconds(60),
                1L
        );
        return new ClaimedMarketCatalogOutboxEvent(eventId, payload, attempt);
    }
}
