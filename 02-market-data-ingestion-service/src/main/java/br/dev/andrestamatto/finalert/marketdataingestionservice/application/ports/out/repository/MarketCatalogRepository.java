package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;

import java.util.Optional;

public interface MarketCatalogRepository {

    void save(MarketCatalog catalog);

    Optional<MarketCatalog> findCurrent();

}
