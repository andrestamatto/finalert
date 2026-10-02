package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPriceAlertDocument;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MongoMonitoredMarketPairsRepository implements MonitoredMarketPairsRepository {

    private final SpringDataMonitoredMarketPairsRepository repository;

    public MongoMonitoredMarketPairsRepository(SpringDataMonitoredMarketPairsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void register(MarketPriceAlert alert) {
        repository.save(MarketPriceAlertDocument.fromDomain(alert));
    }

    @Override
    public Set<MarketPriceAlert> fetchMonitoredMarketPairs() {
        return repository.findAll().stream()
                .map(MarketPriceAlertDocument::toDomain)
                .collect(Collectors.toSet());
    }
}
