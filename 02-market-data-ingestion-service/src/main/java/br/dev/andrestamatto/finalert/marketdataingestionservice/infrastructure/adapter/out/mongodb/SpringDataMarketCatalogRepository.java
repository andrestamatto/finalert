package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMarketCatalogRepository extends MongoRepository<MarketCatalogDocument, String> {
}
