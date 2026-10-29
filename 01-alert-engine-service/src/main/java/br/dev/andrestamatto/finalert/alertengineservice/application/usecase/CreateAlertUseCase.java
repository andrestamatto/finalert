package br.dev.andrestamatto.finalert.alertengineservice.application.usecase;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.commands.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;

public interface CreateAlertUseCase {
    Alert execute(CreateAlertCommand command);
}
