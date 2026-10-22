package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OutboxRetryBackoff {

    private final OutboxProperties properties;

    public OutboxRetryBackoff(OutboxProperties properties) {
        this.properties = properties;
    }

    public Duration delayForAttempt(int failedAttempt) {
        var retry = properties.retry();
        double exponentialMillis = retry.initialDelay().toMillis()
                * Math.pow(retry.multiplier(), Math.max(0, failedAttempt - 1));
        long cappedMillis = Math.min((long) exponentialMillis, retry.maxDelay().toMillis());
        double jitterFactor = retry.jitter() == 0
                ? 1.0
                : ThreadLocalRandom.current().nextDouble(1.0 - retry.jitter(), 1.0 + retry.jitter());
        return Duration.ofMillis(Math.max(1, (long) (cappedMillis * jitterFactor)));
    }
}
