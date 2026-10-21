package br.dev.andrestamatto.finalert.marketdataingestionservice.application.exception;

public class EventPublicationException extends RuntimeException {

    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
