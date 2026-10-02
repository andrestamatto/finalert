package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finalert.rabbitmq.consumers")
public record RabbitMqConsumersProperties(
        EventProperties monitoredPriceAlert
) {
    public record EventProperties(
            String queueName,
            String exchangeName,
            String routingKey
    ) {}
}
