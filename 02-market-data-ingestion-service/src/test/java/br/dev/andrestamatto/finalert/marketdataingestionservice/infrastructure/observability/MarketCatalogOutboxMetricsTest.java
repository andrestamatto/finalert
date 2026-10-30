package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.observability;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketCatalogOutboxMetricsTest {

    @Test
    void refreshesFixedCardinalityGaugesFromOneRepositorySnapshot() {
        var repository = mock(MarketCatalogOutboxRepository.class);
        var registry = new SimpleMeterRegistry();
        var now = Instant.parse("2026-10-30T12:00:00Z");
        var metrics = new MarketCatalogOutboxMetrics(
                repository,
                registry,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        when(repository.summarize()).thenReturn(new MarketCatalogOutboxSummary(
                2,
                1,
                10,
                3,
                Optional.of(now.minusSeconds(45)),
                Optional.of(now.minusSeconds(10))
        ));

        metrics.refresh();

        assertThat(value(registry, "PENDING")).isEqualTo(2);
        assertThat(value(registry, "PROCESSING")).isEqualTo(1);
        assertThat(value(registry, "PUBLISHED")).isEqualTo(10);
        assertThat(value(registry, "FAILED")).isEqualTo(3);
        assertThat(age(registry, "PENDING")).isEqualTo(45);
        assertThat(age(registry, "PROCESSING")).isEqualTo(10);
    }

    @Test
    void preservesThePreviousSnapshotWhenMongoIsTemporarilyUnavailable() {
        var repository = mock(MarketCatalogOutboxRepository.class);
        var registry = new SimpleMeterRegistry();
        var now = Instant.parse("2026-10-30T12:00:00Z");
        var metrics = new MarketCatalogOutboxMetrics(
                repository,
                registry,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        when(repository.summarize())
                .thenReturn(new MarketCatalogOutboxSummary(
                        2,
                        1,
                        10,
                        3,
                        Optional.of(now.minusSeconds(45)),
                        Optional.empty()
                ))
                .thenThrow(new IllegalStateException("Mongo unavailable"));

        metrics.refresh();
        metrics.refresh();

        assertThat(value(registry, "PENDING")).isEqualTo(2);
        assertThat(value(registry, "FAILED")).isEqualTo(3);
        assertThat(age(registry, "PENDING")).isEqualTo(45);
    }

    private double value(SimpleMeterRegistry registry, String status) {
        return registry.get("finalert.outbox.events")
                .tag("status", status)
                .tag("event_type", "MARKET_CATALOG_UPDATED")
                .gauge()
                .value();
    }

    private double age(SimpleMeterRegistry registry, String status) {
        return registry.get("finalert.outbox.oldest.age.seconds")
                .tag("status", status)
                .tag("event_type", "MARKET_CATALOG_UPDATED")
                .gauge()
                .value();
    }
}
