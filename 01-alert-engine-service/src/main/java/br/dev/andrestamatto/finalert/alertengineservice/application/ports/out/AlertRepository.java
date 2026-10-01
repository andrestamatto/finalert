package br.dev.andrestamatto.finalert.alertengineservice.application.ports.out;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;

import java.util.List;
import java.util.Optional;

public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(AlertId alertId);

    List<Alert> findPendingBySymbol(String symbol);
}
