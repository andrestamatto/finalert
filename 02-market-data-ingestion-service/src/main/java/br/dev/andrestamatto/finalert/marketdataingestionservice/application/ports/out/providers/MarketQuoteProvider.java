package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPrice;

import java.util.Set;

public interface MarketQuoteProvider {
    Set<MarketPrice> fetchLastUpdatedMarketQuotes(String quotes);
}
