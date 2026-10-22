package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;

public interface MarketCatalogUpdatedUseCase {
    void save(MarketCatalog marketCatalog);
}
