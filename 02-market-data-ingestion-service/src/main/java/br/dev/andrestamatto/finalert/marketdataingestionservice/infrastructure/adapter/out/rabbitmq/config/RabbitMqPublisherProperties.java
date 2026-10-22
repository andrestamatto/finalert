package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("finalert.rabbitmq.publishers")
public record RabbitMqPublisherProperties(
        @Valid @NotNull EventProperties marketPriceUpdated,
        @Valid @NotNull EventProperties marketCatalogUpdated
) {
    public record EventProperties(
            @NotBlank String exchangeName,
            @NotBlank String routingKey,
            @NotNull Duration confirmTimeout
    ) {}
}
