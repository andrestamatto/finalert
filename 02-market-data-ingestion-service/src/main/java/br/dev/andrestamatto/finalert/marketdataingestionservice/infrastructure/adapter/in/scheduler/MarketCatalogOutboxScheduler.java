package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.in.scheduler;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.PublishMarketCatalogOutboxUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketCatalogOutboxScheduler {

    private final PublishMarketCatalogOutboxUseCase publishMarketCatalogOutboxUseCase;

    public MarketCatalogOutboxScheduler(PublishMarketCatalogOutboxUseCase publishMarketCatalogOutboxUseCase) {
        this.publishMarketCatalogOutboxUseCase = publishMarketCatalogOutboxUseCase;
    }

    @Scheduled(
            initialDelayString = "${market-data.outbox.publish-fixed-delay:PT1S}",
            fixedDelayString = "${market-data.outbox.publish-fixed-delay:PT1S}"
    )
    public void publishPendingEvents() {
        publishMarketCatalogOutboxUseCase.execute();
    }

}
