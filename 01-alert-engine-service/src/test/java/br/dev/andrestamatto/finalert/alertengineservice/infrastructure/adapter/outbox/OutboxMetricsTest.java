package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxMetricsTest {

    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    @Test
    void exportsMetricNamesExpectedByPrometheusRules() {
        var repository = mock(SpringDataOutboxEventRepository.class);
        when(repository.readMetricsSnapshot()).thenReturn(List.of());
        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        var metrics = new OutboxMetrics(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC));
        metrics.refresh();

        assertThat(registry.scrape())
                .contains("finalert_outbox_events")
                .contains("finalert_outbox_oldest_age_seconds");
    }

    @Test
    void refreshesCountsAndOldestAgeFromARepositorySnapshot() {
        var repository = mock(SpringDataOutboxEventRepository.class);
        var registry = new SimpleMeterRegistry();
        var snapshot = snapshot(
                OutboxEventStatus.PENDING,
                "alert.triggered",
                3,
                NOW.minusSeconds(42)
        );
        when(repository.readMetricsSnapshot()).thenReturn(List.of(snapshot));

        var metrics = new OutboxMetrics(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC));
        metrics.refresh();

        assertThat(gauge(registry, "finalert.outbox.events", "PENDING", "alert.triggered"))
                .isEqualTo(3);
        assertThat(gauge(registry, "finalert.outbox.oldest.age", "PENDING", "alert.triggered"))
                .isEqualTo(42);
        assertThat(gauge(registry, "finalert.outbox.events", "FAILED", "alert.triggered"))
                .isZero();
    }

    @Test
    void resetsMissingSeriesWhenTheSnapshotChanges() {
        var repository = mock(SpringDataOutboxEventRepository.class);
        var registry = new SimpleMeterRegistry();
        var failedSnapshot = snapshot(
                OutboxEventStatus.FAILED,
                "alert.monitoring.requested",
                1,
                NOW.minusSeconds(10)
        );
        when(repository.readMetricsSnapshot())
                .thenReturn(List.of(failedSnapshot))
                .thenReturn(List.of());

        var metrics = new OutboxMetrics(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC));
        metrics.refresh();
        metrics.refresh();

        assertThat(gauge(registry, "finalert.outbox.events", "FAILED", "alert.monitoring.requested"))
                .isZero();
        assertThat(gauge(registry, "finalert.outbox.oldest.age", "FAILED", "alert.monitoring.requested"))
                .isZero();
    }

    @Test
    void preservesTheLastSnapshotWhenTheRepositoryIsUnavailable() {
        var repository = mock(SpringDataOutboxEventRepository.class);
        var registry = new SimpleMeterRegistry();
        var pendingSnapshot = snapshot(
                OutboxEventStatus.PENDING,
                "alert.triggered",
                2,
                NOW.minusSeconds(15)
        );
        when(repository.readMetricsSnapshot())
                .thenReturn(List.of(pendingSnapshot))
                .thenThrow(new IllegalStateException("database unavailable"));

        var metrics = new OutboxMetrics(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC));
        metrics.refresh();
        metrics.refresh();

        assertThat(gauge(registry, "finalert.outbox.events", "PENDING", "alert.triggered"))
                .isEqualTo(2);
        assertThat(gauge(registry, "finalert.outbox.oldest.age", "PENDING", "alert.triggered"))
                .isEqualTo(15);
    }

    private SpringDataOutboxEventRepository.OutboxMetricsSnapshot snapshot(
            OutboxEventStatus status,
            String eventType,
            long count,
            Instant oldestCreatedAt
    ) {
        var snapshot = mock(SpringDataOutboxEventRepository.OutboxMetricsSnapshot.class);
        when(snapshot.getStatus()).thenReturn(status);
        when(snapshot.getEventType()).thenReturn(eventType);
        when(snapshot.getEventCount()).thenReturn(count);
        when(snapshot.getOldestCreatedAt()).thenReturn(oldestCreatedAt);
        return snapshot;
    }

    private double gauge(SimpleMeterRegistry registry, String name, String status, String eventType) {
        return registry.get(name)
                .tags("status", status, "event_type", eventType)
                .gauge()
                .value();
    }
}
