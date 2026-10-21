package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertMonitoringRequestedEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertMonitoringRequestedEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers.RabbitMqPublisherProperties;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxAlertMonitoringRequestedEventEnqueuer implements AlertMonitoringRequestedEventEnqueuer {

    private static final String EVENT_TYPE = "alert.monitoring.requested";

    private final SpringDataOutboxEventRepository outboxRepository;
    private final RabbitMqPublisherProperties publisherProperties;
    private final ObjectMapper objectMapper;

    public OutboxAlertMonitoringRequestedEventEnqueuer(
            SpringDataOutboxEventRepository outboxRepository,
            RabbitMqPublisherProperties publisherProperties,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.publisherProperties = publisherProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(AlertMonitoringRequestedEvent event) {
        try {
            var destination = publisherProperties.alertMonitoringRequested();
            outboxRepository.save(new OutboxEventEntity(
                    UUID.fromString(event.eventId()),
                    UUID.fromString(event.alertId()),
                    EVENT_TYPE,
                    destination.exchangeName(),
                    destination.routingKey(),
                    objectMapper.writeValueAsString(event),
                    Instant.now()
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize alert-monitoring-requested event", exception);
        }
    }
}
