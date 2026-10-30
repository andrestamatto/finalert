package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MarketPriceMetrics {

    private final Clock clock;
    private final AtomicLong lastSuccessEpochSecond;

    public MarketPriceMetrics(MeterRegistry meterRegistry, Clock clock) {
        this.clock = clock;
        this.lastSuccessEpochSecond = new AtomicLong(Long.MIN_VALUE);

        Gauge.builder(
                        "finalert.market.price.last.success.age.seconds",
                        this,
                        MarketPriceMetrics::lastSuccessAgeSeconds
                )
                .description("Seconds elapsed since a market price event was last confirmed by the broker")
                .register(meterRegistry);
    }

    public void recordPublicationSuccess() {
        lastSuccessEpochSecond.set(clock.instant().getEpochSecond());
    }

    private double lastSuccessAgeSeconds() {
        var lastSuccess = lastSuccessEpochSecond.get();
        if (lastSuccess == Long.MIN_VALUE) {
            return Double.NaN;
        }
        return Math.max(0, clock.instant().getEpochSecond() - lastSuccess);
    }
}
