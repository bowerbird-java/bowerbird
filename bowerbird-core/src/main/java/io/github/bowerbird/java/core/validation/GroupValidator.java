package io.github.bowerbird.java.core.validation;

import io.github.bowerbird.java.core.diagnostic.Diagnostic;
import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import io.github.bowerbird.java.core.diagnostic.Severity;
import io.github.bowerbird.java.core.parser.ConditionalAnnotationType;
import io.github.bowerbird.java.core.parser.ConditionalElement;
import io.github.bowerbird.java.core.parser.ConditionalGroup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates the structural integrity of conditional groups.
 *
 * <p>Enforces the rules defined in the Bowerbird specification:</p>
 * <ul>
 *   <li>BWB-001: every group must have exactly one {@code @IfDef} or {@code @IfNotDef} head</li>
 *   <li>BWB-002: at most one {@code @ElseDef} per group</li>
 *   <li>BWB-003: all members must share the same element kind</li>
 *   <li>BWB-004: orphaned {@code @ElseIfDef}/{@code @ElseDef} references</li>
 *   <li>BWB-005: {@code @ElseDef} must be the last branch</li>
 *   <li>BWB-006: single-branch group (informational)</li>
 * </ul>
 */
public final class GroupValidator {

    /**
     * Validates all groups and returns any diagnostics.
     *
     * @param groups     the groups to validate, indexed by group key
     * @param sourceFile the source file path (for diagnostic messages)
     * @return a list of diagnostics (may be empty)
     */
    public List<Diagnostic> validate(Map<String, ConditionalGroup> groups, Path sourceFile) {
        var diagnostics = new ArrayList<Diagnostic>();
        for (var group : groups.values()) {
            validateGroup(group, sourceFile, diagnostics);
        }
        return diagnostics;
    }

    private void validateGroup(ConditionalGroup group, Path sourceFile, List<Diagnostic> diagnostics) {
        var members = group.members();
        if (members.isEmpty()) {
            return;
        }

        // BWB-001: group must have a head
        var headOpt = group.head();
        if (headOpt.isEmpty()) {
            var first = members.getFirst();
            diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_001, Severity.ERROR,
                    "Group \"%s\" has no head annotation. An @IfDef or @IfNotDef must open the group."
                            .formatted(group.groupKey()),
                    sourceFile, first.sourceRange().startLine(), first.sourceRange().startColumn()));
            return; // remaining validations depend on a valid head
        }

        var head = headOpt.get();

        // BWB-003: all members must share the same element kind
        var expectedKind = head.elementKind();
        for (var member : members) {
            if (member.elementKind() != expectedKind) {
                diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_003, Severity.ERROR,
                        "Group \"%s\" mixes %s and %s. All members must be the same element kind."
                                .formatted(group.groupKey(), expectedKind, member.elementKind()),
                        sourceFile, member.sourceRange().startLine(), member.sourceRange().startColumn()));
            }
        }

        // BWB-002: at most one @ElseDef
        var elseDefCount = members.stream()
                .filter(m -> m.annotationType() == ConditionalAnnotationType.ELSE_DEF)
                .count();
        if (elseDefCount > 1) {
            diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_002, Severity.ERROR,
                    "Group \"%s\" has multiple @ElseDef annotations. At most one is allowed."
                            .formatted(group.groupKey()),
                    sourceFile, head.sourceRange().startLine(), head.sourceRange().startColumn()));
        }

        // BWB-005: @ElseDef must be last
        boolean elseDefSeen = false;
        for (var member : members) {
            if (elseDefSeen && member.annotationType() != ConditionalAnnotationType.ELSE_DEF) {
                diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_005, Severity.ERROR,
                        "@ElseDef in group \"%s\" must be the last branch. Found @%s after it."
                                .formatted(group.groupKey(), member.annotationType()),
                        sourceFile, member.sourceRange().startLine(), member.sourceRange().startColumn()));
            }
            if (member.annotationType() == ConditionalAnnotationType.ELSE_DEF) {
                elseDefSeen = true;
            }
        }

        // BWB-006: single-branch group (informational)
        if (members.size() == 1) {
            diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_006, Severity.WARNING,
                    "Group \"%s\" has only one branch. Consider using a standalone @IfDef instead."
                            .formatted(group.groupKey()),
                    sourceFile, head.sourceRange().startLine(), head.sourceRange().startColumn()));
        }
    }

    /**
     * Detects orphaned {@code @ElseIfDef} and {@code @ElseDef} elements that reference
     * groups with no head in the provided group map.
     *
     * @param allElements all conditional elements found in the source file
     * @param groups      the validated groups
     * @param sourceFile  the source file path
     * @return diagnostics for orphaned references (BWB-004)
     */
    public List<Diagnostic> detectOrphans(List<ConditionalElement> allElements,
                                          Map<String, ConditionalGroup> groups, Path sourceFile) {
        var diagnostics = new ArrayList<Diagnostic>();
        for (var element : allElements) {
            if (!element.isHead() && element.isGrouped()) {
                var group = groups.get(element.group());
                if (group == null || group.head().isEmpty()) {
                    diagnostics.add(Diagnostic.of(DiagnosticCode.BWB_004, Severity.ERROR,
                            "@%s references group \"%s\" which has no @IfDef/@IfNotDef head in this type."
                                    .formatted(element.annotationType(), element.group()),
                            sourceFile, element.sourceRange().startLine(), element.sourceRange().startColumn()));
                }
            }
        }
        return diagnostics;
    }
}
