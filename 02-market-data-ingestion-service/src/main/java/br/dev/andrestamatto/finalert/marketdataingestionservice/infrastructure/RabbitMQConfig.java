package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    private final RabbitMQProperties rabbitMQProperties;

    public RabbitMQConfig(RabbitMQProperties rabbitMQProperties) {
        this.rabbitMQProperties = rabbitMQProperties;
    }

    @Bean
    public Queue priceUpdatedQueue(){
        return new Queue(rabbitMQProperties.queueName(),true);
    }

    @Bean
    public TopicExchange marketEventsExchange(){
        return new TopicExchange(rabbitMQProperties.exchangeName(),true,false);
    }

    @Bean
    public Binding priceUpdatedBinding(Queue priceUpdatedQueue, TopicExchange marketEventsExchange){
        return BindingBuilder
                .bind(priceUpdatedQueue)
                .to(marketEventsExchange)
                .with(rabbitMQProperties.routingKey());
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

}
