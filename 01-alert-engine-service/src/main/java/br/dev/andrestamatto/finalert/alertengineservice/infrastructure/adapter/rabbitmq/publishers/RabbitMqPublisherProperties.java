package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("finalert.rabbitmq.publishers")
public record RabbitMqPublisherProperties(
    @Valid @NotNull EventProperties alertMonitoringRequested
) {
    public record EventProperties(
            @NotBlank
            String exchangeName,
            @NotBlank
            String routingKey
    ) {}
}
