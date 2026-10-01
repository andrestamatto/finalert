package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

import java.time.Instant;
import java.util.Set;

public class MongoMarketCatalogRepository implements MarketCatalogRepository {

    @Override
    public void replaceCatalog(Set<MarketPair> pairs, long catalogVersion, Instant updatedAt) {

    }
}
