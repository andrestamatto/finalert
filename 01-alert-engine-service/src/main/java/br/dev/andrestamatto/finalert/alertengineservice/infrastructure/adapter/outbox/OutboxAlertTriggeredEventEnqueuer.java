package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertTriggeredEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertTriggeredEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers.RabbitMqPublisherProperties;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

@Component
public class OutboxAlertTriggeredEventEnqueuer implements AlertTriggeredEventEnqueuer {

    private static final String EVENT_TYPE = "alert.triggered";

    private final SpringDataOutboxEventRepository outboxRepository;
    private final RabbitMqPublisherProperties publisherProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxAlertTriggeredEventEnqueuer(
            SpringDataOutboxEventRepository outboxRepository,
            RabbitMqPublisherProperties publisherProperties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.publisherProperties = publisherProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void enqueue(AlertTriggeredEvent event) {
        try {
            var destination = publisherProperties.alertTriggered();
            outboxRepository.save(new OutboxEventEntity(
                    UUID.fromString(event.eventId()),
                    UUID.fromString(event.alertId()),
                    EVENT_TYPE,
                    destination.exchangeName(),
                    destination.routingKey(),
                    objectMapper.writeValueAsString(event),
                    clock.instant()
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize alert-triggered event", exception);
        }
    }
}
