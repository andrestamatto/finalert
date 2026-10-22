package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.AlertMonitoringRequestedEvent;

public interface AlertMonitoringRequestedEventConsumer {
    void consume(AlertMonitoringRequestedEvent event);
}
