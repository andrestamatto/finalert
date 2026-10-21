package br.dev.andrestamatto.finalert.alertengineservice.application.ports.in.messaging;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MarketCatalogUpdatedMessage(
        @NotNull UUID eventId,
        @NotEmpty Map<String, String> catalog,
        @NotNull Instant updatedAt,
        @PositiveOrZero long version
) {
    public static MarketCatalog toDomain(@Valid MarketCatalogUpdatedMessage marketCatalogUpdatedMessage) {
        return new MarketCatalog(
                marketCatalogUpdatedMessage.eventId(),
                marketCatalogUpdatedMessage.catalog(),
                marketCatalogUpdatedMessage.updatedAt(),
                marketCatalogUpdatedMessage.version()
        );
    }

}
