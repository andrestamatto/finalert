package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers;

import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.MarketCatalogUpdatedUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging.MarketCatalogUpdatedMessage;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging.MarketCatalogUpdatedConsumer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqMarketCatalogUpdatedConsumer implements MarketCatalogUpdatedConsumer {

    private final MarketCatalogUpdatedUseCase marketCatalogUpdatedUseCase;

    public RabbitMqMarketCatalogUpdatedConsumer(MarketCatalogUpdatedUseCase marketCatalogUpdatedUseCase) {
        this.marketCatalogUpdatedUseCase = marketCatalogUpdatedUseCase;
    }

    @Override
    @RabbitListener(
            queues = "${finalert.rabbitmq.consumers.market-catalog-updated.queue-name}"
    )
    public void consume(@Payload @Valid MarketCatalogUpdatedMessage message) {
        log.info("Market Catalog Updated Event Received: {}", message);

        marketCatalogUpdatedUseCase.save(
                MarketCatalogUpdatedMessage.toDomain(message)
        );

    }
}
