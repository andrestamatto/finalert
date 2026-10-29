package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers.RabbitMqConsumerProperties;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers.RabbitMqPublisherProperties;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

@Configuration
public class RabbitMqConfig implements RabbitListenerConfigurer {

    private final LocalValidatorFactoryBean validator;

    public RabbitMqConfig(LocalValidatorFactoryBean validator) {
        this.validator = validator;
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setValidator(validator);
    }

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
        return durableQueueWithDeadLetter(properties.marketCatalogUpdated().queueName());
    }

    @Bean
    Queue marketPriceUpdatedQueue(RabbitMqConsumerProperties properties) {
        return durableQueueWithDeadLetter(properties.marketPriceUpdated().queueName());
    }

    @Bean
    Queue marketCatalogUpdatedDeadLetterQueue(RabbitMqConsumerProperties properties) {
        return QueueBuilder.durable(properties.marketCatalogUpdated().queueName() + ".dlq").build();
    }

    @Bean
    Queue marketPriceUpdatedDeadLetterQueue(RabbitMqConsumerProperties properties) {
        return QueueBuilder.durable(properties.marketPriceUpdated().queueName() + ".dlq").build();
    }

    @Bean
    Binding marketCatalogUpdatedBinding(
            @Qualifier("marketCatalogUpdatedQueue") Queue marketCatalogUpdatedQueue,
            @Qualifier("marketEventsExchange") TopicExchange marketEventsExchange,
            RabbitMqConsumerProperties properties
    ) {
        return BindingBuilder
                .bind(marketCatalogUpdatedQueue)
                .to(marketEventsExchange)
                .with(properties.marketCatalogUpdated().routingKey());
    }

    @Bean
    Binding marketPriceUpdatedBinding(
            @Qualifier("marketPriceUpdatedQueue") Queue marketPriceUpdatedQueue,
            @Qualifier("marketEventsExchange") TopicExchange marketEventsExchange,
            RabbitMqConsumerProperties properties
    ) {
        return BindingBuilder
                .bind(marketPriceUpdatedQueue)
                .to(marketEventsExchange)
                .with(properties.marketPriceUpdated().routingKey());
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    private Queue durableQueueWithDeadLetter(String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange("")
                .deadLetterRoutingKey(queueName + ".dlq")
                .build();
    }
}
