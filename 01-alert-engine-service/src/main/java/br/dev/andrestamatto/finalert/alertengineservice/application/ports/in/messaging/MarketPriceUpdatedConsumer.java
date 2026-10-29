package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging;

public interface MarketPriceUpdatedConsumer {
    void consume(MarketPriceUpdatedMessage message);
}
