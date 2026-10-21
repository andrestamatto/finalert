package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketCatalogUpdatedServiceTest {

    @Test
    void ignoresAnEventThatWasAlreadyProcessed() {
        var repository = mock(MarketCatalogRepository.class);
        var service = new MarketCatalogUpdatedService(repository);
        var catalog = catalog();
        when(repository.saveIfAbsent(catalog)).thenReturn(false);

        service.save(catalog);

        verify(repository).saveIfAbsent(catalog);
    }

    @Test
    void savesAnEventThatWasNotProcessedYet() {
        var repository = mock(MarketCatalogRepository.class);
        var service = new MarketCatalogUpdatedService(repository);
        var catalog = catalog();
        when(repository.saveIfAbsent(catalog)).thenReturn(true);

        service.save(catalog);

        verify(repository).saveIfAbsent(catalog);
    }

    private MarketCatalog catalog() {
        return new MarketCatalog(
                UUID.randomUUID(),
                Map.of("USD-BRL", "Dolar/Real"),
                Instant.parse("2026-10-21T12:00:00Z"),
                1L
        );
    }
}
