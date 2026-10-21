package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class RabbitMqMarketCatalogUpdatedEventPublisherTest {

    @Test
    void propagatesPublicationFailure() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var properties = new RabbitMqPublisherProperties(
                new RabbitMqPublisherProperties.EventProperties("market.events", "market.price.updated"),
                new RabbitMqPublisherProperties.EventProperties("market.events", "market.catalog.updated")
        );
        var publisher = new RabbitMqMarketCatalogUpdatedEventPublisher(rabbitTemplate, properties);
        var event = new MarketCatalogUpdatedEvent(
                UUID.randomUUID(),
                Map.of("USD-BRL", "Dólar/Real"),
                Instant.parse("2026-10-21T12:00:00Z"),
                1L
        );
        var failure = new AmqpException("RabbitMQ unavailable");
        doThrow(failure).when(rabbitTemplate).convertAndSend(
                "market.events",
                "market.catalog.updated",
                event
        );

        assertThatThrownBy(() -> publisher.publish(event)).isSameAs(failure);
    }
}
