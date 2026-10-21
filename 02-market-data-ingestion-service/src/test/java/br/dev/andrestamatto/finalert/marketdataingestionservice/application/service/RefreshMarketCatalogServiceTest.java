package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.MarketCatalogUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.AvailableMarketCatalogProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.publishers.MarketCatalogUpdatedEventPublisher;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshMarketCatalogServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Mock
    private AvailableMarketCatalogProvider catalogProvider;

    @Mock
    private MarketCatalogRepository repository;

    @Mock
    private MarketCatalogUpdatedEventPublisher publisher;

    private RefreshMarketCatalogService service;

    @BeforeEach
    void setUp() {
        service = new RefreshMarketCatalogService(
                catalogProvider,
                repository,
                publisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsVersionOneWhenThereIsNoCurrentCatalog() throws Exception {
        Set<MarketPair> pairs = Set.of(new MarketPair("USD-BRL", "Dólar/Real"));
        when(catalogProvider.fetchAvailableMarketPairsCatalog()).thenReturn(pairs);
        when(repository.findCurrent()).thenReturn(Optional.empty());

        service.execute();

        var catalogCaptor = ArgumentCaptor.forClass(MarketCatalog.class);
        var eventCaptor = ArgumentCaptor.forClass(MarketCatalogUpdatedEvent.class);
        verify(repository).save(catalogCaptor.capture());
        verify(publisher).publish(eventCaptor.capture());

        assertThat(catalogCaptor.getValue())
                .isEqualTo(new MarketCatalog(pairs, 1L, NOW));
        assertThat(eventCaptor.getValue().catalog())
                .containsExactlyEntriesOf(java.util.Map.of("USD-BRL", "Dólar/Real"));
        assertThat(eventCaptor.getValue().version()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().updatedAt()).isEqualTo(NOW);
    }

    @Test
    void ignoresAnUnchangedCatalog() throws Exception {
        Set<MarketPair> pairs = Set.of(new MarketPair("USD-BRL", "Dólar/Real"));
        when(catalogProvider.fetchAvailableMarketPairsCatalog()).thenReturn(pairs);
        when(repository.findCurrent()).thenReturn(Optional.of(
                new MarketCatalog(pairs, 7L, NOW.minusSeconds(60))
        ));

        service.execute();

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void incrementsTheVersionWhenTheCatalogChanges() throws Exception {
        Set<MarketPair> previousPairs = Set.of(new MarketPair("USD-BRL", "Dólar/Real"));
        Set<MarketPair> newPairs = Set.of(
                new MarketPair("USD-BRL", "Dólar/Real"),
                new MarketPair("EUR-BRL", "Euro/Real")
        );
        when(catalogProvider.fetchAvailableMarketPairsCatalog()).thenReturn(newPairs);
        when(repository.findCurrent()).thenReturn(Optional.of(
                new MarketCatalog(previousPairs, 7L, NOW.minusSeconds(60))
        ));

        service.execute();

        var catalogCaptor = ArgumentCaptor.forClass(MarketCatalog.class);
        verify(repository).save(catalogCaptor.capture());
        verify(publisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event.version() == 8L && event.updatedAt().equals(NOW)
        ));
        assertThat(catalogCaptor.getValue())
                .isEqualTo(new MarketCatalog(newPairs, 8L, NOW));
    }

    @Test
    void retriesWithTheLatestVersionAfterAConcurrentInsert() throws Exception {
        Set<MarketPair> newPairs = Set.of(new MarketPair("EUR-BRL", "Euro/Real"));
        when(catalogProvider.fetchAvailableMarketPairsCatalog()).thenReturn(newPairs);
        when(repository.findCurrent())
                .thenReturn(Optional.of(catalog(1L, "USD-BRL")))
                .thenReturn(Optional.of(catalog(2L, "BTC-BRL")));
        doThrow(new DuplicateKeyException("duplicate version"))
                .doNothing()
                .when(repository).save(org.mockito.ArgumentMatchers.any());

        service.execute();

        var captor = ArgumentCaptor.forClass(MarketCatalog.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(MarketCatalog::version)
                .containsExactly(2L, 3L);
        verify(publisher).publish(org.mockito.ArgumentMatchers.argThat(event -> event.version() == 3L));
    }

    @Test
    void doesNotPersistOrPublishWhenTheProviderFails() {
        var failure = new IllegalStateException("AwesomeAPI unavailable");
        when(catalogProvider.fetchAvailableMarketPairsCatalog()).thenThrow(failure);

        assertThatThrownBy(service::execute).isSameAs(failure);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private MarketCatalog catalog(long version, String symbol) {
        return new MarketCatalog(
                Set.of(new MarketPair(symbol, symbol)),
                version,
                NOW.minusSeconds(60)
        );
    }
}
