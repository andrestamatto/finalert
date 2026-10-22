package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("finalert.rabbitmq.consumers")
public record RabbitMqConsumerProperties(
    @Valid @NotNull EventProperties marketCatalogUpdated
) {
    public record EventProperties(
            @NotBlank
            String queueName,
            @NotBlank
            String exchangeName,
            @NotBlank
            String routingKey
    ) {}
}
