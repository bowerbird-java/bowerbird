package io.github.bowerbird.java.core.diagnostic;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when the preprocessor encounters unrecoverable errors in strict mode.
 */
public final class PreprocessorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<Diagnostic> errors;

    /**
     * Creates a preprocessor exception with the given message and associated error diagnostics.
     *
     * @param message the summary message
     * @param errors  the list of error diagnostics that triggered this exception
     */
    public PreprocessorException(String message, List<Diagnostic> errors) {
        super(message);
        this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * Returns the error diagnostics that triggered this exception.
     *
     * @return an unmodifiable list of error diagnostics
     */
    public List<Diagnostic> getErrors() {
        return errors;
    }
}
