package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;

public interface MarketCatalogUpdatedEventPublisher {

    void publish(MarketCatalogUpdatedEvent event);
}
