package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.consumer;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.AlertMonitoringRequestedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.AlertMonitoringRequestedEventConsumer;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RegisterMonitoredMarketPriceUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqAlertMonitoringRequestedEventConsumer implements AlertMonitoringRequestedEventConsumer {

    private final RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase;

    public RabbitMqAlertMonitoringRequestedEventConsumer(
            RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase
    ) {
        this.registerMonitoredMarketPriceUseCase = registerMonitoredMarketPriceUseCase;
    }

    @Override
    @RabbitListener(
            queues = "${finalert.rabbitmq.consumers.market-price-alert.queue-name}"
    )
    public void consume(@Payload @Valid AlertMonitoringRequestedEvent event) {
        log.info(
                "AlertMonitoringRequestedEvent received: eventId={}, alertId={}",
                event.eventId(),
                event.alertId()
        );

        registerMonitoredMarketPriceUseCase.execute(
                AlertMonitoringRequestedEvent.toDomain(event)
        );
    }
}
