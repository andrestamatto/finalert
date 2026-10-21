package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.outbox;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    @Test
    void marksAnEventAsPublishedOnlyAfterThePublisherSucceeds() {
        var fixture = fixture(3);

        var result = fixture.service.dispatchNext();

        assertThat(result).isEqualTo(OutboxDispatchService.DispatchResult.PROCESSED);
        assertThat(fixture.event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(fixture.event.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void schedulesAPersistentRetryAfterAPublicationFailure() {
        var fixture = fixture(3);
        doThrow(new OutboxPublishException("broker unavailable"))
                .when(fixture.publisher).publish(fixture.event);

        fixture.service.dispatchNext();

        assertThat(fixture.event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(fixture.event.getAttemptCount()).isEqualTo(1);
        assertThat(fixture.event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(fixture.event.getLastError()).isEqualTo("broker unavailable");
    }

    @Test
    void movesAnEventToFailedAfterTheAttemptLimit() {
        var fixture = fixture(1);
        doThrow(new OutboxPublishException("unroutable"))
                .when(fixture.publisher).publish(fixture.event);

        fixture.service.dispatchNext();

        assertThat(fixture.event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(fixture.event.getAttemptCount()).isEqualTo(1);
        assertThat(fixture.event.getLastError()).isEqualTo("unroutable");
    }

    @Test
    void doesNotConsumeAnOutboxAttemptWhenTheCircuitIsOpen() {
        var fixture = fixture(3);
        doThrow(mock(CallNotPermittedException.class))
                .when(fixture.publisher).publish(fixture.event);

        var result = fixture.service.dispatchNext();

        assertThat(result).isEqualTo(OutboxDispatchService.DispatchResult.CIRCUIT_OPEN);
        assertThat(fixture.event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(fixture.event.getAttemptCount()).isZero();
        assertThat(fixture.event.getLastError()).isNull();
    }

    private Fixture fixture(int maxAttempts) {
        var repository = mock(SpringDataOutboxEventRepository.class);
        var publisher = mock(MessageBrokerPublisher.class);
        var backoff = mock(OutboxRetryBackoff.class);
        var properties = properties(maxAttempts);
        var event = event();
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);

        when(repository.lockNextDue(NOW)).thenReturn(Optional.of(event));
        when(backoff.delayForAttempt(1)).thenReturn(Duration.ofSeconds(2));

        var service = new OutboxDispatchService(
                repository,
                publisher,
                backoff,
                properties,
                clock
        );
        return new Fixture(service, publisher, event);
    }

    private OutboxProperties properties(int maxAttempts) {
        return new OutboxProperties(
                Duration.ofSeconds(1),
                10,
                Duration.ofSeconds(5),
                new OutboxProperties.Retry(
                        maxAttempts,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(1),
                        2,
                        0.2
                )
        );
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alert.monitoring.requested",
                "alert.events",
                "alert.monitoring.requested",
                "{}",
                NOW
        );
    }

    private record Fixture(
            OutboxDispatchService service,
            MessageBrokerPublisher publisher,
            OutboxEventEntity event
    ) {
    }
}
