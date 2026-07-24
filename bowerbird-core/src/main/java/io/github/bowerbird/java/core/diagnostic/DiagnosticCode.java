package io.github.bowerbird.java.core.diagnostic;

/**
 * All diagnostic codes emitted by the Bowerbird preprocessor.
 *
 * <p>Codes are partitioned by subsystem:</p>
 * <ul>
 *   <li>{@code BWB-001} to {@code BWB-006} — group validation</li>
 *   <li>{@code BWB-007} to {@code BWB-009} — expression evaluation</li>
 *   <li>{@code BWB-010} to {@code BWB-013} — I/O and configuration</li>
 *   <li>{@code BWB-014} — import cleanup</li>
 * </ul>
 */
public enum DiagnosticCode {

    // group validation
    BWB_001("Group has no head annotation", Severity.ERROR),
    BWB_002("Duplicate @ElseDef in group", Severity.ERROR),
    BWB_003("Mixed element kinds in group", Severity.ERROR),
    BWB_004("Orphaned branch reference", Severity.ERROR),
    BWB_005("@ElseDef not last in group", Severity.ERROR),
    BWB_006("Single-branch group", Severity.WARNING),

    // expression evaluation
    BWB_007("Malformed expression", Severity.ERROR),
    BWB_008("Empty expression", Severity.ERROR),
    BWB_009("Undefined flag reference", Severity.WARNING),

    // I/O and configuration
    BWB_010("Flag file not found", Severity.ERROR),
    BWB_011("Flag file parse error", Severity.ERROR),
    BWB_012("Source file parse error", Severity.ERROR),
    BWB_013("Output write error", Severity.ERROR),

    // import cleanup
    BWB_014("Orphaned import removed", Severity.INFO);

    private final String description;
    private final Severity defaultSeverity;

    DiagnosticCode(String description, Severity defaultSeverity) {
        this.description = description;
        this.defaultSeverity = defaultSeverity;
    }

    /**
     * Returns a human-readable description of this diagnostic.
     *
     * @return the description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the default severity for this diagnostic code.
     *
     * @return the default severity
     */
    public Severity defaultSeverity() {
        return defaultSeverity;
    }

    /**
     * Formats the code as {@code BWB-NNN}.
     *
     * @return the formatted code string
     */
    public String formatted() {
        return name().replace('_', '-');
    }
}
