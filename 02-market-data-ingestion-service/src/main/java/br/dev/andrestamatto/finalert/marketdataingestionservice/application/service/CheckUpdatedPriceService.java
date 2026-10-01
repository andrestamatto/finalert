package br.dev.andrestamatto.finalert.marketdataingestionservice.application.service;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.event.PriceUpdatedEvent;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.CheckUpdatedPriceUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class CheckUpdatedPriceService implements CheckUpdatedPriceUseCase {


    @Override
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
