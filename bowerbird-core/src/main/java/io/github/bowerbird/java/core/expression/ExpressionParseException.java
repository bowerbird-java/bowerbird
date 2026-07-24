package io.github.bowerbird.java.core.expression;

import java.io.Serial;

/**
 * Thrown when a condition expression cannot be lexed or parsed.
 */
public final class ExpressionParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String expression;
    private final int position;

    /**
     * Creates an expression parse exception.
     *
     * @param message    the error description
     * @param expression the original expression string
     * @param position   the 0-based position where the error was detected
     */
    public ExpressionParseException(String message, String expression, int position) {
        super("%s at position %d in \"%s\"".formatted(message, position, expression));
        this.expression = expression;
        this.position = position;
    }

    /**
     * Returns the original expression that failed to parse.
     *
     * @return the expression string
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the 0-based position where the error was detected.
     *
     * @return the error position
     */
    public int getPosition() {
        return position;
    }
}
