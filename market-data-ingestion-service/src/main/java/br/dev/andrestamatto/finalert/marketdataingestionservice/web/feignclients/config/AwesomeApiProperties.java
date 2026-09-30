package br.dev.andrestamatto.finalert.marketdataingestionservice.web.feignclients.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("marketdataingestionservice.awesomeapi")
public record AwesomeApiProperties(
    String apiKey,
    String url
) {}
