package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.PriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.MarketQuoteProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.PriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.CheckUpdatedPriceUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class CheckUpdatedPriceService implements CheckUpdatedPriceUseCase {

    private final MonitoredMarketPairsRepository marketPairsRepository;
    private final MarketQuoteProvider marketQuoteProvider;
    private final PriceUpdatedEventPublisher priceUpdatedEventPublisher;

    public CheckUpdatedPriceService(MonitoredMarketPairsRepository marketPairsRepository, MarketQuoteProvider marketQuoteProvider, PriceUpdatedEventPublisher priceUpdatedEventPublisher) {
        this.marketPairsRepository = marketPairsRepository;
        this.marketQuoteProvider = marketQuoteProvider;
        this.priceUpdatedEventPublisher = priceUpdatedEventPublisher;
    }

    @Override
    public void execute() {
        var monitoredPairs = marketPairsRepository.fetchMonitoredMarketPairs();

        if (monitoredPairs.isEmpty()) {
            log.info("Ignored polling due to empty monitored list.");
            return;
        }

        var monitoredPairsStr = String.join(",",
                monitoredPairs.stream()
                        .map(MarketPriceAlert::symbol)
                        .collect(Collectors.toSet())
        );

        var marketQuotes = marketQuoteProvider.fetchLastUpdatedMarketQuotes(monitoredPairsStr);

        if (marketQuotes.isEmpty()) {
            log.info("No updated prices found.");
            return;
        }

        log.info("Received {} updated prices.", marketQuotes.size());

        marketQuotes.forEach(quote -> {
            var symbol = String.join("-",quote.baseCurrency(),quote.quoteCurrency());
            priceUpdatedEventPublisher.publish(
                    PriceUpdatedEvent.fromDomain(symbol, quote)
            );
        });

    }
}
