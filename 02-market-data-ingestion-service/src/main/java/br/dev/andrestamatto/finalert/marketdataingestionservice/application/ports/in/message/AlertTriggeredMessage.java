package br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.in.message;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AlertTriggeredMessage(
        @NotBlank String eventId,
        @NotBlank String alertId
) {
    public UUID alertUuid() {
        return UUID.fromString(alertId);
    }
}
