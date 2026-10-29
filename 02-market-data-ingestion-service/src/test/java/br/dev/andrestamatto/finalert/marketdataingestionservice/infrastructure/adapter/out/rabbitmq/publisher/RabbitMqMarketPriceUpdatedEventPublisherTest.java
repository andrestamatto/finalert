package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.publisher;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception.EventPublicationException;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RabbitMqMarketPriceUpdatedEventPublisherTest {

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
                eq("market.price.updated"),
                eq(event),
                any(CorrelationData.class)
        );

        publisher.publish(event);
    }

    @Test
    void failsWhenTheBrokerDoesNotConfirmWithinTheTimeout() {
        var publisher = publisher(mock(RabbitTemplate.class), Duration.ofMillis(1));

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(EventPublicationException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    private RabbitMqMarketPriceUpdatedEventPublisher publisher(
            RabbitTemplate rabbitTemplate,
            Duration confirmTimeout
    ) {
        var eventProperties = new RabbitMqPublisherProperties.EventProperties(
                "market.events",
                "market.price.updated",
                confirmTimeout
        );
        return new RabbitMqMarketPriceUpdatedEventPublisher(
                rabbitTemplate,
                new RabbitMqPublisherProperties(eventProperties, eventProperties)
        );
    }

    private MarketPriceUpdatedEvent event() {
        return new MarketPriceUpdatedEvent(
                UUID.randomUUID(),
                "USD-BRL",
                new BigDecimal("5.20"),
                Instant.parse("2026-10-29T12:00:00Z")
        );
    }
}
