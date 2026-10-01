package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in;

import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;

public interface CreateAlertUseCase {
    Alert execute(CreateAlertCommand command);
}
