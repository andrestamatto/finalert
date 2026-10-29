package br.dev.andrestamatto.finalert.alertengineservice.application.usecase;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketPrice;

public interface AlertMarketPriceUpdatedUseCase {
    void propagate(MarketPrice marketPrice);
}
