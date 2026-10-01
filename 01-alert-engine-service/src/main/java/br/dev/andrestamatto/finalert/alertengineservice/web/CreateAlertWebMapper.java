package br.dev.andrestamatto.finalert.alertengineservice.web;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertCommand;
import br.dev.andrestamatto.finalert.alertengineservice.domain.Alert;
import br.dev.andrestamatto.finalert.alertengineservice.web.request.CreateAlertRequest;
import br.dev.andrestamatto.finalert.alertengineservice.web.response.CreateAlertResponse;
import org.springframework.stereotype.Component;

@Component
public class CreateAlertWebMapper {
    public CreateAlertCommand toCommand(String symbol, CreateAlertRequest request) {
        return new CreateAlertCommand(
                request.userEmail(),
                symbol,
                request.targetPrice(),
                request.triggerOperator()
        );
    }

    public CreateAlertResponse toResponse(Alert alert) {
        return new CreateAlertResponse(
                alert.alertId().value(),
                alert.symbol(),
                alert.price(),
                alert.userEmail(),
                alert.createdAt().toString(),
                alert.updatedAt().toString(),
                alert.cancelledAt().toString()
        );
    }
}
