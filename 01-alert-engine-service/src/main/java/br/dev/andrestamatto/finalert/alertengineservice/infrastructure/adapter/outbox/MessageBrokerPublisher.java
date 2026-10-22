package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

public interface MessageBrokerPublisher {

    void publish(OutboxEventEntity event);
}
