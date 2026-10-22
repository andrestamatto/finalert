package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class RabbitMqMarketCatalogUpdatedEventPublisherTest {

    @Test
    void completesOnlyAfterTheBrokerAcknowledgesThePublication() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = publisher(rabbitTemplate, Duration.ofSeconds(1));
        var event = event();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq("market.events"),
                eq("market.catalog.updated"),
                eq(event),
                any(CorrelationData.class)
        );

        publisher.publish(event);
    }

    @Test
    void translatesAnAmqpFailure() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = publisher(rabbitTemplate, Duration.ofSeconds(1));
        var event = event();
        var failure = new AmqpException("RabbitMQ unavailable");
        doThrow(failure).when(rabbitTemplate).convertAndSend(
                eq("market.events"),
                eq("market.catalog.updated"),
                eq(event),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(EventPublicationException.class)
                .hasCause(failure);
    }

    @Test
    void rejectsANegativeBrokerAcknowledgement() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = publisher(rabbitTemplate, Duration.ofSeconds(1));
        var event = event();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(false, "broker rejected")
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(Object.class),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(EventPublicationException.class)
                .hasMessageContaining("negatively acknowledged");
    }

    @Test
    void rejectsAnUnroutableReturnedMessage() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = publisher(rabbitTemplate, Duration.ofSeconds(1));
        var event = event();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.setReturned(new ReturnedMessage(
                    new Message(new byte[0]),
                    312,
                    "NO_ROUTE",
                    "market.events",
                    "market.catalog.updated"
            ));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(Object.class),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(EventPublicationException.class)
                .hasMessageContaining("returned by RabbitMQ");
    }

    @Test
    void failsWhenTheBrokerDoesNotConfirmWithinTheTimeout() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = publisher(rabbitTemplate, Duration.ofMillis(1));

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(EventPublicationException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    private RabbitMqMarketCatalogUpdatedEventPublisher publisher(
            RabbitTemplate rabbitTemplate,
            Duration confirmTimeout
    ) {
        var properties = new RabbitMqPublisherProperties(
                new RabbitMqPublisherProperties.EventProperties(
                        "market.events",
                        "market.price.updated",
                        confirmTimeout
                ),
                new RabbitMqPublisherProperties.EventProperties(
                        "market.events",
                        "market.catalog.updated",
                        confirmTimeout
                )
        );
        return new RabbitMqMarketCatalogUpdatedEventPublisher(rabbitTemplate, properties);
    }

    private MarketCatalogUpdatedEvent event() {
        return new MarketCatalogUpdatedEvent(
                UUID.randomUUID(),
                Map.of("USD-BRL", "Dolar/Real"),
                Instant.parse("2026-10-21T12:00:00Z"),
                1L
        );
    }
}
