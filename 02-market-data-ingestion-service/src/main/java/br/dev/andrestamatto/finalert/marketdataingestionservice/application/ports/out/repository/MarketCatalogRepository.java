package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;

import java.util.Optional;

public interface MarketCatalogRepository {

    void saveWithOutbox(
            MarketCatalog catalog,
            MarketCatalogUpdatedEvent marketCatalogUpdatedEvent
    );

    Optional<MarketCatalog> findCurrent();

}
