package io.github.bowerbird.java.core.expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates boolean condition expressions against a set of active feature flags.
 *
 * <p>This class is <em>immutable and thread-safe</em> after construction. The active flag
 * set is captured at creation time and shared safely across evaluation calls from multiple
 * threads.</p>
 */
public final class ExpressionEvaluator {

    private final Set<String> activeFlags;

    /**
     * Creates an evaluator with the given active flag set.
     *
     * @param activeFlags the set of active flags (defensively copied)
     */
    public ExpressionEvaluator(Set<String> activeFlags) {
        Objects.requireNonNull(activeFlags, "activeFlags must not be null");
        this.activeFlags = Collections.unmodifiableSet(new HashSet<>(activeFlags));
    }

    /**
     * Parses and evaluates a condition expression string.
     *
     * @param expression the boolean expression (e.g., {@code "FEATURE_A && !PRODUCTION"})
     * @return {@code true} if the expression evaluates to true against the active flags
     * @throws ExpressionParseException if the expression is malformed
     */
    public boolean evaluate(String expression) {
        Objects.requireNonNull(expression, "expression must not be null");
        var ast = ExpressionParser.parse(expression);
        return evaluate(ast);
    }

    /**
     * Evaluates a pre-parsed AST node.
     *
     * @param node the AST root
     * @return the evaluation result
     */
    public boolean evaluate(ExprNode node) {
        return switch (node) {
            case ExprNode.Identifier id -> activeFlags.contains(id.name());
            case ExprNode.Not not -> !evaluate(not.operand());
            case ExprNode.And and -> evaluate(and.left()) && evaluate(and.right());
            case ExprNode.Or or -> evaluate(or.left()) || evaluate(or.right());
        };
    }

    /**
     * Returns the set of flags referenced by the given expression.
     *
     * @param expression the expression to inspect
     * @return an unmodifiable set of flag names
     */
    public static Set<String> referencedFlags(String expression) {
        var ast = ExpressionParser.parse(expression);
        var flags = new HashSet<String>();
        collectFlags(ast, flags);
        return Collections.unmodifiableSet(flags);
    }

    private static void collectFlags(ExprNode node, Set<String> flags) {
        switch (node) {
            case ExprNode.Identifier id -> flags.add(id.name());
            case ExprNode.Not not -> collectFlags(not.operand(), flags);
            case ExprNode.And and -> { collectFlags(and.left(), flags); collectFlags(and.right(), flags); }
            case ExprNode.Or or -> { collectFlags(or.left(), flags); collectFlags(or.right(), flags); }
        }
    }

    /**
     * Returns an unmodifiable view of the active flag set.
     *
     * @return the active flags
     */
    public Set<String> getActiveFlags() {
        return activeFlags;
    }
}
