package br.dev.andrestamatto.finalert.alertengineservice.application.usecase;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;

public interface MarketCatalogUpdatedUseCase {
    void save(MarketCatalog marketCatalog);
}
