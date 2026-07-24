package io.github.bowerbird.java.core.diagnostic;

/**
 * Controls how the preprocessor handles validation errors.
 */
public enum ErrorMode {

    /**
     * Any validation error causes the build to fail immediately.
     */
    STRICT,

    /**
     * Validation errors are emitted as warnings; affected elements are left untouched.
     */
    LENIENT
}
