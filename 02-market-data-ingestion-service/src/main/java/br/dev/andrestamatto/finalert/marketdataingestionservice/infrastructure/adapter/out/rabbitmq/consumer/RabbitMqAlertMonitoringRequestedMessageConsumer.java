package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.consumer;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.message.AlertMonitoringRequestedMessage;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.AlertMonitoringRequestedMessageConsumer;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RegisterMonitoredMarketPriceUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqAlertMonitoringRequestedMessageConsumer implements AlertMonitoringRequestedMessageConsumer {

    private final RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase;

    public RabbitMqAlertMonitoringRequestedMessageConsumer(
            RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase
    ) {
        this.registerMonitoredMarketPriceUseCase = registerMonitoredMarketPriceUseCase;
    }

    @Override
    @RabbitListener(
            queues = "${finalert.rabbitmq.consumers.market-price-alert.queue-name}"
    )
    public void consume(@Payload @Valid AlertMonitoringRequestedMessage event) {
        log.info(
                "AlertMonitoringRequestedEvent received: eventId={}, alertId={}",
                event.eventId(),
                event.alertId()
        );

        registerMonitoredMarketPriceUseCase.execute(
                AlertMonitoringRequestedMessage.toDomain(event)
        );
    }
}
