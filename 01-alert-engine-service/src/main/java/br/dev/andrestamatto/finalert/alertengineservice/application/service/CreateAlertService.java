package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAlertService implements CreateAlertUseCase {

    private final AlertRepository alertRepository;

    public CreateAlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public Alert execute(CreateAlertCommand command) {
        return alertRepository.save(
                this.createAlertFromCommand(command)
        );
    }

    public Alert createAlertFromCommand(CreateAlertCommand command) {
        var now = Instant.now();
        return new Alert(
                new AlertId(UUID.randomUUID()),
                command.userEmail(),
                command.symbol(),
                command.targetPrice(),
                command.triggerOperator(),
                AlertStatus.PENDING,
                now,
                now,
                null,
                null,
                null
        );
    }
}
