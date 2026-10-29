package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.consumer;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.message.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.ReleaseAlertMonitoringUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqAlertTriggeredConsumer {

    private final ReleaseAlertMonitoringUseCase useCase;

    public RabbitMqAlertTriggeredConsumer(ReleaseAlertMonitoringUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = "${finalert.rabbitmq.consumers.alert-triggered.queue-name}")
    public void consume(@Payload @Valid AlertTriggeredMessage message) {
        log.info("AlertTriggeredEvent received for monitoring release: eventId={}, alertId={}",
                message.eventId(), message.alertId());
        useCase.execute(message.alertUuid());
    }
}
