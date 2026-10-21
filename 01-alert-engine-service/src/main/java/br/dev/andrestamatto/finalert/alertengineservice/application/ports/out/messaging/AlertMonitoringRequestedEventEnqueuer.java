package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertMonitoringRequestedEvent;

public interface AlertMonitoringRequestedEventEnqueuer {
    void enqueue(AlertMonitoringRequestedEvent event);
}
