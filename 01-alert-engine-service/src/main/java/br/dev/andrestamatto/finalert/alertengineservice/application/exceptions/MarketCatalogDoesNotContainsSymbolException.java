package br.dev.andrestamatto.finalert.alertengineservice.application.exceptions;

public class MarketCatalogDoesNotContainsSymbolException extends RuntimeException {

    public MarketCatalogDoesNotContainsSymbolException(String symbol) {
        super("Symbol " + symbol + " doesn't exist in the market catalog.");
    }
}
