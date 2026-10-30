package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketPriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.observability.MarketPriceMetrics;
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
public class RabbitMqMarketPriceUpdatedEventPublisher implements MarketPriceUpdatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitPublishersProperties;
    private final MarketPriceMetrics metrics;

    public RabbitMqMarketPriceUpdatedEventPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMqPublisherProperties rabbitPublishersProperties,
            MarketPriceMetrics metrics
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitPublishersProperties = rabbitPublishersProperties;
        this.metrics = metrics;
    }

    @Override
    public void publish(MarketPriceUpdatedEvent marketPriceUpdatedEvent) {
        log.info("Publishing MarketPriceUpdatedEvent: eventId={}", marketPriceUpdatedEvent.eventId());

        var publisher = rabbitPublishersProperties.marketPriceUpdated();
        var correlationData = new CorrelationData(marketPriceUpdatedEvent.eventId().toString());

        try {
            rabbitTemplate.convertAndSend(
                    publisher.exchangeName(),
                    publisher.routingKey(),
                    marketPriceUpdatedEvent,
                    correlationData
            );

            var confirm = correlationData.getFuture().get(
                    publisher.confirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (correlationData.getReturned() != null) {
                throw new EventPublicationException(
                        "MarketPriceUpdatedEvent was returned by RabbitMQ: "
                                + marketPriceUpdatedEvent.eventId(),
                        null
                );
            }

            if (!confirm.ack()) {
                throw new EventPublicationException(
                        "RabbitMQ negatively acknowledged MarketPriceUpdatedEvent: "
                                + marketPriceUpdatedEvent.eventId(),
                        null
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publicationException(marketPriceUpdatedEvent, exception);
        } catch (AmqpException | ExecutionException | TimeoutException exception) {
            log.warn(
                    "Failed to publish MarketPriceUpdatedEvent: eventId={}",
                    marketPriceUpdatedEvent.eventId(),
                    exception
            );
            throw publicationException(marketPriceUpdatedEvent, exception);
        }

        metrics.recordPublicationSuccess();
        log.info("MarketPriceUpdatedEvent confirmed by RabbitMQ: eventId={}", marketPriceUpdatedEvent.eventId());
    }

    private EventPublicationException publicationException(
            MarketPriceUpdatedEvent event,
            Exception cause
    ) {
        return new EventPublicationException(
                "Failed to publish MarketPriceUpdatedEvent " + event.eventId(),
                cause
        );
    }
}
