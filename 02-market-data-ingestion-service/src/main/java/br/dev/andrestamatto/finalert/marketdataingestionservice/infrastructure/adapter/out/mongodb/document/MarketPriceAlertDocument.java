package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.document;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPriceAlert;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.TriggerOperator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "monitored_alerts")
public record MarketPriceAlertDocument(
        @Id UUID alertId,
        String symbol,
        BigDecimal targetValue,
        TriggerOperator triggerOperator,
        Instant registeredAt
) {
    public static MarketPriceAlertDocument fromDomain(MarketPriceAlert alert) {
        return new MarketPriceAlertDocument(
                alert.alertId(),
                alert.symbol(),
                alert.targetValue(),
                alert.triggerOperator(),
                alert.registeredAt()
        );
    }

    public MarketPriceAlert toDomain() {
        return new MarketPriceAlert(
                alertId,
                symbol,
                targetValue,
                triggerOperator,
                registeredAt
        );
    }
}
