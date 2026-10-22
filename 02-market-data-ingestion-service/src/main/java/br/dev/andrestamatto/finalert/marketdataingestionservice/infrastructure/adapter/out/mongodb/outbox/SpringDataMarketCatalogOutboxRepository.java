package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document.MarketCatalogOutboxDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface SpringDataMarketCatalogOutboxRepository extends MongoRepository<MarketCatalogOutboxDocument, UUID> {

}
