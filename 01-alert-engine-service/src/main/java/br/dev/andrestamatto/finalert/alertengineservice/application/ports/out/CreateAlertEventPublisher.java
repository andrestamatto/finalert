package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.CreateAlertEvent;

public interface CreateAlertEventPublisher {
    void publish (CreateAlertEvent createAlertEvent);
}
