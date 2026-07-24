package io.github.bowerbird.java.core.parser;

import com.github.javaparser.ast.Node;

import java.util.Objects;

/**
 * Metadata for a single source element annotated with a Bowerbird conditional annotation.
 *
 * @param annotationType the type of conditional annotation
 * @param expression     the condition expression (empty for {@code @ElseDef})
 * @param group          the group key (empty for standalone conditionals)
 * @param elementKind    the kind of source element (type, method, or field)
 * @param node           the JavaParser AST node for this element
 * @param sourceRange    the source location of this element
 */
public record ConditionalElement(
        ConditionalAnnotationType annotationType,
        String expression,
        String group,
        TargetElementKind elementKind,
        Node node,
        SourceRange sourceRange
) {

    public ConditionalElement {
        Objects.requireNonNull(annotationType, "annotationType must not be null");
        Objects.requireNonNull(expression, "expression must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(elementKind, "elementKind must not be null");
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(sourceRange, "sourceRange must not be null");
    }

    /**
     * Returns {@code true} if this element is a group head ({@code @IfDef} or {@code @IfNotDef}).
     *
     * @return whether this element is a group head
     */
    public boolean isHead() {
        return annotationType == ConditionalAnnotationType.IF_DEF
                || annotationType == ConditionalAnnotationType.IF_NOT_DEF;
    }

    /**
     * Returns {@code true} if this element belongs to a named group.
     *
     * @return whether the group key is non-empty
     */
    public boolean isGrouped() {
        return !group.isEmpty();
    }
}
