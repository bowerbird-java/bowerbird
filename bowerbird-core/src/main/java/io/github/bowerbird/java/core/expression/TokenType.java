package io.github.bowerbird.java.core.expression;

/**
 * Token types produced by the {@link ExpressionLexer}.
 */
public enum TokenType {
    IDENTIFIER,
    AND,
    OR,
    NOT,
    LPAREN,
    RPAREN,
    EOF
}
