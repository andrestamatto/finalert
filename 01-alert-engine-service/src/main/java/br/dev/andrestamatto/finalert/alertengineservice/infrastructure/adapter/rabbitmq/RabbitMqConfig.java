package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers.RabbitMqConsumerProperties;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers.RabbitMqPublisherProperties;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RabbitMqConfig {

    @Bean
    TopicExchange alertEventsExchange(RabbitMqPublisherProperties properties) {
        return new TopicExchange(properties.alertMonitoringRequested().exchangeName(), true, false);
    }

    @Bean
    TopicExchange marketEventsExchange(RabbitMqConsumerProperties properties) {
        return new TopicExchange(properties.marketCatalogUpdated().exchangeName(), true, false);
    }

    @Bean
    Queue marketCatalogUpdatedQueue(RabbitMqConsumerProperties properties) {
        return QueueBuilder.durable(
                properties.marketCatalogUpdated().queueName()
        ).build();
    }

    @Bean
    Binding marketCatalogUpdatedBinding(
            Queue marketCatalogUpdatedQueue,
            TopicExchange marketEventsExchange,
            RabbitMqConsumerProperties properties
    ) {
        return BindingBuilder
                .bind(marketCatalogUpdatedQueue)
                .to(marketEventsExchange)
                .with(properties.marketCatalogUpdated().routingKey());
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
