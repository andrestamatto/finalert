package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringDataMarketCatalogRepository extends MongoRepository<MarketCatalogDocument, String> {

    Optional<MarketCatalogDocument> findFirstByOrderByCatalogVersionDescUpdatedAtDesc();
}
