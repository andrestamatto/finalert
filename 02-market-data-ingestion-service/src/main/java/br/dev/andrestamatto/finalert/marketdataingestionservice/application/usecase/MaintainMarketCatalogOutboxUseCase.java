package br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;

import java.util.UUID;

public interface MaintainMarketCatalogOutboxUseCase {

    boolean resubmitFailed(UUID eventId);

    long purgePublished();

    MarketCatalogOutboxSummary summarize();
}
