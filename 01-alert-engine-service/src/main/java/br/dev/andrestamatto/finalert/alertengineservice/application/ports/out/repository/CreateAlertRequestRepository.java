package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository;

import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;

import java.time.Instant;
import java.util.Optional;

public interface CreateAlertRequestRepository {

    boolean tryAcquire(String idempotencyKey, String requestHash, Instant createdAt);

    Optional<CreateAlertRequestState> findByIdempotencyKey(String idempotencyKey);

    void complete(String idempotencyKey, AlertId alertId, Instant completedAt);

    record CreateAlertRequestState(
            String idempotencyKey,
            String requestHash,
            AlertId alertId
    ) {
    }
}
