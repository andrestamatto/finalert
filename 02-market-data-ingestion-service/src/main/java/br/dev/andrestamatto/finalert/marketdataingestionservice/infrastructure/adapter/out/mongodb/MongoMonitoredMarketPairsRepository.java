package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPairDocument;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MongoMonitoredMarketPairsRepository implements MonitoredMarketPairsRepository {

    private final SpringDataMarketCatalogRepository repository;

    public MongoMonitoredMarketPairsRepository(SpringDataMarketCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<MarketPriceAlert> fetchMonitoredMarketPairs() {
        return Optional.of(
                repository.findAll().stream()
                        .map(pair -> new MarketPriceAlert(

                        ))
        ).orElse(Collections.emptySet());
    }
}
