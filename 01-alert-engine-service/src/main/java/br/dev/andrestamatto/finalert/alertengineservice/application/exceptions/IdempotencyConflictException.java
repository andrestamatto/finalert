package br.dev.andrestamatto.finalert.alertengineservice.application.exceptions;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key was already used with a different request: " + idempotencyKey);
    }
}
