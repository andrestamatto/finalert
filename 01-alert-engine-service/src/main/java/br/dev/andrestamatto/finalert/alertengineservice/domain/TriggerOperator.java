package br.dev.andrestamatto.finalert.alertengineservice.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.IntPredicate;

public enum TriggerOperator {
    GTE(comparison -> comparison >= 0),
    LTE(comparison -> comparison <= 0),
    GT(comparison -> comparison > 0),
    LT(comparison -> comparison < 0),
    EQ(comparison -> comparison == 0),
    NEQ(comparison -> comparison != 0);

    private final IntPredicate operation;

    TriggerOperator(IntPredicate operation) {
        this.operation = operation;
    }

    public boolean isSatisfiedBy(BigDecimal currentPrice, BigDecimal targetPrice) {
        Objects.requireNonNull(currentPrice, "currentPrice must not be null");
        Objects.requireNonNull(targetPrice, "targetPrice must not be null");

        return operation.test(currentPrice.compareTo(targetPrice));
    }
}
