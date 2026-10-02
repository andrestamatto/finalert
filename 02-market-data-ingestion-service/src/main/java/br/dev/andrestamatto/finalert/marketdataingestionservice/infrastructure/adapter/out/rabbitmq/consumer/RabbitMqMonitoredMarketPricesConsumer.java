package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.consumer;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MonitoredMarketPricesEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.MonitoredMarketPricesEventConsumer;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RegisterMonitoredMarketPriceUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqMonitoredMarketPricesConsumer implements MonitoredMarketPricesEventConsumer {

    private final RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase;

    public RabbitMqMonitoredMarketPricesConsumer(
            RegisterMonitoredMarketPriceUseCase registerMonitoredMarketPriceUseCase
    ) {
        this.registerMonitoredMarketPriceUseCase = registerMonitoredMarketPriceUseCase;
    }

    @Override
    @RabbitListener(
            queues = "${finalert.rabbitmq.consumers.monitored-price-alert.queue-name}"
    )
    public void consume(@Payload @Valid MonitoredMarketPricesEvent monitoredMarketPricesEvent) {
        log.info(
                "Monitored price alert received: eventId={}, alertId={}",
                monitoredMarketPricesEvent.eventId(),
                monitoredMarketPricesEvent.alertId()
        );

        registerMonitoredMarketPriceUseCase.execute(
                MonitoredMarketPricesEvent.toDomain(monitoredMarketPricesEvent)
        );
    }
}
