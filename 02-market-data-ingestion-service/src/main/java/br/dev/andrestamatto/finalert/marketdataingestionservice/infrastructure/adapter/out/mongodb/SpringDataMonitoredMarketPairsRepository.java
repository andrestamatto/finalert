package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document.MarketPriceAlertDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface SpringDataMonitoredMarketPairsRepository extends MongoRepository<MarketPriceAlertDocument, UUID> {
}
