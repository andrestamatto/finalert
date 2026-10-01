package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.PriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.PriceUpdatedEventPublisher;

public class RabbitMqPriceUpdatedEventPublisher implements PriceUpdatedEventPublisher {

    @Override
    public void publish(PriceUpdatedEvent event) {

    }
}
