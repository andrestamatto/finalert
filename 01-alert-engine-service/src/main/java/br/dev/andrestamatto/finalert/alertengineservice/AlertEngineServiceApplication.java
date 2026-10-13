package br.dev.andrestamatto.finalert.alertengineservice;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.publishers.rabbitmq.RabbitMqPublisherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RabbitMqPublisherProperties.class)
public class AlertEngineServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlertEngineServiceApplication.class, args);
	}

}
