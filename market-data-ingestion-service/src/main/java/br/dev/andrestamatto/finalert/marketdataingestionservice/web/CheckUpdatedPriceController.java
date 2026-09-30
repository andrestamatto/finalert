package br.dev.andrestamatto.finalert.marketdataingestionservice.web;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.CheckUpdatedPriceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/prices")
public class CheckUpdatedPriceController {

    private final CheckUpdatedPriceUseCase checkUpdatedPriceUseCase;

    public CheckUpdatedPriceController(CheckUpdatedPriceUseCase checkUpdatedPriceUseCase) {
        this.checkUpdatedPriceUseCase = checkUpdatedPriceUseCase;
    }

    @PostMapping(value = "/register")
    public ResponseEntity<?> register(@RequestBody Set<String> priceSet){
        // TODO: throw issues to GlobalHandlerException
        var priceListStr = String.join(",",  priceSet);
        checkUpdatedPriceUseCase.registerPrice(priceListStr);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value="/unregister")
    public ResponseEntity<?> unregister(@RequestBody Set<String> priceSet){
        // TODO: throw issues to GlobalHandlerException
        var priceListStr = String.join(",",  priceSet);
        checkUpdatedPriceUseCase.unregisterPrice(priceListStr);
        return ResponseEntity.ok().build();
    }

}
