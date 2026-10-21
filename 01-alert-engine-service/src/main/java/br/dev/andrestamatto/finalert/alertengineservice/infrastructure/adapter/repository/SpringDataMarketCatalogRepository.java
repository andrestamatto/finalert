package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.entity.MarketCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataMarketCatalogRepository extends JpaRepository<MarketCatalogEntity, UUID> {

    Optional<MarketCatalogEntity>
    findFirstByOrderByCatalogVersionDesc();

}
