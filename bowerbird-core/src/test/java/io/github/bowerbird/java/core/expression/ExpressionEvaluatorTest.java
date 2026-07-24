package io.github.bowerbird.java.core.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionEvaluatorTest {

    @ParameterizedTest
    @CsvSource({
            // expression,            flags,   expected
            "DEBUG,                    DEBUG,   true",
            "DEBUG,                    '',      false",
            "!DEBUG,                   DEBUG,   false",
            "!DEBUG,                   '',      true",
            "A && B,                   A:B,     true",
            "A && B,                   A,       false",
            "A || B,                   A,       true",
            "A || B,                   '',      false",
            "(A || B) && !C,           B,       true",
            "(A || B) && !C,           B:C,     false",
            "A && B && C,              A:B:C,   true",
            "A && B && C,              A:B,     false",
            "A || B || C,              C,       true",
            "!!DEBUG,                  DEBUG,   true",
            "!!DEBUG,                  '',      false",
            "!(A && B),                A,       true",
            "!(A && B),                A:B,     false",
    })
    void evaluate_variousExpressions_returnsExpected(String expression, String flagsCsv, boolean expected) {
        var flags = flagsCsv.isEmpty() ? Set.<String>of() : Set.of(flagsCsv.split(":"));
        var evaluator = new ExpressionEvaluator(flags);
        assertThat(evaluator.evaluate(expression)).isEqualTo(expected);
    }

    @Test
    void evaluate_malformedExpression_throwsException() {
        var evaluator = new ExpressionEvaluator(Set.of());
        assertThatThrownBy(() -> evaluator.evaluate("&&"))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void evaluate_unmatchedParen_throwsException() {
        var evaluator = new ExpressionEvaluator(Set.of());
        assertThatThrownBy(() -> evaluator.evaluate("(A && B"))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void evaluate_trailingOperator_throwsException() {
        var evaluator = new ExpressionEvaluator(Set.of());
        assertThatThrownBy(() -> evaluator.evaluate("A &&"))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void evaluate_unmatchedRightParen_throwsException() {
        var evaluator = new ExpressionEvaluator(Set.of());
        assertThatThrownBy(() -> evaluator.evaluate("A && B)"))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void referencedFlags_extractsAllIdentifiers() {
        var flags = ExpressionEvaluator.referencedFlags("(A || B) && !C");
        assertThat(flags).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void getActiveFlags_returnsUnmodifiableSet() {
        var evaluator = new ExpressionEvaluator(Set.of("X", "Y"));
        assertThat(evaluator.getActiveFlags()).containsExactlyInAnyOrder("X", "Y");
        assertThatThrownBy(() -> evaluator.getActiveFlags().add("Z"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
