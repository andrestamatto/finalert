package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPriceMetricsTest {

    @Test
    void measuresAgeFromTheLastBrokerConfirmedPublication() {
        var registry = new SimpleMeterRegistry();
        var clock = new MutableClock(Instant.parse("2026-10-30T12:00:00Z"));
        var metrics = new MarketPriceMetrics(registry, clock);

        assertThat(age(registry)).isNaN();
        clock.advanceSeconds(120);
        assertThat(age(registry)).isNaN();

        metrics.recordPublicationSuccess();
        assertThat(age(registry)).isZero();
        clock.advanceSeconds(120);
        assertThat(age(registry)).isEqualTo(120);
    }

    private double age(SimpleMeterRegistry registry) {
        return registry.get("finalert.market.price.last.success.age.seconds").gauge().value();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
