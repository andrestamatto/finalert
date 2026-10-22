package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.AvailableMarketCatalogProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RefreshMarketCatalogUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
public class RefreshMarketCatalogService implements RefreshMarketCatalogUseCase {

    private static final int MAX_VERSION_CONFLICT_ATTEMPTS = 3;

    private final AvailableMarketCatalogProvider catalogProvider;
    private final MarketCatalogRepository marketCatalogRepository;
    private final Clock clock;

    public RefreshMarketCatalogService(
            AvailableMarketCatalogProvider catalogProvider,
            MarketCatalogRepository marketCatalogRepository,
            Clock clock
    ) {
        this.catalogProvider = catalogProvider;
        this.marketCatalogRepository = marketCatalogRepository;
        this.clock = clock;
    }

    @Override
    public void execute() throws Exception {
        var marketPairs = Set.copyOf(catalogProvider.fetchAvailableMarketPairsCatalog());
        refreshCatalog(marketPairs, 1);
    }

    private void refreshCatalog(Set<MarketPair> marketPairs, int attempt) {
        var currentCatalog = marketCatalogRepository.findCurrent();

        if (currentCatalog.map(catalog -> catalog.pairs().equals(marketPairs)).orElse(false)) {
            log.info("Market catalog has not changed; update ignored.");
            return;
        }

        var now = Instant.now(clock);
        var version = currentCatalog
                .map(MarketCatalog::version)
                .map(Math::incrementExact)
                .orElse(1L);

        var catalog = new MarketCatalog(marketPairs, version, now);
        var event = MarketCatalogUpdatedEvent.fromDomain(catalog);

        try {
            marketCatalogRepository.saveWithOutbox(catalog, event);
        } catch (DuplicateKeyException exception) {
            if (attempt >= MAX_VERSION_CONFLICT_ATTEMPTS) {
                throw exception;
            }

            log.warn(
                    "Catalog version {} was concurrently created; retrying ({}/{}).",
                    version,
                    attempt,
                    MAX_VERSION_CONFLICT_ATTEMPTS
            );
            refreshCatalog(marketPairs, attempt + 1);
        }
    }
}
