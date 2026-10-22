package br.dev.andrestamatto.finalert.marketdataingestionservice.domain;

import java.util.Objects;

public record MarketPair(
        String name,
        String description
) {
}
