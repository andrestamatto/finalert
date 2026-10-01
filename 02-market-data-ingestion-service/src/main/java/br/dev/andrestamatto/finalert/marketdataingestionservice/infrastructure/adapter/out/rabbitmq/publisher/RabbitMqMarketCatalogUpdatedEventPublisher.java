package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.CatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqMarketCatalogUpdatedEventPublisher implements MarketCatalogUpdatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitPublishersProperties;

    public RabbitMqMarketCatalogUpdatedEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitPublishersProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitPublishersProperties = rabbitPublishersProperties;
    }

    @Override
    public void publish(CatalogUpdatedEvent catalogUpdatedEvent) {
        log.info("Start publishing catalogUpdatedEvent.");

        try {
            rabbitTemplate.convertAndSend(
                    rabbitPublishersProperties.marketCatalogUpdated().exchangeName(),
                    rabbitPublishersProperties.marketCatalogUpdated().routingKey(),
                    catalogUpdatedEvent
            );

            log.info("CatalogUpdatedEvent submitted for publication: eventId={}", catalogUpdatedEvent.eventId());

        } catch (AmqpException exception){
            log.warn(
                    "Failed to publish price event: eventId={}",
                    catalogUpdatedEvent.eventId(),
                    exception
            );
        }

    }
}
