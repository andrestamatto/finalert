package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.PriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.PriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqPriceUpdatedEventPublisher implements PriceUpdatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitPublishersProperties;

    public RabbitMqPriceUpdatedEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitPublishersProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitPublishersProperties = rabbitPublishersProperties;
    }

    @Override
    public void publish(PriceUpdatedEvent priceUpdatedEvent) {
        log.info("Start publishing priceUpdatedEvent.");

        try {
            rabbitTemplate.convertAndSend(
                    rabbitPublishersProperties.priceUpdated().exchangeName(),
                    rabbitPublishersProperties.priceUpdated().routingKey(),
                    priceUpdatedEvent
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish price event: eventId={}",
                    priceUpdatedEvent.eventId(),
                    e
            );
            throw e;
        }

        log.info("Price event submitted for publication: eventId={}", priceUpdatedEvent.eventId());
    }
}
