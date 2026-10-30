package br.dev.andrestamatto.finalert.notificationservice.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter success;
    private final Counter failure;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        success = counter(meterRegistry, "success");
        failure = counter(meterRegistry, "failure");
    }

    public void recordSuccess() {
        success.increment();
    }

    public void recordFailure() {
        failure.increment();
    }

    private Counter counter(MeterRegistry meterRegistry, String outcome) {
        return Counter.builder("finalert.notification.process")
                .description("Alert notification processing attempts by outcome")
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
