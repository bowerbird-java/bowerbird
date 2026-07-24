package io.github.bowerbird.java.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A complete group of related conditional branches sharing the same {@code group} key
 * within a single enclosing type.
 */
public final class ConditionalGroup {

    private final String groupKey;
    private final String enclosingType;
    private final List<ConditionalElement> members;

    /**
     * Creates a conditional group.
     *
     * @param groupKey      the shared group key
     * @param enclosingType the fully-qualified name of the enclosing type
     * @param members       the ordered list of group members (head first)
     */
    public ConditionalGroup(String groupKey, String enclosingType, List<ConditionalElement> members) {
        this.groupKey = Objects.requireNonNull(groupKey, "groupKey must not be null");
        this.enclosingType = Objects.requireNonNull(enclosingType, "enclosingType must not be null");
        this.members = Collections.unmodifiableList(Objects.requireNonNull(members, "members must not be null"));
    }

    /**
     * Returns the group key.
     *
     * @return the key
     */
    public String groupKey() {
        return groupKey;
    }

    /**
     * Returns the enclosing type name.
     *
     * @return the type name
     */
    public String enclosingType() {
        return enclosingType;
    }

    /**
     * Returns all members in order.
     *
     * @return unmodifiable member list
     */
    public List<ConditionalElement> members() {
        return members;
    }

    /**
     * Returns the group head ({@code @IfDef} or {@code @IfNotDef}), if present.
     *
     * @return the head element
     */
    public Optional<ConditionalElement> head() {
        return members.stream().filter(ConditionalElement::isHead).findFirst();
    }

    /**
     * Returns the {@code @ElseIfDef} branches in order.
     *
     * @return the intermediate branches
     */
    public List<ConditionalElement> elseIfBranches() {
        return members.stream()
                .filter(m -> m.annotationType() == ConditionalAnnotationType.ELSE_IF_DEF)
                .toList();
    }

    /**
     * Returns the {@code @ElseDef} branch, if present.
     *
     * @return the fallback branch
     */
    public Optional<ConditionalElement> elseBranch() {
        return members.stream()
                .filter(m -> m.annotationType() == ConditionalAnnotationType.ELSE_DEF)
                .findFirst();
    }

    /**
     * Returns the element kind of the group head, or the first member if no head is present.
     *
     * @return the element kind
     */
    public TargetElementKind elementKind() {
        return members.getFirst().elementKind();
    }
}
