package br.dev.andrestamatto.finalert.notificationservice.application.ports.out;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;

public interface AlertEmailSender {
    void send(AlertTriggeredMessage message);
}
