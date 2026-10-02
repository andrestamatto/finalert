package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MonitoredMarketPricesEvent;
public interface MonitoredMarketPricesEventConsumer {
    void consume(MonitoredMarketPricesEvent monitoredMarketPricesEvent);
}
