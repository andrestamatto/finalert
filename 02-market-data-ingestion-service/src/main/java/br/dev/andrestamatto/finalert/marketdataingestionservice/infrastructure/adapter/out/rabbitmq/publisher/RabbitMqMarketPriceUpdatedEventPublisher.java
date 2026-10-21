package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketPriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqMarketPriceUpdatedEventPublisher implements MarketPriceUpdatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitPublishersProperties;

    public RabbitMqMarketPriceUpdatedEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitPublishersProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitPublishersProperties = rabbitPublishersProperties;
    }

    @Override
    public void publish(MarketPriceUpdatedEvent marketPriceUpdatedEvent) {
        log.info("Publishing MarketPriceUpdatedEvent: eventId={}", marketPriceUpdatedEvent.eventId());

        try {
            rabbitTemplate.convertAndSend(
                    rabbitPublishersProperties.marketPriceUpdated().exchangeName(),
                    rabbitPublishersProperties.marketPriceUpdated().routingKey(),
                    marketPriceUpdatedEvent
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish MarketPriceUpdatedEvent: eventId={}",
                    marketPriceUpdatedEvent.eventId(),
                    e
            );
            throw e;
        }

        log.info("MarketPriceUpdatedEvent submitted for publication: eventId={}", marketPriceUpdatedEvent.eventId());
    }
}
