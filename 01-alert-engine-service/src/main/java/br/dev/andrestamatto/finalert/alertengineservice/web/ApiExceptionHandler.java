package br.dev.andrestamatto.finalert.alertengineservice.web;

import br.dev.andrestamatto.finalert.alertengineservice.application.exceptions.IdempotencyConflictException;
import br.dev.andrestamatto.finalert.alertengineservice.application.exceptions.MarketCatalogDoesNotContainsSymbolException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Idempotency key conflict");
        return problem;
    }

    @ExceptionHandler(MarketCatalogDoesNotContainsSymbolException.class)
    ProblemDetail handleMarketCatalogDoesNotContainsSymbolException(MarketCatalogDoesNotContainsSymbolException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problem.setTitle("Market catalog doesn't contain symbol");
        return problem;
    }
}
