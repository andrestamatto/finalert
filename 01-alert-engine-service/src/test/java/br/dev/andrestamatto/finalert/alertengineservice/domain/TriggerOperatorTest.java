package br.dev.andrestamatto.finalert.alertengineservice.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerOperatorTest {

    @ParameterizedTest
    @MethodSource("priceComparisons")
    void evaluatesPriceComparison(
        TriggerOperator operator,
        String currentPrice,
        String targetPrice,
        boolean expected
    ) {
        assertThat(operator.isSatisfiedBy(
            new BigDecimal(currentPrice),
            new BigDecimal(targetPrice)
        )).isEqualTo(expected);
    }

    private static Stream<Arguments> priceComparisons() {
        return Stream.of(
            Arguments.of(TriggerOperator.GTE, "10.01", "10.00", true),
            Arguments.of(TriggerOperator.GTE, "10.00", "10.00", true),
            Arguments.of(TriggerOperator.GTE, "9.99", "10.00", false),
            Arguments.of(TriggerOperator.LTE, "9.99", "10.00", true),
            Arguments.of(TriggerOperator.LTE, "10.0", "10.00", true),
            Arguments.of(TriggerOperator.LTE, "10.01", "10.00", false),
            Arguments.of(TriggerOperator.GT, "10.01", "10.00", true),
            Arguments.of(TriggerOperator.GT, "10.00", "10.00", false),
            Arguments.of(TriggerOperator.LT, "9.99", "10.00", true),
            Arguments.of(TriggerOperator.LT, "10.00", "10.00", false),
            Arguments.of(TriggerOperator.EQ, "10.0", "10.00", true),
            Arguments.of(TriggerOperator.EQ, "10.01", "10.00", false),
            Arguments.of(TriggerOperator.NEQ, "10.01", "10.00", true),
            Arguments.of(TriggerOperator.NEQ, "10.0", "10.00", false)
        );
    }
}
