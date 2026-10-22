package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.idempotency;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.CreateAlertRequestRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaCreateAlertRequestRepositoryAdapter implements CreateAlertRequestRepository {

    private final SpringDataCreateAlertRequestRepository repository;

    public JpaCreateAlertRequestRepositoryAdapter(SpringDataCreateAlertRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean tryAcquire(String idempotencyKey, String requestHash, Instant createdAt) {
        return repository.tryAcquire(idempotencyKey, requestHash, createdAt) == 1;
    }

    @Override
    public Optional<CreateAlertRequestState> findByIdempotencyKey(String idempotencyKey) {
        return repository.findById(idempotencyKey)
                .map(entity -> new CreateAlertRequestState(
                        entity.getIdempotencyKey(),
                        entity.getRequestHash(),
                        entity.getAlertId() == null ? null : new AlertId(entity.getAlertId())
                ));
    }

    @Override
    public void complete(String idempotencyKey, AlertId alertId, Instant completedAt) {
        int updated = repository.complete(idempotencyKey, alertId.value(), completedAt);
        if (updated != 1) {
            throw new IllegalStateException("Could not complete idempotency request: " + idempotencyKey);
        }
    }
}
