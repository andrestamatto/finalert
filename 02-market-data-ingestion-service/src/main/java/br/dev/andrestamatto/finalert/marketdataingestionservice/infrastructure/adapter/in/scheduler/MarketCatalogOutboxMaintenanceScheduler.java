package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.in.scheduler;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.MaintainMarketCatalogOutboxUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketCatalogOutboxMaintenanceScheduler {

    private final MaintainMarketCatalogOutboxUseCase useCase;

    public MarketCatalogOutboxMaintenanceScheduler(MaintainMarketCatalogOutboxUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(
            initialDelayString = "${market-data.outbox.cleanup-fixed-delay:PT1H}",
            fixedDelayString = "${market-data.outbox.cleanup-fixed-delay:PT1H}"
    )
    public void purgePublishedEvents() {
        var deleted = useCase.purgePublished();
        if (deleted > 0) {
            log.info("Purged {} published market catalog outbox events.", deleted);
        }
    }
}
