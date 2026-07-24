package io.github.bowerbird.java.core.expression;

/**
 * A single token produced by the {@link ExpressionLexer}.
 *
 * @param type     the token type
 * @param value    the lexeme text (meaningful for {@link TokenType#IDENTIFIER}, empty for operators)
 * @param position the 0-based position in the input where this token starts
 */
public record Token(TokenType type, String value, int position) {

    /**
     * Creates a token with an empty value at the given position.
     *
     * @param type     the token type
     * @param position the position
     * @return the token
     */
    public static Token of(TokenType type, int position) {
        return new Token(type, "", position);
    }
}
