package br.dev.andrestamatto.finalert.notificationservice.application.service;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.AlertEmailSender;
import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.ProcessedNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendAlertNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    @Mock
    private ProcessedNotificationRepository repository;

    @Mock
    private AlertEmailSender emailSender;

    private SendAlertNotificationService service;

    @BeforeEach
    void setUp() {
        service = new SendAlertNotificationService(
                repository,
                emailSender,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void sendsAnUnprocessedNotification() {
        var message = message();
        when(repository.tryClaim(message.eventUuid(), message.alertUuid(), NOW)).thenReturn(true);

        service.execute(message);

        verify(emailSender).send(message);
    }

    @Test
    void ignoresAnAlreadyProcessedEvent() {
        var message = message();
        when(repository.tryClaim(message.eventUuid(), message.alertUuid(), NOW)).thenReturn(false);

        service.execute(message);

        verify(emailSender, never()).send(message);
    }

    private AlertTriggeredMessage message() {
        return new AlertTriggeredMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.10"),
                "GTE",
                new BigDecimal("5.20"),
                NOW.minusSeconds(5),
                NOW
        );
    }
}
