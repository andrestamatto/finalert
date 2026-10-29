package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository;

import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.entity.AlertEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface SpringDataAlertRepository extends JpaRepository<AlertEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AlertEntity> findBySymbolAndStatus(
            String symbol,
            AlertStatus status
    );

}
