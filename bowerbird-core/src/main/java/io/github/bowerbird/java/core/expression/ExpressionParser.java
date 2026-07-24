package io.github.bowerbird.java.core.expression;

import java.util.List;

/**
 * Recursive-descent parser that builds an {@link ExprNode} AST from a token list.
 *
 * <p>Implements the grammar with standard operator precedence:
 * {@code !} (highest) > {@code &&} > {@code ||} (lowest).</p>
 */
public final class ExpressionParser {

    private final List<Token> tokens;
    private final String expression;
    private int position;

    /**
     * Creates a parser for the given token list.
     *
     * @param tokens     the tokens produced by {@link ExpressionLexer}
     * @param expression the original expression string (for error messages)
     */
    public ExpressionParser(List<Token> tokens, String expression) {
        this.tokens = tokens;
        this.expression = expression;
        this.position = 0;
    }

    /**
     * Parses a condition expression string into an AST.
     *
     * @param expression the expression to parse
     * @return the root AST node
     * @throws ExpressionParseException if the expression is malformed
     */
    public static ExprNode parse(String expression) {
        var tokens = new ExpressionLexer(expression).tokenize();
        var parser = new ExpressionParser(tokens, expression);
        var result = parser.parseOr();
        parser.expect(TokenType.EOF, "unexpected token after expression");
        return result;
    }

    private ExprNode parseOr() {
        var left = parseAnd();
        while (check(TokenType.OR)) {
            advance();
            var right = parseAnd();
            left = new ExprNode.Or(left, right);
        }
        return left;
    }

    private ExprNode parseAnd() {
        var left = parseUnary();
        while (check(TokenType.AND)) {
            advance();
            var right = parseUnary();
            left = new ExprNode.And(left, right);
        }
        return left;
    }

    private ExprNode parseUnary() {
        if (check(TokenType.NOT)) {
            advance();
            var operand = parseUnary();
            return new ExprNode.Not(operand);
        }
        return parsePrimary();
    }

    private ExprNode parsePrimary() {
        if (check(TokenType.IDENTIFIER)) {
            var token = advance();
            return new ExprNode.Identifier(token.value());
        }
        if (check(TokenType.LPAREN)) {
            advance();
            var expr = parseOr();
            expect(TokenType.RPAREN, "expected closing ')'");
            return expr;
        }
        throw new ExpressionParseException("expected identifier or '('", expression, current().position());
    }

    private Token current() {
        return tokens.get(position);
    }

    private boolean check(TokenType type) {
        return current().type() == type;
    }

    private Token advance() {
        var token = current();
        position++;
        return token;
    }

    private void expect(TokenType type, String errorMessage) {
        if (!check(type)) {
            throw new ExpressionParseException(errorMessage, expression, current().position());
        }
        advance();
    }
}
