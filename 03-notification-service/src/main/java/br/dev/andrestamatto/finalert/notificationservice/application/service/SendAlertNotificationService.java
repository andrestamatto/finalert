package br.dev.andrestamatto.finalert.notificationservice.application.service;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.AlertEmailSender;
import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.ProcessedNotificationRepository;
import br.dev.andrestamatto.finalert.notificationservice.application.usecase.SendAlertNotificationUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
public class SendAlertNotificationService implements SendAlertNotificationUseCase {

    private final ProcessedNotificationRepository processedRepository;
    private final AlertEmailSender emailSender;
    private final Clock clock;

    public SendAlertNotificationService(
            ProcessedNotificationRepository processedRepository,
            AlertEmailSender emailSender,
            Clock clock
    ) {
        this.processedRepository = processedRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(AlertTriggeredMessage message) {
        if (!processedRepository.tryClaim(message.eventUuid(), message.alertUuid(), clock.instant())) {
            log.info("Ignoring duplicate AlertTriggeredEvent: eventId={}", message.eventId());
            return;
        }

        emailSender.send(message);
        log.info("Alert notification sent: eventId={}, alertId={}", message.eventId(), message.alertId());
    }
}
