package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi;

import br.dev.andrestamatto.finalert.marketdataingestionservice.application.ports.out.providers.MarketQuoteProvider;
import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPrice;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.AwesomeClient;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.response.AwesomeApiQuoteResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AwesomeApiMarketQuoteProvider implements MarketQuoteProvider {

    private final AwesomeClient awesomeClient;

    public AwesomeApiMarketQuoteProvider(AwesomeClient awesomeClient) {
        this.awesomeClient = awesomeClient;
    }

    @Override
    public Set<MarketPrice> fetchLastUpdatedMarketQuotes(String quotes) {
        return Optional.ofNullable(awesomeClient.last(quotes))
                .orElseGet(Collections::emptyMap)
                .values().stream()
                .map(AwesomeApiQuoteResponse::toDomain)
                .collect(Collectors.toSet());
    }

}
