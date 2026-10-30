package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.rabbitmq;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.usecase.SendAlertNotificationUseCase;
import br.dev.andrestamatto.finalert.notificationservice.infrastructure.observability.NotificationMetrics;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqAlertTriggeredConsumer {

    private final SendAlertNotificationUseCase useCase;
    private final NotificationMetrics metrics;

    public RabbitMqAlertTriggeredConsumer(
            SendAlertNotificationUseCase useCase,
            NotificationMetrics metrics
    ) {
        this.useCase = useCase;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "${finalert.rabbitmq.alert-triggered.queue-name}")
    public void consume(@Payload @Valid AlertTriggeredMessage message) {
        log.info("AlertTriggeredEvent received: eventId={}, alertId={}", message.eventId(), message.alertId());
        try {
            useCase.execute(message);
            metrics.recordSuccess();
        } catch (RuntimeException | Error exception) {
            metrics.recordFailure();
            throw exception;
        }
    }
}
