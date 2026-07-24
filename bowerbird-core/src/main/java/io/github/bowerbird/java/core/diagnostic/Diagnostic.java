package io.github.bowerbird.java.core.diagnostic;

import java.nio.file.Path;
import java.util.Objects;

/**
 * An immutable diagnostic message emitted during preprocessing.
 *
 * @param code       the diagnostic code identifying the issue category
 * @param severity   the severity level
 * @param message    the human-readable message with context details
 * @param sourceFile the source file where the issue was detected, or {@code null} for global diagnostics
 * @param line       the 1-based line number, or {@code -1} if not applicable
 * @param column     the 1-based column number, or {@code -1} if not applicable
 */
public record Diagnostic(DiagnosticCode code, Severity severity, String message, Path sourceFile, int line, int column) {

    public Diagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Creates a diagnostic with file location context.
     *
     * @param code       the diagnostic code
     * @param severity   the severity
     * @param message    the detail message
     * @param sourceFile the source file path
     * @param line       the 1-based line number
     * @param column     the 1-based column number
     * @return the diagnostic
     */
    public static Diagnostic of(DiagnosticCode code, Severity severity, String message, Path sourceFile, int line, int column) {
        return new Diagnostic(code, severity, message, sourceFile, line, column);
    }

    /**
     * Creates a diagnostic without file location context (global/configuration-level).
     *
     * @param code     the diagnostic code
     * @param severity the severity
     * @param message  the detail message
     * @return the diagnostic
     */
    public static Diagnostic global(DiagnosticCode code, Severity severity, String message) {
        return new Diagnostic(code, severity, message, null, -1, -1);
    }

    /**
     * Formats this diagnostic for display.
     *
     * @return a formatted string such as {@code [BWB-007] ERROR: Malformed expression "&&" → Foo.java:12:5}
     */
    public String format() {
        var sb = new StringBuilder()
                .append('[').append(code.formatted()).append("] ")
                .append(severity).append(": ")
                .append(message);
        if (sourceFile != null) {
            sb.append(" → ").append(sourceFile.getFileName());
            if (line > 0) {
                sb.append(':').append(line);
                if (column > 0) {
                    sb.append(':').append(column);
                }
            }
        }
        return sb.toString();
    }
}
