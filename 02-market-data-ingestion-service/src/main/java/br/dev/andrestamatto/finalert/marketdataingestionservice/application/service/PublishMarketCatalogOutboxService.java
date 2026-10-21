package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.ClaimedMarketCatalogOutboxEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.config.MarketCatalogOutboxProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.PublishMarketCatalogOutboxUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class PublishMarketCatalogOutboxService implements PublishMarketCatalogOutboxUseCase {

    private final Clock clock;
    private final MarketCatalogOutboxProperties properties;
    private final MarketCatalogOutboxRepository marketCatalogOutboxRepository;
    private final MarketCatalogUpdatedEventPublisher marketCatalogUpdatedEventPublisher;

    public PublishMarketCatalogOutboxService(
            Clock clock,
            MarketCatalogOutboxProperties properties,
            MarketCatalogOutboxRepository marketCatalogOutboxRepository,
            MarketCatalogUpdatedEventPublisher marketCatalogUpdatedEventPublisher
    ) {
        this.clock = clock;
        this.properties = properties;
        this.marketCatalogOutboxRepository = marketCatalogOutboxRepository;
        this.marketCatalogUpdatedEventPublisher = marketCatalogUpdatedEventPublisher;
    }

    @Override
    public void execute() {
        var now = Instant.now(clock);
        marketCatalogOutboxRepository.recoverStaleProcessing(
                now.minus(properties.processingTimeout()),
                now,
                properties.maxPublishAttempts()
        );

        for (int processed = 0; processed < properties.maxEventsPerExecution(); processed++) {
            var claimed = marketCatalogOutboxRepository.claimNextPending();

            if (claimed.isEmpty()) {
                log.debug("No market catalog outbox event available for publication.");
                return;
            }

            publish(claimed.get());
        }
    }

    private void publish(ClaimedMarketCatalogOutboxEvent outbox) {
        try {
            marketCatalogUpdatedEventPublisher.publish(outbox.payload());
        } catch (EventPublicationException exception) {
            handlePublicationFailure(outbox, exception);
            return;
        }

        marketCatalogOutboxRepository.markAsPublished(
                outbox.eventId(),
                Instant.now(clock)
        );

        log.info(
                "Market catalog outbox event published: eventId={}, attempt={}",
                outbox.eventId(),
                outbox.attempt()
        );
    }

    private void handlePublicationFailure(
            ClaimedMarketCatalogOutboxEvent outbox,
            EventPublicationException exception
    ) {
        var now = Instant.now(clock);

        if (outbox.attempt() >= properties.maxPublishAttempts()) {
            marketCatalogOutboxRepository.markAsFailed(
                    outbox.eventId(),
                    now,
                    exception.getMessage()
            );

            log.error(
                    "Market catalog event permanently failed: eventId={}, attempts={}",
                    outbox.eventId(),
                    outbox.attempt(),
                    exception
            );
            return;
        }

        marketCatalogOutboxRepository.reschedule(
                outbox.eventId(),
                now.plus(calculateBackoff(outbox.attempt())),
                exception.getMessage()
        );

        log.warn(
                "Market catalog event rescheduled: eventId={}, attempt={}",
                outbox.eventId(),
                outbox.attempt(),
                exception
        );
    }

    private Duration calculateBackoff(int attempt) {
        var exponent = Math.max(0, attempt - 1);
        var multiplier = Math.pow(properties.retryMultiplier(), exponent);
        return properties.initialRetryDelay().multipliedBy(Math.max(1L, Math.round(multiplier)));
    }
}
