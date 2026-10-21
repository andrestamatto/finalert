package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;

import java.util.Optional;

public interface MarketCatalogRepository {
    void save(MarketCatalog catalog);

    Optional<MarketCatalog> findCurrent();

    boolean containsPair(String symbol);
}
