package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.PriceUpdatedEvent;

public interface PriceUpdatedEventPublisher {
    void publish(PriceUpdatedEvent event);
}
