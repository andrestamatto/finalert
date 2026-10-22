package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("finalert.outbox")
public record OutboxProperties(
        @NotNull Duration workerFixedDelay,
        @Min(1) int batchSize,
        @NotNull Duration publisherConfirmTimeout,
        @Valid @NotNull Retry retry
) {
    public record Retry(
            @Min(1) int maxAttempts,
            @NotNull Duration initialDelay,
            @NotNull Duration maxDelay,
            @DecimalMin("1.0") double multiplier,
            @DecimalMin("0.0") @DecimalMax("1.0") double jitter
    ) {
    }
}
