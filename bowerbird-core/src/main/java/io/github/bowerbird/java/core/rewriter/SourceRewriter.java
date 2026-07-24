package io.github.bowerbird.java.core.rewriter;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.bowerbird.java.core.expression.ExpressionEvaluator;
import io.github.bowerbird.java.core.parser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates conditional annotations against active flags and removes excluded
 * elements from the JavaParser AST.
 *
 * <p>After rewriting, the AST can be serialized back to valid Java source that
 * contains only the surviving elements.</p>
 */
public final class SourceRewriter {

    private final ExpressionEvaluator evaluator;

    /**
     * Creates a rewriter with the given expression evaluator.
     *
     * @param evaluator the evaluator configured with the active flag set
     */
    public SourceRewriter(ExpressionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    /**
     * Rewrites the parsed source by evaluating conditions and removing excluded elements.
     *
     * @param parseResult the parse result from {@link SourceParser}
     * @return the rewrite result
     */
    public RewriteResult rewrite(ParseResult parseResult) {
        var removed = new ArrayList<ConditionalElement>();
        var retained = new ArrayList<ConditionalElement>();

        // resolve grouped conditionals
        for (var group : parseResult.conditionalGroups().values()) {
            resolveGroup(group, removed, retained);
        }

        // resolve standalone conditionals
        for (var element : parseResult.standaloneConditions()) {
            if (evaluateElement(element)) {
                retained.add(element);
            } else {
                removed.add(element);
            }
        }

        // check if a top-level type was excluded (whole-file exclusion)
        boolean fileExcluded = removed.stream()
                .anyMatch(e -> e.elementKind() == TargetElementKind.TYPE && isTopLevelType(e.node()));

        if (fileExcluded) {
            return new RewriteResult("", removed, retained, true);
        }

        // remove excluded nodes from the AST
        for (var element : removed) {
            element.node().remove();
        }

        // remove Bowerbird annotations from retained nodes
        for (var element : retained) {
            removeBowerbirdAnnotations(element.node());
        }

        var modifiedSource = parseResult.compilationUnit().toString();
        return new RewriteResult(modifiedSource, removed, retained, false);
    }

    private void resolveGroup(ConditionalGroup group, List<ConditionalElement> removed, List<ConditionalElement> retained) {
        var headOpt = group.head();
        if (headOpt.isEmpty()) {
            return; // validation catches this; leave untouched
        }

        ConditionalElement winner = null;

        // evaluate head
        var head = headOpt.get();
        if (evaluateElement(head)) {
            winner = head;
        }

        // evaluate @ElseIfDef branches (only if no winner yet)
        if (winner == null) {
            for (var elseIf : group.elseIfBranches()) {
                if (evaluateElement(elseIf)) {
                    winner = elseIf;
                    break;
                }
            }
        }

        // fallthrough to @ElseDef (only if no winner yet)
        if (winner == null) {
            winner = group.elseBranch().orElse(null);
        }

        // partition members into removed/retained
        for (var member : group.members()) {
            if (member == winner) {
                retained.add(member);
            } else {
                removed.add(member);
            }
        }
    }

    private boolean evaluateElement(ConditionalElement element) {
        return switch (element.annotationType()) {
            case IF_DEF -> evaluator.evaluate(element.expression());
            case IF_NOT_DEF -> !evaluator.evaluate(element.expression());
            case ELSE_IF_DEF -> evaluator.evaluate(element.expression());
            case ELSE_DEF -> true; // @ElseDef is the default; always true if reached
        };
    }

    private static boolean isTopLevelType(Node node) {
        if (!(node instanceof TypeDeclaration<?> type)) {
            return false;
        }
        // a top-level type's parent is the CompilationUnit
        return type.getParentNode()
                .map(parent -> parent instanceof com.github.javaparser.ast.CompilationUnit)
                .orElse(false);
    }

    private static void removeBowerbirdAnnotations(Node node) {
        if (node instanceof com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> annotatable) {
            annotatable.getAnnotations().removeIf(ann -> {
                var name = ann.getNameAsString();
                return "IfDef".equals(name) || "IfNotDef".equals(name)
                        || "ElseIfDef".equals(name) || "ElseDef".equals(name);
            });
        }
    }
}
