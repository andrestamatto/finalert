package br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase;

import java.util.UUID;

public interface ReleaseAlertMonitoringUseCase {
    void execute(UUID alertId);
}
