package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.mapper;

import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.entity.MarketCatalogEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MarketCatalogEntityMapper {

    public MarketCatalogEntity toEntity(MarketCatalog catalog) {
        var entity = new MarketCatalogEntity();

        entity.setEventId(catalog.eventId());
        entity.setCatalog(new HashMap<>(catalog.pairs()));
        entity.setUpdatedAt(catalog.updatedAt());
        entity.setCatalogVersion(catalog.version());

        return entity;
    }

    public MarketCatalog toDomain(MarketCatalogEntity entity) {
        return new MarketCatalog(
                entity.getEventId(),
                Map.copyOf(entity.getCatalog()),
                entity.getUpdatedAt(),
                entity.getCatalogVersion()
        );
    }
}
