package io.github.bowerbird.java.core.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionLexerTest {

    @Test
    void tokenize_singleIdentifier_producesIdentifierAndEof() {
        var tokens = new ExpressionLexer("DEBUG").tokenize();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.getFirst().type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.getFirst().value()).isEqualTo("DEBUG");
        assertThat(tokens.getLast().type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void tokenize_negation_producesNotAndIdentifier() {
        var tokens = new ExpressionLexer("!DEBUG").tokenize();
        assertThat(tokens).hasSize(3);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.NOT);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(1).value()).isEqualTo("DEBUG");
    }

    @Test
    void tokenize_binaryAnd_producesCorrectTokens() {
        var tokens = new ExpressionLexer("A && B").tokenize();
        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.AND);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.IDENTIFIER);
    }

    @Test
    void tokenize_binaryOr_producesCorrectTokens() {
        var tokens = new ExpressionLexer("A || B").tokenize();
        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.OR);
    }

    @Test
    void tokenize_parenthesized_producesCorrectTokens() {
        var tokens = new ExpressionLexer("(A || B) && !C").tokenize();
        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.OR, TokenType.IDENTIFIER,
                TokenType.RPAREN, TokenType.AND, TokenType.NOT, TokenType.IDENTIFIER, TokenType.EOF
        );
    }

    @Test
    void tokenize_noSpaces_producesCorrectTokens() {
        var tokens = new ExpressionLexer("A&&B").tokenize();
        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(0).value()).isEqualTo("A");
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.AND);
        assertThat(tokens.get(2).value()).isEqualTo("B");
    }

    @Test
    void tokenize_extraWhitespace_producesCorrectTokens() {
        var tokens = new ExpressionLexer("  A  &&  B  ").tokenize();
        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(0).value()).isEqualTo("A");
        assertThat(tokens.get(2).value()).isEqualTo("B");
    }

    @Test
    void tokenize_underscoreInIdentifier_recognized() {
        var tokens = new ExpressionLexer("FEATURE_X_2").tokenize();
        assertThat(tokens.getFirst().value()).isEqualTo("FEATURE_X_2");
    }

    @Test
    void tokenize_emptyString_producesEofOnly() {
        var tokens = new ExpressionLexer("").tokenize();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void tokenize_invalidCharacter_throwsException() {
        assertThatThrownBy(() -> new ExpressionLexer("A & B").tokenize())
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("expected '&&'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A @ B", "A # B", "A $ B"})
    void tokenize_variousInvalidCharacters_throwsException(String input) {
        assertThatThrownBy(() -> new ExpressionLexer(input).tokenize())
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void tokenize_loneAmpersand_throwsException() {
        assertThatThrownBy(() -> new ExpressionLexer("&").tokenize())
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void tokenize_lonePipe_throwsException() {
        assertThatThrownBy(() -> new ExpressionLexer("|").tokenize())
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void tokenize_positionsAreCorrect() {
        var tokens = new ExpressionLexer("A && !B").tokenize();
        assertThat(tokens.get(0).position()).isEqualTo(0);  // A
        assertThat(tokens.get(1).position()).isEqualTo(2);  // &&
        assertThat(tokens.get(2).position()).isEqualTo(5);  // !
        assertThat(tokens.get(3).position()).isEqualTo(6);  // B
    }
}
