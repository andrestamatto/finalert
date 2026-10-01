package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in;

import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;

import java.math.BigDecimal;

public record CreateAlertCommand(
        String userEmail,
        String symbol,
        BigDecimal targetPrice,
        TriggerOperator triggerOperator
) {}
