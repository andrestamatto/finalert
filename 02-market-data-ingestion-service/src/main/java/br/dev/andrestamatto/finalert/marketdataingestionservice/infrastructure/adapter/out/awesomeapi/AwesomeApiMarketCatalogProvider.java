package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.AvailableMarketCatalogProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPair;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.AwesomeClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AwesomeApiMarketCatalogProvider implements AvailableMarketCatalogProvider {

    private final AwesomeClient awesomeClient;

    public AwesomeApiMarketCatalogProvider(AwesomeClient awesomeClient) {
        this.awesomeClient = awesomeClient;
    }

    @Override
    public Set<MarketPair> fetchAvailableMarketPairsCatalog() {
        return awesomeClient.available().stream().map(
                response -> new MarketPair(response.pair(), response.description())
        ).collect(Collectors.toSet());
    }
}
