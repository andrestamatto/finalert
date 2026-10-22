package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(name="tb_market_catalog")
public class MarketCatalogEntity {

    @Id
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> catalog;

    @Column(nullable=false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long catalogVersion;

}
