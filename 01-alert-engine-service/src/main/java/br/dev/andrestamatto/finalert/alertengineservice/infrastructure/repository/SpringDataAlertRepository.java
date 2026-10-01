package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository;

import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAlertRepository extends JpaRepository<AlertEntity, UUID> {

    List<AlertEntity> findBySymbolAndStatus(
            String symbol,
            AlertStatus status
    );

}
