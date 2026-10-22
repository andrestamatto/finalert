package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertMonitoringRequestedEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.exceptions.IdempotencyConflictException;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.commands.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertMonitoringRequestedEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.CreateAlertRequestRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertMonitoringRequestedEventEnqueuer eventEnqueuer;

    @Mock
    private CreateAlertRequestRepository requestRepository;

    @Mock
    private MarketCatalogRepository marketCatalogRepository;

    @InjectMocks
    private CreateAlertService service;

    @BeforeEach
    void configureAvailableMarketPair() {
        when(marketCatalogRepository.containsPair("USD-BRL")).thenReturn(true);
    }

    @Test
    void createsAlertAndOutboxEventForANewIdempotencyKey() {
        var command = command("request-1");
        when(requestRepository.tryAcquire(anyString(), anyString(), any())).thenReturn(true);
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = service.execute(command);

        ArgumentCaptor<AlertMonitoringRequestedEvent> event = ArgumentCaptor.forClass(AlertMonitoringRequestedEvent.class);
        verify(eventEnqueuer).enqueue(event.capture());
        verify(requestRepository).complete(anyString(), any(AlertId.class), any());
        assertThat(result.alertId()).isNotNull();
        assertThat(event.getValue().alertId()).isEqualTo(result.alertId().value().toString());
        assertThat(event.getValue().targetValue()).isEqualByComparingTo(command.targetPrice());
    }

    @Test
    void returnsTheExistingAlertForAnEquivalentReplay() {
        var command = command("request-1");
        var existingAlert = alert(UUID.randomUUID());
        var capturedHash = new AtomicReference<String>();
        when(requestRepository.tryAcquire(anyString(), anyString(), any())).thenAnswer(invocation -> {
            capturedHash.set(invocation.getArgument(1));
            return false;
        });
        when(requestRepository.findByIdempotencyKey(command.idempotencyKey())).thenAnswer(invocation -> Optional.of(
                new CreateAlertRequestRepository.CreateAlertRequestState(
                        command.idempotencyKey(),
                        capturedHash.get(),
                        existingAlert.alertId()
                )
        ));
        when(alertRepository.findById(existingAlert.alertId())).thenReturn(Optional.of(existingAlert));

        Alert result = service.execute(command);

        assertThat(result).isSameAs(existingAlert);
        verify(alertRepository, never()).save(any());
        verify(eventEnqueuer, never()).enqueue(any());
    }

    @Test
    void rejectsReuseOfAKeyForADifferentRequest() {
        var command = command("request-1");
        when(requestRepository.tryAcquire(anyString(), anyString(), any())).thenReturn(false);
        when(requestRepository.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.of(
                new CreateAlertRequestRepository.CreateAlertRequestState(
                        command.idempotencyKey(),
                        "a-different-request-hash",
                        new AlertId(UUID.randomUUID())
                )
        ));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(alertRepository, never()).save(any());
        verify(eventEnqueuer, never()).enqueue(any());
    }

    private CreateAlertCommand command(String idempotencyKey) {
        return new CreateAlertCommand(
                idempotencyKey,
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.60"),
                TriggerOperator.GTE
        );
    }

    private Alert alert(UUID id) {
        var now = Instant.parse("2026-10-13T12:00:00Z");
        return new Alert(
                new AlertId(id),
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.60"),
                TriggerOperator.GTE,
                AlertStatus.PENDING,
                now,
                now,
                null,
                null,
                0L
        );
    }
}
