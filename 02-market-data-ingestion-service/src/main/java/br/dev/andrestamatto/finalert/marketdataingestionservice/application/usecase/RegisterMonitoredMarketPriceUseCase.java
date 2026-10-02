package br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;

public interface RegisterMonitoredMarketPriceUseCase {
    void execute(MarketPriceAlert marketPriceAlert);
}
