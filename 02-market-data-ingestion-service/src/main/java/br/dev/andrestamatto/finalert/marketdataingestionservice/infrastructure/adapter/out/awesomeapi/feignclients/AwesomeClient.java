package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients;

import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.config.AwesomeFeignConfig;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.response.AwesomeApiPriceCatalogResponse;
import br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.response.AwesomeApiQuoteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(
        name="awesome-client",
        url="${marketdataingestionservice.awesomeapi.url}",
        configuration = AwesomeFeignConfig.class
)
public interface AwesomeClient {

    @GetMapping("/last/{exchangeRequest}")
    Map<String, AwesomeApiQuoteResponse> last(
            @PathVariable("exchangeRequest") String exchangeRequest
    );

    @GetMapping("/daily/{currency}/{days}")
    List<AwesomeApiQuoteResponse> daily(
            @PathVariable("currency") String currency,
            @PathVariable("days") int days,
            @RequestParam(name = "start_date", required = false) String startDate,
            @RequestParam(name = "end_date", required = false) String endDate
    );

    @GetMapping("/available")
    Set<AwesomeApiPriceCatalogResponse> available();

}
