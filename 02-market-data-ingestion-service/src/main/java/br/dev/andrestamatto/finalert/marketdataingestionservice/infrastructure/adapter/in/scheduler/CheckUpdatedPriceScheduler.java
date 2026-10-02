package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.in.scheduler;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.usecase.CheckUpdatedPriceUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckUpdatedPriceScheduler {

    private final CheckUpdatedPriceUseCase checkUpdatedPriceUseCase;


    public CheckUpdatedPriceScheduler(CheckUpdatedPriceUseCase checkUpdatedPriceUseCase) {
        this.checkUpdatedPriceUseCase = checkUpdatedPriceUseCase;
    }

    @Scheduled(fixedRateString = "PT30S")
    public void execute() {
        checkUpdatedPriceUseCase.execute();
    }
}
