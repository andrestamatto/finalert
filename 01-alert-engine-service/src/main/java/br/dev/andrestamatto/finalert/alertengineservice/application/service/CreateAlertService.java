package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.CreateAlertEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.CreateAlertEventPublisher;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreateAlertService implements CreateAlertUseCase {

    private final AlertRepository alertRepository;
    private final CreateAlertEventPublisher createAlertEventPublisher;


    public CreateAlertService(AlertRepository alertRepository, CreateAlertEventPublisher createAlertEventPublisher) {
        this.alertRepository = alertRepository;
        this.createAlertEventPublisher = createAlertEventPublisher;
    }

    @Override
    @Transactional
    public Alert execute(CreateAlertCommand command) {
        Alert savedAlert = alertRepository.save(
                this.createAlertFromCommand(command)
        );

        createAlertEventPublisher.publish(
                CreateAlertEvent.fromDomain(savedAlert)
        );

        return savedAlert;
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
