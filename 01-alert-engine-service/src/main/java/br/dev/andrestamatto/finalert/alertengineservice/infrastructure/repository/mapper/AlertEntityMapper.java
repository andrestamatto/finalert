package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository.mapper;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository.entity.AlertEntity;
import org.springframework.stereotype.Component;

@Component
public class AlertEntityMapper {
    public Alert toDomain(AlertEntity entity) {
        return new Alert(
                new AlertId(entity.getId()),
                entity.getUserEmail(),
                entity.getSymbol(),
                entity.getTargetPrice(),
                entity.getTriggerOperator(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getTriggeredAt(),
                entity.getCancelledAt(),
                entity.getVersion()
        );
    }

    public AlertEntity toEntity(Alert alert) {
        var entity = new AlertEntity();

        entity.setId(alert.alertId().value());
        entity.setUserEmail(alert.userEmail());
        entity.setSymbol(alert.symbol());
        entity.setTargetPrice(alert.price());
        entity.setTriggerOperator(alert.triggerOperator());
        entity.setStatus(alert.status());
        entity.setCreatedAt(alert.createdAt());
        entity.setUpdatedAt(alert.updatedAt());
        entity.setTriggeredAt(alert.triggeredAt());
        entity.setCancelledAt(alert.cancelledAt());
        entity.setVersion(alert.version());

        return entity;
    }
}
