package br.dev.andrestamatto.finalert.marketdataingestionservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.data.mongodb.auto-index-creation=false",
		"spring.rabbitmq.listener.simple.auto-startup=false"
})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
