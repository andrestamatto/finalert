package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
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
    public void publish(MarketCatalogUpdatedEvent event) {
        log.info("Publishing MarketCatalogUpdatedEvent: eventId={}", event.eventId());

        try {
            rabbitTemplate.convertAndSend(
                    rabbitPublishersProperties.marketCatalogUpdated().exchangeName(),
                    rabbitPublishersProperties.marketCatalogUpdated().routingKey(),
                    event
            );

            log.info("MarketCatalogUpdatedEvent submitted for publication: eventId={}", event.eventId());

        } catch (AmqpException exception){
            log.error(
                    "Failed to publish MarketCatalogUpdatedEvent: eventId={}",
                    event.eventId(),
                    exception
            );
            throw exception;
        }

    }
}
