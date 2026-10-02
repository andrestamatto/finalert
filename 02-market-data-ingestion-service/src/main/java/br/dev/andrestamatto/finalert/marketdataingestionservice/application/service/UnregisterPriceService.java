package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.UnregisterUpdatedPriceUseCase;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.AwesomeClient;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.rabbitmq.config.RabbitMqPublisherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UnregisterPriceService implements UnregisterUpdatedPriceUseCase {

    private final Set<String> monitoredPrices =  ConcurrentHashMap.newKeySet();

    private final AwesomeClient awesomeClient;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqPublisherProperties rabbitMqPublisherProperties;

    public UnregisterPriceService(AwesomeClient awesomeClient, RabbitTemplate rabbitTemplate, RabbitMqPublisherProperties rabbitMqPublisherProperties) {
        this.awesomeClient = awesomeClient;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqPublisherProperties = rabbitMqPublisherProperties;
    }

    @Override
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
