package br.dev.andrestamatto.finalert.alertengineservice.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MarketCatalog(
        UUID eventId,
        Map<String, String> pairs,
        Instant updatedAt,
        long version
) {
}