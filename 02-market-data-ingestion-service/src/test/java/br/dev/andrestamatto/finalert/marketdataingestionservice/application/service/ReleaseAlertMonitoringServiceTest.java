package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReleaseAlertMonitoringServiceTest {

    @Test
    void unregistersTheTriggeredAlert() {
        var repository = mock(MonitoredMarketPairsRepository.class);
        var service = new ReleaseAlertMonitoringService(repository);
        var alertId = UUID.randomUUID();

        service.execute(alertId);

        verify(repository).unregister(alertId);
    }
}
