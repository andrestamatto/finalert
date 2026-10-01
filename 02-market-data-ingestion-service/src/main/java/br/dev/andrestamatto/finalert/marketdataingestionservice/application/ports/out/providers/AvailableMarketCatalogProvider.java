package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

import java.util.Set;

public interface AvailableMarketCatalogProvider {
    Set<MarketPair> fetchAvailableMarketPairsCatalog();
}
