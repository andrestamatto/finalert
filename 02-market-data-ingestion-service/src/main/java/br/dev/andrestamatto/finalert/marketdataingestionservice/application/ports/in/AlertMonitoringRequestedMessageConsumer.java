package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.message.AlertMonitoringRequestedMessage;

public interface AlertMonitoringRequestedMessageConsumer {
    void consume(AlertMonitoringRequestedMessage event);
}
