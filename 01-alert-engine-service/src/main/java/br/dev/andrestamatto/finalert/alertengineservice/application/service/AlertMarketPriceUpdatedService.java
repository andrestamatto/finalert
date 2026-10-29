package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertTriggeredEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertTriggeredEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.AlertMarketPriceUpdatedUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketPrice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AlertMarketPriceUpdatedService implements AlertMarketPriceUpdatedUseCase {

    private final AlertRepository repository;
    private final AlertTriggeredEventEnqueuer eventEnqueuer;
    private final Clock clock;

    public AlertMarketPriceUpdatedService(
            AlertRepository repository,
            AlertTriggeredEventEnqueuer eventEnqueuer,
            Clock clock
    ) {
        this.repository = repository;
        this.eventEnqueuer = eventEnqueuer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void propagate(MarketPrice marketPrice) {
        repository.findPendingBySymbol(marketPrice.symbol()).stream()
                .filter(alert -> alert.isSatisfiedBy(marketPrice))
                .forEach(alert -> {
                    var occurredAt = clock.instant();
                    var triggeredAlert = repository.save(alert.markAsTriggered(occurredAt));

                    eventEnqueuer.enqueue(AlertTriggeredEvent.fromDomain(
                            triggeredAlert,
                            marketPrice,
                            occurredAt
                    ));
                });
    }

}
