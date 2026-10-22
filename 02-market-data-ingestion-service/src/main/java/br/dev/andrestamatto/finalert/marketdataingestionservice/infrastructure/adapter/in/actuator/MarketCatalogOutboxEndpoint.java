package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.in.actuator;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.model.outbox.MarketCatalogOutboxSummary;
import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.MaintainMarketCatalogOutboxUseCase;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Endpoint(id = "marketcatalogoutbox")
public class MarketCatalogOutboxEndpoint {

    private final MaintainMarketCatalogOutboxUseCase useCase;

    public MarketCatalogOutboxEndpoint(MaintainMarketCatalogOutboxUseCase useCase) {
        this.useCase = useCase;
    }

    @ReadOperation
    public MarketCatalogOutboxSummary summary() {
        return useCase.summarize();
    }

    @WriteOperation
    public RedriveResult redrive(String eventId) {
        var id = UUID.fromString(eventId);
        return new RedriveResult(id, useCase.resubmitFailed(id));
    }

    public record RedriveResult(UUID eventId, boolean resubmitted) {
    }
}
