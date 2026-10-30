package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.observability;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class MarketCatalogOutboxMetrics {

    private static final String EVENT_TYPE = "MARKET_CATALOG_UPDATED";
    private final MarketCatalogOutboxRepository repository;
    private final Clock clock;
    private final Map<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> oldestCreatedAt = new ConcurrentHashMap<>();

    public MarketCatalogOutboxMetrics(
            MarketCatalogOutboxRepository repository,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
        registerGauge(meterRegistry, "PENDING");
        registerGauge(meterRegistry, "PROCESSING");
        registerGauge(meterRegistry, "PUBLISHED");
        registerGauge(meterRegistry, "FAILED");
        registerAgeGauge(meterRegistry, "PENDING");
        registerAgeGauge(meterRegistry, "PROCESSING");
    }

    @Scheduled(fixedDelayString = "${market-data.outbox.metrics-refresh-rate:PT15S}")
    public void refresh() {
        try {
            var summary = repository.summarize();
            eventCounts.get("PENDING").set(summary.pending());
            eventCounts.get("PROCESSING").set(summary.processing());
            eventCounts.get("PUBLISHED").set(summary.published());
            eventCounts.get("FAILED").set(summary.failed());
            oldestCreatedAt.get("PENDING").set(epochSecondOrMissing(
                    summary.oldestPendingCreatedAt().orElse(null)
            ));
            oldestCreatedAt.get("PROCESSING").set(epochSecondOrMissing(
                    summary.oldestProcessingCreatedAt().orElse(null)
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh market catalog outbox metrics; preserving previous snapshot", exception);
        }
    }

    private void registerGauge(MeterRegistry meterRegistry, String status) {
        var value = new AtomicLong();
        eventCounts.put(status, value);
        Gauge.builder("finalert.outbox.events", value, AtomicLong::get)
                .description("Current number of outbox events by status")
                .tag("status", status)
                .tag("event_type", EVENT_TYPE)
                .register(meterRegistry);
    }

    private void registerAgeGauge(MeterRegistry meterRegistry, String status) {
        var createdAt = new AtomicLong(Long.MIN_VALUE);
        oldestCreatedAt.put(status, createdAt);
        Gauge.builder("finalert.outbox.oldest.age.seconds", createdAt, this::ageSeconds)
                .description("Age in seconds of the oldest active outbox event")
                .tag("status", status)
                .tag("event_type", EVENT_TYPE)
                .register(meterRegistry);
    }

    private double ageSeconds(AtomicLong createdAt) {
        var epochSecond = createdAt.get();
        if (epochSecond == Long.MIN_VALUE) {
            return Double.NaN;
        }
        return Math.max(0, clock.instant().getEpochSecond() - epochSecond);
    }

    private long epochSecondOrMissing(Instant instant) {
        return instant == null ? Long.MIN_VALUE : instant.getEpochSecond();
    }
}
