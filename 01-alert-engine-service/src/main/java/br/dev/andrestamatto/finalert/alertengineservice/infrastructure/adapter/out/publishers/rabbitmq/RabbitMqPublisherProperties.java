package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.publishers.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finalert.rabbitmq.publishers")
public record RabbitMqPublisherProperties(
    EventProperties alertCreated
) {
    public record EventProperties(
            String exchangeName,
            String routingKey
    ) {}
}
