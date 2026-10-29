package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertTriggeredEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertTriggeredEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketPrice;
import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertMarketPriceUpdatedServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    @Mock
    private AlertRepository repository;

    @Mock
    private AlertTriggeredEventEnqueuer eventEnqueuer;

    private AlertMarketPriceUpdatedService service;

    @BeforeEach
    void setUp() {
        service = new AlertMarketPriceUpdatedService(
                repository,
                eventEnqueuer,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void triggersAndEnqueuesEverySatisfiedAlert() {
        var satisfied = alert("user@example.com", new BigDecimal("5.10"));
        var notSatisfied = alert("other@example.com", new BigDecimal("6.00"));
        var marketPrice = new MarketPrice("USD-BRL", new BigDecimal("5.20"), NOW.minusSeconds(5));
        when(repository.findPendingBySymbol("USD-BRL")).thenReturn(List.of(satisfied, notSatisfied));
        when(repository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.propagate(marketPrice);

        var alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(repository).save(alertCaptor.capture());
        assertThat(alertCaptor.getValue().status()).isEqualTo(AlertStatus.TRIGGERED);

        var eventCaptor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(eventEnqueuer).enqueue(eventCaptor.capture());
        assertThat(eventCaptor.getValue().alertId()).isEqualTo(satisfied.alertId().value().toString());
        assertThat(eventCaptor.getValue().userEmail()).isEqualTo("user@example.com");
        assertThat(eventCaptor.getValue().currentPrice()).isEqualByComparingTo("5.20");
    }

    @Test
    void ignoresAQuoteThatDoesNotSatisfyAnyAlert() {
        when(repository.findPendingBySymbol("USD-BRL"))
                .thenReturn(List.of(alert("user@example.com", new BigDecimal("6.00"))));

        service.propagate(new MarketPrice("USD-BRL", new BigDecimal("5.20"), NOW));

        verify(repository, never()).save(any());
        verify(eventEnqueuer, never()).enqueue(any());
    }

    private Alert alert(String email, BigDecimal target) {
        return new Alert(
                new AlertId(UUID.randomUUID()),
                email,
                "USD-BRL",
                target,
                TriggerOperator.GTE,
                AlertStatus.PENDING,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60),
                null,
                null,
                0L
        );
    }
}
