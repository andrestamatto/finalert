package br.dev.andrestamatto.finalert.notificationservice.application.usecase;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;

public interface SendAlertNotificationUseCase {
    void execute(AlertTriggeredMessage message);
}
