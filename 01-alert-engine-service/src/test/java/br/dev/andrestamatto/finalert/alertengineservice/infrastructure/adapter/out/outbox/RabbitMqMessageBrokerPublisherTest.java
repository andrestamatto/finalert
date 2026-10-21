package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.outbox;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.OutboxEventEntity;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.OutboxProperties;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.OutboxPublishException;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.RabbitMqMessageBrokerPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RabbitMqMessageBrokerPublisherTest {

    @Test
    void waitsForAPositiveBrokerConfirmation() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var correlation = ArgumentCaptor.forClass(CorrelationData.class);
        doAnswer(invocation -> {
            CorrelationData data = invocation.getArgument(3);
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(
                eq("alert.events"),
                eq("alert.monitoring.requested"),
                any(Message.class),
                correlation.capture()
        );

        new RabbitMqMessageBrokerPublisher(rabbitTemplate, properties()).publish(event());

        assertThat(correlation.getValue().getId()).isNotBlank();
    }

    @Test
    void treatsAnUnroutableReturnedMessageAsAFailure() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            CorrelationData data = invocation.getArgument(3);
            Message message = invocation.getArgument(2);
            data.setReturned(new ReturnedMessage(
                    message,
                    312,
                    "NO_ROUTE",
                    "alert.events",
                    "alert.monitoring.requested"
            ));
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(
                eq("alert.events"),
                eq("alert.monitoring.requested"),
                any(Message.class),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> new RabbitMqMessageBrokerPublisher(
                rabbitTemplate,
                properties()
        ).publish(event()))
                .isInstanceOf(OutboxPublishException.class)
                .hasMessageContaining("NO_ROUTE");
    }

    private OutboxProperties properties() {
        return new OutboxProperties(
                Duration.ofSeconds(1),
                10,
                Duration.ofSeconds(5),
                new OutboxProperties.Retry(3, Duration.ofSeconds(1), Duration.ofMinutes(1), 2, 0.2)
        );
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alert.monitoring.requested",
                "alert.events",
                "alert.monitoring.requested",
                "{}",
                Instant.parse("2026-10-13T12:00:00Z")
        );
    }
}
