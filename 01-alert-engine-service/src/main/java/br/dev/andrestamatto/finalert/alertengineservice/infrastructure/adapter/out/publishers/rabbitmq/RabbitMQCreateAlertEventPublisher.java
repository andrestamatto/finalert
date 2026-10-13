package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.publishers.rabbitmq;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.CreateAlertEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.CreateAlertEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMQCreateAlertEventPublisher implements CreateAlertEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitMqPublisherProperties;

    public RabbitMQCreateAlertEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitMqPublisherProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqPublisherProperties = rabbitMqPublisherProperties;
    }

    @Override
    public void publish(CreateAlertEvent createAlertEvent) {
        log.info("Start publishing createAlertEvent.");

        try {
            rabbitTemplate.convertAndSend(
                    rabbitMqPublisherProperties.alertCreated().exchangeName(),
                    rabbitMqPublisherProperties.alertCreated().routingKey(),
                    createAlertEvent
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish price event: eventId={}",
                    createAlertEvent.eventId(),
                    e
            );
            throw e;
        }

        log.info("CreateAlert event submitted for publication: eventId={}", createAlertEvent.eventId());
    }
}
