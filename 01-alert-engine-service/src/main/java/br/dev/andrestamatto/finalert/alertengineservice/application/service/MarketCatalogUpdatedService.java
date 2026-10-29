package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.MarketCatalogUpdatedUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MarketCatalogUpdatedService implements MarketCatalogUpdatedUseCase {

    private final MarketCatalogRepository marketCatalogRepository;

    public MarketCatalogUpdatedService(MarketCatalogRepository marketCatalogRepository) {
        this.marketCatalogRepository = marketCatalogRepository;
    }

    @Override
    public void save(MarketCatalog marketCatalog) {
        if (!marketCatalogRepository.saveIfAbsent(marketCatalog)) {
            log.info(
                    "Ignoring duplicate MarketCatalogUpdatedEvent: eventId={}",
                    marketCatalog.eventId()
            );
            return;
        }

        log.info(
                "Updated market catalog saved: eventId={}, version={}",
                marketCatalog.eventId(),
                marketCatalog.version()
        );
    }

}
