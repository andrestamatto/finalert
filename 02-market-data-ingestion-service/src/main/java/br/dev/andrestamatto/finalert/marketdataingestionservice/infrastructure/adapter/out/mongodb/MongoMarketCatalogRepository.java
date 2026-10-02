package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPairDocument;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MongoMarketCatalogRepository implements MarketCatalogRepository {

    private final SpringDataMarketCatalogRepository repository;

    public MongoMarketCatalogRepository(SpringDataMarketCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void replaceCatalog(Set<MarketPair> pairs, long catalogVersion, Instant updatedAt) {
        var document = new MarketCatalogDocument(
                MarketCatalogDocument.CURRENT_CATALOG_ID,
                catalogVersion,
                updatedAt,
                pairs.stream()
                        .sorted(Comparator.comparing(MarketPair::name))
                        .map(pair -> new MarketPairDocument(
                                pair.name(),
                                pair.description()
                        ))
                        .collect(Collectors.toSet())
        );

        repository.save(document);
    }
}
