package br.dev.andrestamatto.finalert.alertengineservice.web.request;

import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAlertRequest(
        @NotBlank @Email @Size(max = 320)
        String userEmail,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        BigDecimal targetPrice,
        @NotNull
        TriggerOperator triggerOperator
) {
}
