package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.repository;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPairDocument;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.SpringDataMarketCatalogOutboxRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document.MarketCatalogOutboxDocument;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoMarketCatalogRepository implements MarketCatalogRepository {

    private final SpringDataMarketCatalogRepository repository;
    private final SpringDataMarketCatalogOutboxRepository outboxRepository;
    private final Clock clock;

    public MongoMarketCatalogRepository(
            SpringDataMarketCatalogRepository repository,
            SpringDataMarketCatalogOutboxRepository outboxRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.clock = clock;
    }

    private void saveCatalog(MarketCatalog catalog) {
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
    @Transactional
    public void saveWithOutbox(MarketCatalog catalog, MarketCatalogUpdatedEvent marketCatalogUpdatedEvent) {
        saveCatalog(catalog);
        outboxRepository.save(
                MarketCatalogOutboxDocument.createFrom(
                        marketCatalogUpdatedEvent,
                        Instant.now(clock)
                )
        );
    }

    @Override
    public Optional<MarketCatalog> findCurrent() {
        return repository.findFirstByOrderByCatalogVersionDescUpdatedAtDesc()
                .map(MarketCatalogDocument::toDomain);
    }
}
