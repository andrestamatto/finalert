package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.response;

public record AwesomeApiPriceCatalogResponse(
        String pair,
        String description
) {
}
