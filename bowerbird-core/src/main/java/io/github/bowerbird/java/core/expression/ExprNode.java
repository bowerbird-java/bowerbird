package io.github.bowerbird.java.core.expression;

/**
 * AST node for a boolean condition expression.
 *
 * <p>This sealed hierarchy models the expression grammar:</p>
 * <pre>
 * expression  ::= orExpr
 * orExpr      ::= andExpr ( '||' andExpr )*
 * andExpr     ::= unaryExpr ( '&&' unaryExpr )*
 * unaryExpr   ::= '!' unaryExpr | primary
 * primary     ::= IDENTIFIER | '(' expression ')'
 * </pre>
 */
public sealed interface ExprNode permits ExprNode.And, ExprNode.Or, ExprNode.Not, ExprNode.Identifier {

    /**
     * Logical AND of two sub-expressions.
     *
     * @param left  the left operand
     * @param right the right operand
     */
    record And(ExprNode left, ExprNode right) implements ExprNode {}

    /**
     * Logical OR of two sub-expressions.
     *
     * @param left  the left operand
     * @param right the right operand
     */
    record Or(ExprNode left, ExprNode right) implements ExprNode {}

    /**
     * Logical NOT of a sub-expression.
     *
     * @param operand the operand to negate
     */
    record Not(ExprNode operand) implements ExprNode {}

    /**
     * A flag identifier that evaluates to {@code true} if present in the active flag set.
     *
     * @param name the flag name
     */
    record Identifier(String name) implements ExprNode {}
}
