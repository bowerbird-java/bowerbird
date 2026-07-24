package io.github.bowerbird.java.core.parser;

/**
 * The line and column extent of a source element.
 *
 * @param startLine   the 1-based start line
 * @param endLine     the 1-based end line
 * @param startColumn the 1-based start column
 * @param endColumn   the 1-based end column
 */
public record SourceRange(int startLine, int endLine, int startColumn, int endColumn) {
}
