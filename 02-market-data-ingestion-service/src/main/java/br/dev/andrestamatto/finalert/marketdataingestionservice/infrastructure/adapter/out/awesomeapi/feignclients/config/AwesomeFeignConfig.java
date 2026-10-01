package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwesomeFeignConfig {

    private final AwesomeApiProperties awesomeApiProperties;

    public AwesomeFeignConfig(AwesomeApiProperties awesomeApiProperties) {
        this.awesomeApiProperties = awesomeApiProperties;
    }

    @Bean
    public RequestInterceptor awesomeApiRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("x-api-key", this.awesomeApiProperties.apiKey());
        };
    }
}
