package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.config.MarketCatalogOutboxProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.MaintainMarketCatalogOutboxUseCase;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class MaintainMarketCatalogOutboxService implements MaintainMarketCatalogOutboxUseCase {

    private final MarketCatalogOutboxRepository repository;
    private final MarketCatalogOutboxProperties properties;
    private final Clock clock;

    public MaintainMarketCatalogOutboxService(
            MarketCatalogOutboxRepository repository,
            MarketCatalogOutboxProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean resubmitFailed(UUID eventId) {
        return repository.resubmitFailed(eventId, Instant.now(clock));
    }

    @Override
    public long purgePublished() {
        var publishedBefore = Instant.now(clock).minus(properties.publishedRetention());
        return repository.deletePublishedBefore(publishedBefore);
    }

    @Override
    public MarketCatalogOutboxSummary summarize() {
        return repository.summarize();
    }
}
