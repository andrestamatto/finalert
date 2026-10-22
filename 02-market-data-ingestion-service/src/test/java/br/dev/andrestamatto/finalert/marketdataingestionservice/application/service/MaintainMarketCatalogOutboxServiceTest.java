package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.config.MarketCatalogOutboxProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.repository.MarketCatalogOutboxRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintainMarketCatalogOutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void purgesPublishedEventsOlderThanTheRetentionPeriod() {
        var repository = mock(MarketCatalogOutboxRepository.class);
        var service = service(repository);
        when(repository.deletePublishedBefore(NOW.minus(Duration.ofDays(7))))
                .thenReturn(4L);

        assertThat(service.purgePublished()).isEqualTo(4L);
        verify(repository).deletePublishedBefore(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void resubmitsAFailedEventWithoutChangingItsIdentity() {
        var repository = mock(MarketCatalogOutboxRepository.class);
        var service = service(repository);
        var eventId = UUID.randomUUID();
        when(repository.resubmitFailed(eventId, NOW)).thenReturn(true);

        assertThat(service.resubmitFailed(eventId)).isTrue();
        verify(repository).resubmitFailed(eventId, NOW);
    }

    private MaintainMarketCatalogOutboxService service(
            MarketCatalogOutboxRepository repository
    ) {
        var properties = new MarketCatalogOutboxProperties(
                3,
                10,
                Duration.ofSeconds(5),
                2.0,
                Duration.ofMinutes(2),
                Duration.ofDays(7)
        );
        return new MaintainMarketCatalogOutboxService(
                repository,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
