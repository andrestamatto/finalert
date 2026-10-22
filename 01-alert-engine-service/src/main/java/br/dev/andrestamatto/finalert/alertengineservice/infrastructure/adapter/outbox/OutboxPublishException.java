package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox;

public class OutboxPublishException extends RuntimeException {

    public OutboxPublishException(String message) {
        super(message);
    }

    public OutboxPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
