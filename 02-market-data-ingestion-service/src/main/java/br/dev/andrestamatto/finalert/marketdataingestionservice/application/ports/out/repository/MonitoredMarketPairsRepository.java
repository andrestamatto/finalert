package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;

import java.util.Set;

public interface MonitoredMarketPairsRepository {
    void register(MarketPriceAlert alert);

    Set<MarketPriceAlert> fetchMonitoredMarketPairs();
}
