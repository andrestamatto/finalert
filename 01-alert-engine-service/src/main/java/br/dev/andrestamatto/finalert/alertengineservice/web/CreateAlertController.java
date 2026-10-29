package br.dev.andrestamatto.finalert.alertengineservice.web;

import br.dev.andrestamatto.finalert.alertengineservice.application.usecase.CreateAlertUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.web.request.CreateAlertRequest;
import br.dev.andrestamatto.finalert.alertengineservice.web.response.CreateAlertResponse;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/alerts")
public class CreateAlertController {

    private final CreateAlertUseCase createAlertUseCase;
    private final CreateAlertWebMapper createAlertWebMapper;

    public CreateAlertController(CreateAlertUseCase createAlertUseCase, CreateAlertWebMapper createAlertWebMapper) {
        this.createAlertUseCase = createAlertUseCase;
        this.createAlertWebMapper = createAlertWebMapper;
    }

    @PostMapping(value = "/{symbol}")
    public ResponseEntity<CreateAlertResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Nonnull @PathVariable @NotBlank @Size(max = 32) String symbol,
            @Nonnull @Valid @RequestBody CreateAlertRequest createAlertRequest) {

        var command = createAlertWebMapper.toCommand(idempotencyKey, symbol, createAlertRequest);
        var alert = createAlertUseCase.execute(command);

        return ResponseEntity
                .created(location("/{symbol}"))
                .body(
                    createAlertWebMapper.toResponse(alert)
                );
    }

    private URI location(String path) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(path)
                .build()
                .toUri();
    }

}
