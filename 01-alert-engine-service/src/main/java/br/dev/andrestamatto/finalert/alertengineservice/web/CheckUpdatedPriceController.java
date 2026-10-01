package br.dev.andrestamatto.finalert.alertengineservice.web;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.CreateAlertUseCase;
import br.dev.andrestamatto.finalert.alertengineservice.application.service.CreateAlertService;
import br.dev.andrestamatto.finalert.alertengineservice.web.request.CreateAlertRequest;
import br.dev.andrestamatto.finalert.alertengineservice.web.response.CreateAlertResponse;
import jakarta.annotation.Nonnull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/alerts")
public class CheckUpdatedPriceController {

    private final CreateAlertUseCase createAlertUseCase;
    private final CreateAlertWebMapper createAlertWebMapper;

    public CheckUpdatedPriceController(CreateAlertService createAlertUseCase, CreateAlertWebMapper createAlertWebMapper) {
        this.createAlertUseCase = createAlertUseCase;
        this.createAlertWebMapper = createAlertWebMapper;
    }

    @PostMapping(value = "/{symbol}")
    public ResponseEntity<CreateAlertResponse> register(
            @Nonnull @PathVariable String symbol,
            @Nonnull @RequestBody CreateAlertRequest createAlertRequest) {

        var command = createAlertWebMapper.toCommand(symbol, createAlertRequest);
        var alert = createAlertUseCase.execute(command);

        return ResponseEntity
                .created(location("/{symbol}"))
                .body(
                    createAlertWebMapper.toResponse(alert)
                );
    }

    @PostMapping(value = "/symbols")
    public ResponseEntity<?> registerAll(@RequestBody Set<String> symbols) {

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{symbol}")
    public ResponseEntity<?> unregister(@PathVariable String symbol){

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{symbol}")
    public ResponseEntity<?> unregisterAll(@RequestBody Set<String> symbols) {

        return ResponseEntity.ok().build();
    }

    private URI location(String path) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(path)
                .build()
                .toUri();
    }

}
