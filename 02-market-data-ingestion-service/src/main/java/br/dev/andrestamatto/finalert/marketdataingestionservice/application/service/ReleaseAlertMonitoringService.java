package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.ReleaseAlertMonitoringUseCase;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReleaseAlertMonitoringService implements ReleaseAlertMonitoringUseCase {

    private final MonitoredMarketPairsRepository repository;

    public ReleaseAlertMonitoringService(MonitoredMarketPairsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID alertId) {
        repository.unregister(alertId);
    }
}
