package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.CatalogUpdatedEvent;

public interface MarketCatalogUpdatedEventPublisher {

    void publish(CatalogUpdatedEvent catalogUpdatedEvent);
}
