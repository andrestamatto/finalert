package br.dev.andrestamatto.finalert.alertengineservice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    @Test
    void isSatisfiedByMatchingSymbolAndPrice() {
        var alert = alert(AlertStatus.PENDING);

        assertThat(alert.isSatisfiedBy(new MarketPrice("USD-BRL", new BigDecimal("5.11"), NOW))).isTrue();
        assertThat(alert.isSatisfiedBy(new MarketPrice("EUR-BRL", new BigDecimal("5.11"), NOW))).isFalse();
        assertThat(alert.isSatisfiedBy(new MarketPrice("USD-BRL", new BigDecimal("5.09"), NOW))).isFalse();
    }

    @Test
    void marksPendingAlertAsTriggered() {
        var triggered = alert(AlertStatus.PENDING).markAsTriggered(NOW);

        assertThat(triggered.status()).isEqualTo(AlertStatus.TRIGGERED);
        assertThat(triggered.triggeredAt()).isEqualTo(NOW);
        assertThat(triggered.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void refusesToTriggerAnAlertTwice() {
        assertThatThrownBy(() -> alert(AlertStatus.TRIGGERED).markAsTriggered(NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    private Alert alert(AlertStatus status) {
        return new Alert(
                new AlertId(UUID.randomUUID()),
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.10"),
                TriggerOperator.GTE,
                status,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60),
                status == AlertStatus.TRIGGERED ? NOW.minusSeconds(30) : null,
                null,
                0L
        );
    }
}
