package br.dev.andrestamatto.finalert.alertengineservice.web.request;

import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;

import java.math.BigDecimal;

public record CreateAlertRequest(
        String userEmail,
        BigDecimal targetPrice,
        TriggerOperator triggerOperator
) {
}
