package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RegisterMonitoredMarketPriceUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import org.springframework.stereotype.Service;

@Service
public class RegisterMonitoredMarketPriceService implements RegisterMonitoredMarketPriceUseCase {

    private final MonitoredMarketPairsRepository repository;

    public RegisterMonitoredMarketPriceService(MonitoredMarketPairsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(MarketPriceAlert marketPriceAlert) {
        repository.register(marketPriceAlert);
    }
}
