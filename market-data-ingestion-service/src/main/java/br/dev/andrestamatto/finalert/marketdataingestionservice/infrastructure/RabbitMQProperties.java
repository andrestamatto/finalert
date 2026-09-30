package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("finalert.rabbitmq.properties")
public record RabbitMQProperties(
        String queueName,
        String exchangeName,
        String routingKey
) {
}
