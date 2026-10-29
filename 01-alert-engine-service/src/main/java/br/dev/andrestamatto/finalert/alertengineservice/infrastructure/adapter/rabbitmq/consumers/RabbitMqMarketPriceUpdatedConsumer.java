package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging.MarketPriceUpdatedConsumer;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging.MarketPriceUpdatedMessage;
import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.AlertMarketPriceUpdatedUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqMarketPriceUpdatedConsumer implements MarketPriceUpdatedConsumer {

    private final AlertMarketPriceUpdatedUseCase useCase;

    public RabbitMqMarketPriceUpdatedConsumer(AlertMarketPriceUpdatedUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    @RabbitListener(queues = "${finalert.rabbitmq.consumers.market-price-updated.queue-name}")
    public void consume(@Payload @Valid MarketPriceUpdatedMessage message) {
        log.info("MarketPriceUpdatedEvent received: eventId={}, symbol={}", message.eventId(), message.symbol());
        useCase.propagate(message.toDomain());
    }
}
