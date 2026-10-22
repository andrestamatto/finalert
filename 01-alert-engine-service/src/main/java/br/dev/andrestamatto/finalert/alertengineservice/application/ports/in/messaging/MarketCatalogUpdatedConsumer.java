package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging;

public interface MarketCatalogUpdatedConsumer {
    void consume(MarketCatalogUpdatedMessage event);
}
