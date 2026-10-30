package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxMetrics {

    private static final Logger log = LoggerFactory.getLogger(OutboxMetrics.class);
    private static final Set<String> EVENT_TYPES = Set.of("alert.monitoring.requested", "alert.triggered");

    private final SpringDataOutboxEventRepository repository;
    private final Clock clock;
    private final Map<MetricKey, AtomicLong> eventCounts = new HashMap<>();
    private final Map<MetricKey, AtomicLong> oldestAges = new HashMap<>();

    public OutboxMetrics(
            SpringDataOutboxEventRepository repository,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;

        for (var status : OutboxEventStatus.values()) {
            for (var eventType : EVENT_TYPES) {
                var key = new MetricKey(status, eventType);
                eventCounts.put(key, registerGauge(
                        meterRegistry,
                        "finalert.outbox.events",
                        "Current number of outbox events",
                        null,
                        key
                ));
                oldestAges.put(key, registerGauge(
                        meterRegistry,
                        "finalert.outbox.oldest.age",
                        "Age of the oldest outbox event",
                        "seconds",
                        key
                ));
            }
        }
    }

    @Scheduled(fixedDelayString = "${finalert.outbox.metrics-fixed-delay:5s}")
    void refresh() {
        try {
            var snapshots = repository.readMetricsSnapshot();
            eventCounts.values().forEach(value -> value.set(0));
            oldestAges.values().forEach(value -> value.set(0));

            for (var snapshot : snapshots) {
                var key = new MetricKey(snapshot.getStatus(), snapshot.getEventType());
                var count = eventCounts.get(key);
                var age = oldestAges.get(key);
                if (count == null || age == null) {
                    log.warn("Ignoring outbox metrics for unknown event type: {}", snapshot.getEventType());
                    continue;
                }

                count.set(snapshot.getEventCount());
                age.set(Math.max(0, Duration.between(snapshot.getOldestCreatedAt(), clock.instant()).toSeconds()));
            }
        } catch (RuntimeException exception) {
            log.warn("Could not refresh outbox metrics snapshot", exception);
        }
    }

    private AtomicLong registerGauge(
            MeterRegistry registry,
            String name,
            String description,
            String baseUnit,
            MetricKey key
    ) {
        var value = new AtomicLong();
        var builder = Gauge.builder(name, value, AtomicLong::get)
                .description(description)
                .tag("status", key.status().name())
                .tag("event_type", key.eventType());
        if (baseUnit != null) {
            builder.baseUnit(baseUnit);
        }
        builder.register(registry);
        return value;
    }

    private record MetricKey(OutboxEventStatus status, String eventType) {
    }
}
