package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.rabbitmq;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.usecase.SendAlertNotificationUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqAlertTriggeredConsumer {

    private final SendAlertNotificationUseCase useCase;

    public RabbitMqAlertTriggeredConsumer(SendAlertNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = "${finalert.rabbitmq.alert-triggered.queue-name}")
    public void consume(@Payload @Valid AlertTriggeredMessage message) {
        log.info("AlertTriggeredEvent received: eventId={}, alertId={}", message.eventId(), message.alertId());
        useCase.execute(message);
    }
}
