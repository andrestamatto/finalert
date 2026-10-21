package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitMqMessageBrokerPublisher implements MessageBrokerPublisher {

    static final String CIRCUIT_BREAKER_NAME = "rabbitPublisher";

    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties properties;

    public RabbitMqMessageBrokerPublisher(RabbitTemplate rabbitTemplate, OutboxProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void publish(OutboxEventEntity event) {
        var messageProperties = new MessageProperties();
        messageProperties.setMessageId(event.getEventId().toString());
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setHeader("eventType", event.getEventType());

        var message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);
        var correlationData = new CorrelationData(event.getEventId().toString());

        rabbitTemplate.send(event.getExchangeName(), event.getRoutingKey(), message, correlationData);

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    properties.publisherConfirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            ReturnedMessage returned = correlationData.getReturned();

            if (returned != null) {
                throw new OutboxPublishException(
                        "Message was returned by RabbitMQ: " + returned.getReplyText()
                );
            }
            if (!confirm.ack()) {
                throw new OutboxPublishException("RabbitMQ negatively acknowledged the message: " + confirm.reason());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException("Interrupted while waiting for RabbitMQ confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new OutboxPublishException("RabbitMQ publisher confirmation failed", exception);
        }
    }
}
