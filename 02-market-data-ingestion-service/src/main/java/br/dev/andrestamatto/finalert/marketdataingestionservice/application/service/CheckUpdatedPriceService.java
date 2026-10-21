package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.MarketQuoteProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketPriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.CheckUpdatedPriceUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Service
public class CheckUpdatedPriceService implements CheckUpdatedPriceUseCase {

    private final MonitoredMarketPairsRepository marketPairsRepository;
    private final MarketQuoteProvider marketQuoteProvider;
    private final MarketPriceUpdatedEventPublisher marketPriceUpdatedEventPublisher;

    public CheckUpdatedPriceService(MonitoredMarketPairsRepository marketPairsRepository, MarketQuoteProvider marketQuoteProvider, MarketPriceUpdatedEventPublisher marketPriceUpdatedEventPublisher) {
        this.marketPairsRepository = marketPairsRepository;
        this.marketQuoteProvider = marketQuoteProvider;
        this.marketPriceUpdatedEventPublisher = marketPriceUpdatedEventPublisher;
    }

    @Override
    public void execute() {
        var monitoredPairs = marketPairsRepository.fetchMonitoredMarketPairs();

        if (monitoredPairs.isEmpty()) {
            log.info("Ignored polling due to empty monitored list.");
            return;
        }

        Set<String> monitoredSymbols = new TreeSet<>();
        monitoredPairs.stream()
                .map(MarketPriceAlert::symbol)
                .forEach(monitoredSymbols::add);

        var monitoredPairsStr = String.join(",", monitoredSymbols);

        var marketQuotes = marketQuoteProvider.fetchLastUpdatedMarketQuotes(monitoredPairsStr);

        if (marketQuotes.isEmpty()) {
            log.info("No updated prices found.");
            return;
        }

        log.info("Received {} updated prices.", marketQuotes.size());

        Set<String> returnedSymbols = new TreeSet<>();

        marketQuotes.forEach(quote -> {
            var symbol = String.join("-",quote.baseCurrency(),quote.quoteCurrency());
            returnedSymbols.add(symbol);
            marketPriceUpdatedEventPublisher.publish(
                    MarketPriceUpdatedEvent.fromDomain(symbol, quote)
            );
        });

        var missingSymbols = new TreeSet<>(monitoredSymbols);
        missingSymbols.removeAll(returnedSymbols);
        if (!missingSymbols.isEmpty()) {
            log.warn("AwesomeAPI did not return quotes for monitored symbols: {}", missingSymbols);
        }
    }
}
