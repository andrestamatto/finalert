package br.dev.andrestamatto.finalert.alertengineservice;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.consumers.RabbitMqConsumerProperties;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.rabbitmq.publishers.RabbitMqPublisherProperties;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RabbitMqPublisherProperties.class, OutboxProperties.class, RabbitMqConsumerProperties.class})
public class AlertEngineServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlertEngineServiceApplication.class, args);
	}

}
