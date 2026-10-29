package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;

import java.util.Set;
import java.util.UUID;

public interface MonitoredMarketPairsRepository {
    void register(MarketPriceAlert alert);

    void unregister(UUID alertId);

    Set<MarketPriceAlert> fetchMonitoredMarketPairs();
}
