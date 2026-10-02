package br.dev.andrestamatto.finalert.marketdataingestionservice;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqConsumersProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.config.AwesomeApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({
		AwesomeApiProperties.class,
		RabbitMqPublisherProperties.class,
		RabbitMqConsumersProperties.class
})
public class MarketDataIngestionApplication {
	public static void main(String[] args) {
		SpringApplication.run(MarketDataIngestionApplication.class, args);
	}
}
