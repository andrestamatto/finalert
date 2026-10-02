package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;

import java.time.Instant;
import java.util.Set;

public interface MarketCatalogRepository {

    void replaceCatalog(
            Set<MarketPair> pairs,
            long catalogVersion,
            Instant updatedAt
    );

}
