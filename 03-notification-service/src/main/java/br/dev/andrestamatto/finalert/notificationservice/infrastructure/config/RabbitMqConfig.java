package br.dev.andrestamatto.finalert.notificationservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
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
    TopicExchange alertEventsExchange(@Value("${finalert.rabbitmq.alert-triggered.exchange-name}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    Queue alertTriggeredQueue(@Value("${finalert.rabbitmq.alert-triggered.queue-name}") String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange("")
                .deadLetterRoutingKey(name + ".dlq")
                .build();
    }

    @Bean
    Queue alertTriggeredDeadLetterQueue(
            @Value("${finalert.rabbitmq.alert-triggered.queue-name}") String name
    ) {
        return QueueBuilder.durable(name + ".dlq").build();
    }

    @Bean
    Binding alertTriggeredBinding(
            @Qualifier("alertTriggeredQueue") Queue alertTriggeredQueue,
            @Qualifier("alertEventsExchange") TopicExchange alertEventsExchange,
            @Value("${finalert.rabbitmq.alert-triggered.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(alertTriggeredQueue).to(alertEventsExchange).with(routingKey);
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
