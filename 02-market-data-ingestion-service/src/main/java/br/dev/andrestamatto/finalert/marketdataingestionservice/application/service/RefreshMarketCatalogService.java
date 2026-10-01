package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.CatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.AvailableMarketCatalogProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RefreshMarketCatalogUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshMarketCatalogService implements RefreshMarketCatalogUseCase {

    private final AvailableMarketCatalogProvider catalogProvider;
    private final MarketCatalogRepository marketCatalogRepository;
    private final MarketCatalogUpdatedEventPublisher eventPublisher;

    public RefreshMarketCatalogService(AvailableMarketCatalogProvider catalogProvider, MarketCatalogRepository marketCatalogRepository, MarketCatalogUpdatedEventPublisher eventPublisher) {
        this.catalogProvider = catalogProvider;
        this.marketCatalogRepository = marketCatalogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void execute() throws Exception {
        var marketPairs = catalogProvider.fetchAvailableMarketPairsCatalog();
        var now = Instant.now();
        var version = 0; // TODO: resolve version later.

        // persiste no MongoDB
        marketCatalogRepository.replaceCatalog(marketPairs, version, now);

        // publica InstrumentCatalogUpdatedEvent
        eventPublisher.publish(
                CatalogUpdatedEvent.of(marketPairs, version)
        );
    }
}
