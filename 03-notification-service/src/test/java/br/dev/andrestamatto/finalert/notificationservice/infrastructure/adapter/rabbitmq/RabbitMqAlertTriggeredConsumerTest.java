package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.rabbitmq;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.usecase.SendAlertNotificationUseCase;
import br.dev.andrestamatto.finalert.notificationservice.infrastructure.observability.NotificationMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class RabbitMqAlertTriggeredConsumerTest {

    @Test
    void recordsSuccessAfterTheUseCaseCompletes() {
        var registry = new SimpleMeterRegistry();
        var consumer = consumer(mock(SendAlertNotificationUseCase.class), registry);

        consumer.consume(message());

        assertThat(count(registry, "success")).isEqualTo(1);
        assertThat(count(registry, "failure")).isZero();
    }

    @Test
    void recordsFailureAndPreservesTheExceptionForRabbitRetryHandling() {
        var registry = new SimpleMeterRegistry();
        var useCase = mock(SendAlertNotificationUseCase.class);
        var message = message();
        doThrow(new IllegalStateException("delivery failed")).when(useCase).execute(message);
        var consumer = consumer(useCase, registry);

        assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("delivery failed");
        assertThat(count(registry, "success")).isZero();
        assertThat(count(registry, "failure")).isEqualTo(1);
    }

    private RabbitMqAlertTriggeredConsumer consumer(
            SendAlertNotificationUseCase useCase,
            SimpleMeterRegistry registry
    ) {
        return new RabbitMqAlertTriggeredConsumer(useCase, new NotificationMetrics(registry));
    }

    private double count(SimpleMeterRegistry registry, String outcome) {
        return registry.get("finalert.notification.process")
                .tag("outcome", outcome)
                .counter()
                .count();
    }

    private AlertTriggeredMessage message() {
        var now = Instant.parse("2026-10-30T12:00:00Z");
        return new AlertTriggeredMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.10"),
                "GTE",
                new BigDecimal("5.20"),
                now.minusSeconds(5),
                now
        );
    }
}
