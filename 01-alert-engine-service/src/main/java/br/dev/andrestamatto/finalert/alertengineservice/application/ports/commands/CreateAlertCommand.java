package br.dev.andrestamatto.finalert.alertengineservice.application.ports.commands;

import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;

import java.math.BigDecimal;

public record CreateAlertCommand(
        String idempotencyKey,
        String userEmail,
        String symbol,
        BigDecimal targetPrice,
        TriggerOperator triggerOperator
) {}
