package io.github.bowerbird.java.core.flag;

/**
 * Identifies the origin of a flag definition, which determines its precedence
 * during flag resolution.
 *
 * <p>Precedence order (ascending): {@link #PLUGIN_CONFIG} &lt; {@link #FLAG_FILE}
 * &lt; {@link #SYSTEM_PROPERTY}.</p>
 */
public enum FlagSource {

    /** flags defined in the Maven plugin {@code <configuration>} block (lowest precedence). */
    PLUGIN_CONFIG,

    /** flags loaded from an external {@code .properties} or {@code .yaml} file. */
    FLAG_FILE,

    /** flags passed via system properties ({@code -Dbowerbird.flags=...}) (highest precedence). */
    SYSTEM_PROPERTY
}
