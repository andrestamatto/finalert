package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
public class OutboxDispatchService {

    public enum DispatchResult {
        PROCESSED,
        NO_EVENT,
        CIRCUIT_OPEN
    }

    private static final int MAX_ERROR_LENGTH = 4000;

    private final SpringDataOutboxEventRepository outboxRepository;
    private final MessageBrokerPublisher messageBrokerPublisher;
    private final OutboxRetryBackoff retryBackoff;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxDispatchService(
            SpringDataOutboxEventRepository outboxRepository,
            MessageBrokerPublisher messageBrokerPublisher,
            OutboxRetryBackoff retryBackoff,
            OutboxProperties properties,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.messageBrokerPublisher = messageBrokerPublisher;
        this.retryBackoff = retryBackoff;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DispatchResult dispatchNext() {
        var event = outboxRepository.lockNextDue(clock.instant()).orElse(null);
        if (event == null) {
            return DispatchResult.NO_EVENT;
        }
        try {
            messageBrokerPublisher.publish(event);
            event.markPublished(clock.instant());
            log.info("Outbox event published: eventId={}, type={}", event.getEventId(), event.getEventType());
        } catch (CallNotPermittedException exception) {
            log.debug("RabbitMQ circuit breaker is open; outbox dispatch paused");
            return DispatchResult.CIRCUIT_OPEN;
        } catch (RuntimeException exception) {
            registerFailure(event, exception);
        }

        return DispatchResult.PROCESSED;
    }

    private void registerFailure(OutboxEventEntity event, RuntimeException exception) {
        int failedAttempt = event.getAttemptCount() + 1;
        String error = abbreviate(exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage());

        if (failedAttempt >= properties.retry().maxAttempts()) {
            event.markFailed(failedAttempt, error);
            log.error(
                    "Outbox event permanently failed after {} attempts: eventId={}, error={}",
                    failedAttempt,
                    event.getEventId(),
                    error,
                    exception
            );
            return;
        }

        var delay = retryBackoff.delayForAttempt(failedAttempt);
        event.scheduleRetry(failedAttempt, clock.instant().plus(delay), error);
        log.warn(
                "Outbox publication failed; retry scheduled: eventId={}, attempt={}, delay={}, error={}",
                event.getEventId(),
                failedAttempt,
                delay,
                error
        );
    }

    private String abbreviate(String message) {
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
