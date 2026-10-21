package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb.outbox.document;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}