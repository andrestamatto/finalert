package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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

@Configuration
public class RabbitMQConfig implements RabbitListenerConfigurer {

    private final RabbitMqPublisherProperties rabbitMqPublisherProperties;
    private final RabbitMqConsumersProperties rabbitMqConsumersProperties;
    private final LocalValidatorFactoryBean validator;

    public RabbitMQConfig(
            RabbitMqPublisherProperties rabbitMqPublisherProperties,
            RabbitMqConsumersProperties rabbitMqConsumersProperties,
            LocalValidatorFactoryBean validator
    ) {
        this.rabbitMqPublisherProperties = rabbitMqPublisherProperties;
        this.rabbitMqConsumersProperties = rabbitMqConsumersProperties;
        this.validator = validator;
    }

    @Override
    public void configureRabbitListeners(
            RabbitListenerEndpointRegistrar registrar
    ) {
        registrar.setValidator(validator);
    }

    @Bean
    public TopicExchange marketPriceUpdatedEventsExchange() {
        return new TopicExchange(
                rabbitMqPublisherProperties.marketPriceUpdated().exchangeName(),
                true,
                false
        );
    }

    @Bean
    public TopicExchange marketCatalogUpdatedEventsExchange() {
        return new TopicExchange(
                rabbitMqPublisherProperties.marketCatalogUpdated().exchangeName(),
                true,
                false
        );
    }

    @Bean
    public TopicExchange alertMonitoringRequestedEventsExchange() {
        return new TopicExchange(
                rabbitMqConsumersProperties
                        .marketPriceAlert()
                        .exchangeName(),
                true,
                false
        );
    }

    @Bean
    public Queue marketPriceAlertQueue() {
        return QueueBuilder.durable(
                rabbitMqConsumersProperties
                        .marketPriceAlert()
                        .queueName()
        ).build();
    }

    @Bean
    public Binding marketPriceAlertBinding(
            @Qualifier("marketPriceAlertQueue") Queue marketPriceAlertQueue,
            @Qualifier("alertMonitoringRequestedEventsExchange") TopicExchange alertMonitoringRequestedEventsExchange
    ) {
        return BindingBuilder
                .bind(marketPriceAlertQueue)
                .to(alertMonitoringRequestedEventsExchange)
                .with(
                        rabbitMqConsumersProperties
                                .marketPriceAlert()
                                .routingKey()
                );
    }


    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

}
