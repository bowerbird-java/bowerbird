package io.github.bowerbird.java.core.flag;

import java.util.Objects;

/**
 * A single flag entry parsed from a flag source.
 *
 * <p>A flag may be negated (prefixed with {@code !}) to explicitly remove a flag
 * defined at a lower-precedence layer.</p>
 *
 * @param name    the flag name (without the {@code !} prefix)
 * @param negated {@code true} if this entry removes the flag rather than adding it
 * @param source  the origin of this flag definition
 */
public record FlagDefinition(String name, boolean negated, FlagSource source) {

    public FlagDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("flag name must not be blank");
        }
    }

    /**
     * Parses a flag string that may be prefixed with {@code !} for negation.
     *
     * @param raw    the raw flag string (e.g., {@code "DEBUG"} or {@code "!DEBUG"})
     * @param source the origin of this flag
     * @return the parsed flag definition
     */
    public static FlagDefinition parse(String raw, FlagSource source) {
        Objects.requireNonNull(raw, "raw must not be null");
        var trimmed = raw.strip();
        if (trimmed.startsWith("!")) {
            return new FlagDefinition(trimmed.substring(1).strip(), true, source);
        }
        return new FlagDefinition(trimmed, false, source);
    }
}
