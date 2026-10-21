package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketPriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.MarketQuoteProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketPriceUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MonitoredMarketPairsRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPrice;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.TriggerOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckUpdatedPriceServiceTest {

    @Mock
    private MonitoredMarketPairsRepository repository;

    @Mock
    private MarketQuoteProvider quoteProvider;

    @Mock
    private MarketPriceUpdatedEventPublisher publisher;

    private CheckUpdatedPriceService service;

    @BeforeEach
    void setUp() {
        service = new CheckUpdatedPriceService(repository, quoteProvider, publisher);
    }

    @Test
    void skipsPollingWhenThereAreNoMonitoredAlerts() {
        when(repository.fetchMonitoredMarketPairs()).thenReturn(Set.of());

        service.execute();

        verify(quoteProvider, never()).fetchLastUpdatedMarketQuotes(org.mockito.ArgumentMatchers.any());
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deduplicatesAndSortsSymbolsBeforeRequestingQuotes() {
        when(repository.fetchMonitoredMarketPairs()).thenReturn(Set.of(
                alert("USD-BRL"),
                alert("BTC-BRL"),
                alert("USD-BRL")
        ));
        when(quoteProvider.fetchLastUpdatedMarketQuotes("BTC-BRL,USD-BRL"))
                .thenReturn(Set.of(price("USD", "BRL", "5.25")));

        service.execute();

        verify(quoteProvider).fetchLastUpdatedMarketQuotes("BTC-BRL,USD-BRL");
        var eventCaptor = ArgumentCaptor.forClass(MarketPriceUpdatedEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().symbol()).isEqualTo("USD-BRL");
        assertThat(eventCaptor.getValue().currentPrice()).isEqualByComparingTo("5.25");
    }

    @Test
    void publishesOneEventForEachReturnedQuote() {
        when(repository.fetchMonitoredMarketPairs()).thenReturn(Set.of(
                alert("USD-BRL"),
                alert("EUR-BRL")
        ));
        when(quoteProvider.fetchLastUpdatedMarketQuotes("EUR-BRL,USD-BRL"))
                .thenReturn(Set.of(
                        price("USD", "BRL", "5.25"),
                        price("EUR", "BRL", "6.10")
                ));

        service.execute();

        var captor = ArgumentCaptor.forClass(MarketPriceUpdatedEvent.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(MarketPriceUpdatedEvent::symbol)
                .containsExactlyInAnyOrder("USD-BRL", "EUR-BRL");
    }

    private MarketPriceAlert alert(String symbol) {
        return new MarketPriceAlert(
                UUID.randomUUID(),
                symbol,
                BigDecimal.TEN,
                TriggerOperator.GTE,
                Instant.parse("2026-10-21T12:00:00Z")
        );
    }

    private MarketPrice price(String base, String quote, String bid) {
        var value = new BigDecimal(bid);
        var now = Instant.parse("2026-10-21T12:00:00Z");
        return new MarketPrice(
                base,
                quote,
                base + "/" + quote,
                value,
                value,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                value,
                value,
                now,
                now
        );
    }
}
