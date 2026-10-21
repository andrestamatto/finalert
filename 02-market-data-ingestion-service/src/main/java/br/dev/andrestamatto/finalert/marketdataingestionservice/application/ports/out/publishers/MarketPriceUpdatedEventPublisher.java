package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;

public interface MarketPriceUpdatedEventPublisher {
    void publish(MarketPriceUpdatedEvent event);
}
