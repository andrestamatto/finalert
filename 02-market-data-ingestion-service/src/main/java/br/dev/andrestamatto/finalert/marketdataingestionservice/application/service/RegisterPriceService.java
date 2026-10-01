package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.RegisterUpdatedPriceUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.AwesomeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RegisterPriceService implements RegisterUpdatedPriceUseCase {

    private final Set<String> monitoredPrices =  ConcurrentHashMap.newKeySet();

    private final AwesomeClient awesomeClient;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitMqPublisherProperties;

    public RegisterPriceService(AwesomeClient awesomeClient, RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitMqPublisherProperties) {
        this.awesomeClient = awesomeClient;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqPublisherProperties = rabbitMqPublisherProperties;
    }

    @Override
    public void registerPrice(String prices){
        if (prices == null || prices.isBlank()) {
            // TODO: throw issues to GlobalHandlerException
            return;
        }

        var normalizedPrice = Set.of(prices.trim().toUpperCase().split(","));

        if (monitoredPrices.addAll(normalizedPrice)) {
            log.info("Price registered for monitoring: {}", normalizedPrice);
        }
    }

    public void unregisterPrice(String prices) {
        if (prices == null || prices.isBlank()) {
            // TODO: throw issues to GlobalHandlerException
            return;
        }
        log.info("The following prices will be unregestered: {}", prices);

        var pricesToBeUnregistered = Set.of(prices.trim().toUpperCase().split(","));
        monitoredPrices.removeAll(pricesToBeUnregistered);

        log.info("Remaining prices registered for monitoring: {}", String.join(",", monitoredPrices));

    }



}
