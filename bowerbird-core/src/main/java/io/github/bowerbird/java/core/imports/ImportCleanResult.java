package io.github.bowerbird.java.core.imports;

import java.util.List;

/**
 * The result of import cleanup after source rewriting.
 *
 * @param removedImports   fully-qualified names of imports that were removed
 * @param remainingImports fully-qualified names of imports that were kept
 */
public record ImportCleanResult(List<String> removedImports, List<String> remainingImports) {
}
