package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        var publisher = rabbitPublishersProperties.marketCatalogUpdated();
        var correlationData = new CorrelationData(event.eventId().toString());

        try {
            rabbitTemplate.convertAndSend(
                    publisher.exchangeName(),
                    publisher.routingKey(),
                    event,
                    correlationData
            );

            var confirm = correlationData.getFuture().get(
                    publisher.confirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (correlationData.getReturned() != null) {
                var returned = correlationData.getReturned();
                throw new EventPublicationException(
                        "MarketCatalogUpdatedEvent was returned by RabbitMQ: eventId=%s, reply=%s"
                                .formatted(event.eventId(), returned.getReplyText()),
                        null
                );
            }

            if (!confirm.ack()) {
                throw new EventPublicationException(
                        "RabbitMQ negatively acknowledged MarketCatalogUpdatedEvent: eventId=%s, reason=%s"
                                .formatted(event.eventId(), confirm.reason()),
                        null
                );
            }

            log.info("MarketCatalogUpdatedEvent confirmed by RabbitMQ: eventId={}", event.eventId());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publicationException(event, exception);
        } catch (AmqpException | ExecutionException | TimeoutException exception) {
            log.error(
                    "Failed to publish MarketCatalogUpdatedEvent: eventId={}",
                    event.eventId(),
                    exception
            );
            throw publicationException(event, exception);
        }
    }

    private EventPublicationException publicationException(
            MarketCatalogUpdatedEvent event,
            Exception cause
    ) {
        return new EventPublicationException(
                "Failed to publish MarketCatalogUpdatedEvent " + event.eventId(),
                cause
        );
    }
}
