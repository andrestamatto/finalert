package br.dev.andrestamatto.finalert.alertengineservice.web.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAlertResponse(
        UUID alertId,
        String symbol,
        BigDecimal targetPrice,
        String userEmail,
        String createdAt,
        String updatedAt,
        String cancelledAt
) {
}
