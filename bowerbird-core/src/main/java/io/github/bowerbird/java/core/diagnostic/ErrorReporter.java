package io.github.bowerbird.java.core.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collects diagnostics emitted during preprocessing and enforces the configured error mode.
 *
 * <p>This class is thread-safe: diagnostics can be reported from multiple virtual threads
 * concurrently during parallel file processing.</p>
 */
public final class ErrorReporter {

    private final ErrorMode mode;
    private final ConcurrentLinkedQueue<Diagnostic> diagnostics = new ConcurrentLinkedQueue<>();

    /**
     * Creates a reporter with the specified error mode.
     *
     * @param mode the error mode governing how validation errors are handled
     */
    public ErrorReporter(ErrorMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    /**
     * Reports a diagnostic. In {@link ErrorMode#LENIENT} mode, errors are downgraded
     * to warnings before being stored.
     *
     * @param diagnostic the diagnostic to report
     */
    public void report(Diagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic must not be null");
        if (mode == ErrorMode.LENIENT && diagnostic.severity() == Severity.ERROR) {
            // downgrade errors to warnings in lenient mode
            diagnostics.add(new Diagnostic(
                    diagnostic.code(),
                    Severity.WARNING,
                    diagnostic.message(),
                    diagnostic.sourceFile(),
                    diagnostic.line(),
                    diagnostic.column()
            ));
        } else {
            diagnostics.add(diagnostic);
        }
    }

    /**
     * Reports all diagnostics from the given list.
     *
     * @param items the diagnostics to report
     */
    public void reportAll(List<Diagnostic> items) {
        items.forEach(this::report);
    }

    /**
     * Returns {@code true} if any diagnostic with {@link Severity#ERROR} has been reported.
     *
     * @return whether errors exist
     */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR);
    }

    /**
     * Returns an unmodifiable snapshot of all collected diagnostics.
     *
     * @return the diagnostics list
     */
    public List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    /**
     * Returns the configured error mode.
     *
     * @return the error mode
     */
    public ErrorMode getMode() {
        return mode;
    }

    /**
     * Throws a {@link PreprocessorException} if any errors have been reported.
     * Called at the end of processing to enforce strict mode.
     *
     * @throws PreprocessorException if errors exist
     */
    public void failIfErrors() {
        if (hasErrors()) {
            var errors = diagnostics.stream()
                    .filter(d -> d.severity() == Severity.ERROR)
                    .toList();
            throw new PreprocessorException(
                    "Preprocessing failed with %d error(s)".formatted(errors.size()),
                    errors
            );
        }
    }
}
