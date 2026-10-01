package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config;

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

    private final RabbitMqPublisherProperties rabbitMqPublisherProperties;

    public RabbitMQConfig(RabbitMqPublisherProperties rabbitMqPublisherProperties) {
        this.rabbitMqPublisherProperties = rabbitMqPublisherProperties;
    }

    @Bean
    public TopicExchange priceUpdatedEventsExchange(){
        return new TopicExchange(
                rabbitMqPublisherProperties.priceUpdated().exchangeName(),true,false
        );
    }

    @Bean
    public Binding priceUpdatedBinding(Queue priceUpdatedQueue, TopicExchange priceUpdatedEventsExchange){
        return BindingBuilder
                .bind(priceUpdatedQueue)
                .to(priceUpdatedEventsExchange)
                .with(rabbitMqPublisherProperties.priceUpdated().routingKey());
    }

    @Bean
    public TopicExchange marketCatalogUpdatedEventsExchange(){
        return new TopicExchange(
                rabbitMqPublisherProperties.marketCatalogUpdated().exchangeName(),true,false
        );
    }

    @Bean
    public Binding marketCatalogUpdatedBinding(Queue priceUpdatedQueue, TopicExchange marketCatalogUpdatedEventsExchange){
        return BindingBuilder
                .bind(priceUpdatedQueue)
                .to(marketCatalogUpdatedEventsExchange)
                .with(rabbitMqPublisherProperties.marketCatalogUpdated().routingKey());
    }


    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

}
