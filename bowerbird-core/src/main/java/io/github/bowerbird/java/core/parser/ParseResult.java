package io.github.bowerbird.java.core.parser;

import com.github.javaparser.ast.CompilationUnit;
import io.github.bowerbird.java.core.diagnostic.Diagnostic;

import java.util.List;
import java.util.Map;

/**
 * The result of parsing a single Java source file for Bowerbird annotations.
 *
 * @param compilationUnit     the JavaParser AST of the source file
 * @param conditionalElements all annotated elements found in the file
 * @param conditionalGroups   groups indexed by group key
 * @param standaloneConditions elements with no group (standalone @IfDef/@IfNotDef)
 * @param diagnostics         any parsing-level diagnostics
 */
public record ParseResult(
        CompilationUnit compilationUnit,
        List<ConditionalElement> conditionalElements,
        Map<String, ConditionalGroup> conditionalGroups,
        List<ConditionalElement> standaloneConditions,
        List<Diagnostic> diagnostics
) {

    /**
     * Returns {@code true} if no conditional annotations were found.
     *
     * @return whether the file has any Bowerbird annotations
     */
    public boolean hasNoConditionals() {
        return conditionalElements.isEmpty();
    }
}
