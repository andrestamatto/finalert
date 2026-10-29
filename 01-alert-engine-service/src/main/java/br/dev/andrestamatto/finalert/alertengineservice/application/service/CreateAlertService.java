package br.dev.andrestamatto.finalert.alertengineservice.application.service;

import br.dev.andrestamatto.finalert.alertengineservice.application.event.AlertMonitoringRequestedEvent;
import br.dev.andrestamatto.finalert.alertengineservice.application.exceptions.IdempotencyConflictException;
import br.dev.andrestamatto.finalert.alertengineservice.application.exceptions.MarketCatalogDoesNotContainsSymbolException;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.commands.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.CreateAlertUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.AlertRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.CreateAlertRequestRepository;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.messaging.AlertMonitoringRequestedEventEnqueuer;
import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertId;
import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CreateAlertService implements CreateAlertUseCase {

    private final AlertRepository alertRepository;
    private final MarketCatalogRepository marketCatalogRepository;
    private final AlertMonitoringRequestedEventEnqueuer alertMonitoringRequestedEventEnqueuer;
    private final CreateAlertRequestRepository createAlertRequestRepository;

    public CreateAlertService(
            AlertRepository alertRepository, MarketCatalogRepository marketCatalogRepository,
            AlertMonitoringRequestedEventEnqueuer alertMonitoringRequestedEventEnqueuer,
            CreateAlertRequestRepository createAlertRequestRepository
    ) {
        this.alertRepository = alertRepository;
        this.marketCatalogRepository = marketCatalogRepository;
        this.alertMonitoringRequestedEventEnqueuer = alertMonitoringRequestedEventEnqueuer;
        this.createAlertRequestRepository = createAlertRequestRepository;
    }

    @Override
    @Transactional
    public Alert execute(CreateAlertCommand command) {
        String requestHash = fingerprint(command);

        if ( !this.doesSymbolExist(command.symbol()) ) {
            throw new MarketCatalogDoesNotContainsSymbolException(command.symbol());
        }

        boolean acquired = createAlertRequestRepository.tryAcquire(
                command.idempotencyKey(),
                requestHash,
                Instant.now()
        );

        if (!acquired) {
            return replayExistingRequest(command.idempotencyKey(), requestHash);
        }

        Alert savedAlert = alertRepository.save(
                this.createAlertFromCommand(command)
        );

        alertMonitoringRequestedEventEnqueuer.enqueue(
                AlertMonitoringRequestedEvent.fromDomain(savedAlert)
        );

        createAlertRequestRepository.complete(
                command.idempotencyKey(),
                savedAlert.alertId(),
                Instant.now()
        );

        return savedAlert;
    }

    private Alert replayExistingRequest(String idempotencyKey, String requestHash) {
        var existingRequest = createAlertRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency request disappeared after a uniqueness conflict"
                ));

        if (!existingRequest.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }

        AlertId alertId = existingRequest.alertId();
        if (alertId == null) {
            throw new IllegalStateException("Idempotency request is incomplete: " + idempotencyKey);
        }

        return alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalStateException(
                        "Alert associated with idempotency key was not found: " + idempotencyKey
                ));
    }

    private String fingerprint(CreateAlertCommand command) {
        String canonicalRequest = lengthPrefixed(command.userEmail())
                + lengthPrefixed(command.symbol())
                + lengthPrefixed(command.targetPrice().stripTrailingZeros().toPlainString())
                + lengthPrefixed(command.triggerOperator().name());

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String lengthPrefixed(String value) {
        return value.length() + ":" + value;
    }

    private boolean doesSymbolExist(String symbol) {
        return marketCatalogRepository.containsPair(symbol);
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
