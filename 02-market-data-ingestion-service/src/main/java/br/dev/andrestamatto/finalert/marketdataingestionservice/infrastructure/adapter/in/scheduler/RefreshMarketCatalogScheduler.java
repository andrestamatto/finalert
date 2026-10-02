package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.in.scheduler;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RefreshMarketCatalogUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshMarketCatalogScheduler {

    private final RefreshMarketCatalogUseCase refreshMarketCatalogUseCase;

    public RefreshMarketCatalogScheduler(
            RefreshMarketCatalogUseCase refreshMarketCatalogUseCase
    ) {
        this.refreshMarketCatalogUseCase = refreshMarketCatalogUseCase;
    }

    @Scheduled(
        cron = "${market-data.catalog-refresh-cron:0 0 3 * * *}",
        zone = "${market-data.catalog-refresh-zone:America/Fortaleza}"
    )
    public void refreshCatalog() throws Exception {
        refreshMarketCatalogUseCase.execute();
    }


}
