package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
