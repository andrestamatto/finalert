package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.mail;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringMailAlertEmailSenderTest {

    @Test
    void buildsAnEmailFromTheTriggeredAlert() {
        var mailSender = mock(JavaMailSender.class);
        var sender = new SpringMailAlertEmailSender(mailSender, "alerts@finalert.local");
        var now = Instant.parse("2026-10-29T12:00:00Z");
        var event = new AlertTriggeredMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user@example.com",
                "USD-BRL",
                new BigDecimal("5.10"),
                "GTE",
                new BigDecimal("5.20"),
                now,
                now
        );

        sender.send(event);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(captor.getValue().getSubject()).contains("USD-BRL");
        assertThat(captor.getValue().getText()).contains("5.20");
    }
}
