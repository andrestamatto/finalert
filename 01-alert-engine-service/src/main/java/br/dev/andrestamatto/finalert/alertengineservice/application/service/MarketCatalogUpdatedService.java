package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.MarketCatalogUpdatedUseCase;
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

        log.info("Iniciando update de market catalog: {}", marketCatalog);

        marketCatalogRepository.save(marketCatalog);
    }

}
