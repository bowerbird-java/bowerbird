package io.github.bowerbird.java.core.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes a boolean condition expression into a list of {@link Token}s.
 *
 * <p>Recognized tokens: identifiers ({@code [A-Za-z_][A-Za-z0-9_]*}),
 * {@code &&}, {@code ||}, {@code !}, {@code (}, {@code )}, and EOF.</p>
 */
public final class ExpressionLexer {

    private final String input;
    private int position;

    /**
     * Creates a lexer for the given expression string.
     *
     * @param input the expression to tokenize
     */
    public ExpressionLexer(String input) {
        this.input = input;
        this.position = 0;
    }

    /**
     * Tokenizes the entire input and returns an immutable list of tokens,
     * always terminated by an {@link TokenType#EOF} token.
     *
     * @return the token list
     * @throws ExpressionParseException if an unexpected character is encountered
     */
    public List<Token> tokenize() {
        var tokens = new ArrayList<Token>();
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) {
                position++;
            } else if (c == '&') {
                expectNext('&', "expected '&&'");
                tokens.add(Token.of(TokenType.AND, position - 2));
            } else if (c == '|') {
                expectNext('|', "expected '||'");
                tokens.add(Token.of(TokenType.OR, position - 2));
            } else if (c == '!') {
                tokens.add(Token.of(TokenType.NOT, position));
                position++;
            } else if (c == '(') {
                tokens.add(Token.of(TokenType.LPAREN, position));
                position++;
            } else if (c == ')') {
                tokens.add(Token.of(TokenType.RPAREN, position));
                position++;
            } else if (isIdentifierStart(c)) {
                tokens.add(readIdentifier());
            } else {
                throw new ExpressionParseException("unexpected character '%c'".formatted(c), input, position);
            }
        }
        tokens.add(Token.of(TokenType.EOF, position));
        return List.copyOf(tokens);
    }

    private void expectNext(char expected, String errorMessage) {
        position++; // consume first character
        if (position >= input.length() || input.charAt(position) != expected) {
            throw new ExpressionParseException(errorMessage, input, position - 1);
        }
        position++; // consume second character
    }

    private Token readIdentifier() {
        int start = position;
        while (position < input.length() && isIdentifierPart(input.charAt(position))) {
            position++;
        }
        return new Token(TokenType.IDENTIFIER, input.substring(start, position), start);
    }

    private static boolean isIdentifierStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }
}
