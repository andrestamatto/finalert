package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertTriggeredEvent;

public interface AlertTriggeredEventEnqueuer {
    void enqueue(AlertTriggeredEvent event);
}
