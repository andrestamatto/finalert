package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPairDocument;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoMarketCatalogRepository implements MarketCatalogRepository {

    private final SpringDataMarketCatalogRepository repository;

    public MongoMarketCatalogRepository(SpringDataMarketCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(MarketCatalog catalog) {
        var document = new MarketCatalogDocument(
                MarketCatalogDocument.generateUUID(),
                catalog.version(),
                catalog.updatedAt(),
                catalog.pairs().stream()
                        .sorted(Comparator.comparing(MarketPair::name))
                        .map(MarketPairDocument::fromDomain)
                        .collect(Collectors.toSet())
        );

        repository.save(document);
    }

    @Override
    public Optional<MarketCatalog> findCurrent() {
        return repository.findFirstByOrderByCatalogVersionDescUpdatedAtDesc()
                .map(MarketCatalogDocument::toDomain);
    }
}
