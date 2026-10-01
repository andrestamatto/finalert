package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.repository.entity;

import br.dev.andrestamatto.finalert.alertengineservice.domain.AlertStatus;
import br.dev.andrestamatto.finalert.alertengineservice.domain.TriggerOperator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(name="tb_alerts")
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(name="target_price", nullable = false, precision = 24, scale = 12)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(name="trigger_operator", nullable = false, length = 3)
    private TriggerOperator triggerOperator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name="triggered_at")
    private Instant triggeredAt;

    @Column(name="cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

}
