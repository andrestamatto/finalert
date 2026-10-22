package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatchScheduler {

    private final OutboxDispatchService dispatchService;
    private final OutboxProperties properties;

    public OutboxDispatchScheduler(OutboxDispatchService dispatchService, OutboxProperties properties) {
        this.dispatchService = dispatchService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${finalert.outbox.worker-fixed-delay:1s}")
    public void dispatchPendingEvents() {
        for (int index = 0; index < properties.batchSize(); index++) {
            var result = dispatchService.dispatchNext();
            if (result != OutboxDispatchService.DispatchResult.PROCESSED) {
                return;
            }
        }
    }
}
