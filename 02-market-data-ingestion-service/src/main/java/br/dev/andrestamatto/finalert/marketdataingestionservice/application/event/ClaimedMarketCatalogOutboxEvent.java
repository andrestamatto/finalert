package br.dev.andrestamatto.finalert.marketdataingestionservice.application.event;

import java.util.UUID;

public record ClaimedMarketCatalogOutboxEvent(
        UUID eventId,
        MarketCatalogUpdatedEvent payload,
        int attempt
) {
}
