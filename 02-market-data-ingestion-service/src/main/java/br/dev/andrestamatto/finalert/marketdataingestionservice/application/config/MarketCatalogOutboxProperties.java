package br.dev.andrestamatto.finalert.marketdataingestionservice.application.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("market-data.outbox")
public record MarketCatalogOutboxProperties(
        @Min(1) int maxPublishAttempts,
        @Min(1) int maxEventsPerExecution,
        @NotNull Duration initialRetryDelay,
        @DecimalMin("1.0") double retryMultiplier,
        @NotNull Duration processingTimeout,
        @NotNull Duration publishedRetention
) {
}
