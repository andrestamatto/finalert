package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finalert.rabbitmq.publishers")
public record RabbitMqPublisherProperties(
        EventProperties priceUpdated,
        EventProperties marketCatalogUpdated
) {
    public record EventProperties(
            String exchangeName,
            String routingKey
    ) {}
}
