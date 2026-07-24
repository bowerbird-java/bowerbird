package io.github.bowerbird.java.core.flag;

import io.github.bowerbird.java.core.diagnostic.Diagnostic;
import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import io.github.bowerbird.java.core.diagnostic.Severity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Resolves the active flag set by merging flags from multiple sources with
 * defined precedence.
 *
 * <p>Precedence order (ascending): plugin configuration &lt; flag file &lt; system properties.
 * Higher-precedence sources override lower ones. Negated flags (prefixed with {@code !})
 * remove a flag from the accumulated set.</p>
 */
public final class FlagResolver {

    private final Set<String> pluginFlags;
    private final Path flagFilePath;
    private final Set<String> systemFlags;
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    /**
     * Creates a flag resolver.
     *
     * @param pluginFlags  flags defined in the plugin configuration (may be empty)
     * @param flagFilePath path to the external flag file, or {@code null} if not specified
     * @param systemFlags  flags from system properties (may be empty)
     */
    public FlagResolver(Set<String> pluginFlags, Path flagFilePath, Set<String> systemFlags) {
        this.pluginFlags = pluginFlags != null ? Set.copyOf(pluginFlags) : Set.of();
        this.flagFilePath = flagFilePath;
        this.systemFlags = systemFlags != null ? Set.copyOf(systemFlags) : Set.of();
    }

    /**
     * Resolves the final active flag set by merging all sources.
     *
     * @return the resolved set of active flag names
     */
    public Set<String> resolve() {
        var flags = new LinkedHashSet<>(pluginFlags);

        // layer 2: flag file (medium precedence)
        if (flagFilePath != null) {
            applyFileFlags(flags);
        }

        // layer 3: system properties (highest precedence)
        flags.addAll(systemFlags);

        return Collections.unmodifiableSet(flags);
    }

    private void applyFileFlags(Set<String> flags) {
        if (!Files.exists(flagFilePath)) {
            diagnostics.add(Diagnostic.global(
                    DiagnosticCode.BWB_010,
                    Severity.ERROR,
                    "Flag file not found: %s".formatted(flagFilePath)
            ));
            return;
        }
        try {
            var parser = FlagFileParserFactory.forPath(flagFilePath);
            var fileDefinitions = parser.parse(flagFilePath);
            for (var def : fileDefinitions) {
                if (def.negated()) {
                    flags.remove(def.name());
                } else {
                    flags.add(def.name());
                }
            }
        } catch (IOException e) {
            diagnostics.add(Diagnostic.global(
                    DiagnosticCode.BWB_011,
                    Severity.ERROR,
                    "Failed to parse flag file %s: %s".formatted(flagFilePath, e.getMessage())
            ));
        }
    }

    /**
     * Returns diagnostics generated during flag resolution (e.g., file-not-found errors).
     *
     * @return the list of diagnostics
     */
    public List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }
}
