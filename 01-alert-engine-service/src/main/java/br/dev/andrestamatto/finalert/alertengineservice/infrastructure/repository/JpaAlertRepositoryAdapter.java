package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository.mapper.AlertEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaAlertRepositoryAdapter implements AlertRepository {

    private final SpringDataAlertRepository jpaRepository;
    private final AlertEntityMapper alertMapper;

    public JpaAlertRepositoryAdapter(SpringDataAlertRepository jpaRepository, AlertEntityMapper alertMapper) {
        this.jpaRepository = jpaRepository;
        this.alertMapper = alertMapper;
    }

    @Override
    public Alert save(Alert alert) {
        var entity = alertMapper.toEntity(alert);
        var savedEntity = jpaRepository.save(entity);

        return alertMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Alert> findById(AlertId alertId) {
        return jpaRepository
                .findById(alertId.value())
                .map(alertMapper::toDomain);
    }

    @Override
    public List<Alert> findPendingBySymbol(String symbol) {
        return jpaRepository
                .findBySymbolAndStatus(symbol, AlertStatus.PENDING)
                .stream()
                .map(alertMapper::toDomain)
                .toList();
    }
}
