package br.dev.andrestamatto.finalert.marketdataingestionservice.application;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.PriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.RabbitMQProperties;
import br.dev.andrestamatto.finalert.marketdataingestionservice.web.feignclients.AwesomeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CheckUpdatedPriceUseCase {

    private final Set<String> monitoredPrices =  ConcurrentHashMap.newKeySet();

    private final AwesomeClient awesomeClient;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    public CheckUpdatedPriceUseCase(AwesomeClient awesomeClient, RabbitTemplate rabbitTemplate, RabbitMQProperties rabbitMQProperties) {
        this.awesomeClient = awesomeClient;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQProperties = rabbitMQProperties;
    }

    public void registerPrice(String price){
        if (price == null || price.isBlank()) {
            // TODO: throw issues to GlobalHandlerException
            return;
        }

        var normalizedPrice = price.trim().toUpperCase();

        if (monitoredPrices.add(normalizedPrice)) {
            log.info("Price registered for monitoring: {}", normalizedPrice);
        }
    }

    @Scheduled(fixedRateString = "PT30S")
    public void execute() {
        log.info("Starting price polling");

        if (monitoredPrices.isEmpty()) {
            log.info("Ignored polling due to empty monitored list.");
            return;
        }

        var monitoredExchangeRates = String.join(",", this.monitoredPrices);

        var updatedPrices = Optional
                .ofNullable(awesomeClient.last(monitoredExchangeRates))
                .orElse(Collections.emptyMap());

        if (updatedPrices.isEmpty()) {
            log.info("No updated prices found.");
            return;
        }

        log.info("Received {} updated prices.", updatedPrices.size());

        updatedPrices.forEach( (symbol, quote) -> {

            var event = PriceUpdatedEvent.fromDomain(symbol, quote.toDomain());

            try {
                rabbitTemplate.convertAndSend(
                        rabbitMQProperties.exchangeName(),
                        rabbitMQProperties.routingKey(),
                        event
                );

                log.info("Price event submitted for publication: eventId={}", event.eventId());

            } catch (AmqpException exception){
                log.warn(
                        "Failed to publish price event: eventId={}, symbol={}",
                        event.eventId(),
                        symbol,
                        exception
                );
            }

        });

    }
}
