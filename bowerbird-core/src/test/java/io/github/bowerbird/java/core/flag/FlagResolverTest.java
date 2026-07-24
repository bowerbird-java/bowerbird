package io.github.bowerbird.java.core.flag;

import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FlagResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolve_pluginFlagsOnly_returnsPluginFlags() {
        var resolver = new FlagResolver(Set.of("DEBUG", "FEATURE_A"), null, null);
        assertThat(resolver.resolve()).containsExactlyInAnyOrder("DEBUG", "FEATURE_A");
    }

    @Test
    void resolve_fileFlagsAdded_mergedWithPlugin() throws IOException {
        var flagFile = tempDir.resolve("bowerbird-flags.properties");
        Files.writeString(flagFile, "flags=METRICS,TRACE\n");

        var resolver = new FlagResolver(Set.of("DEBUG"), flagFile, null);
        assertThat(resolver.resolve()).containsExactlyInAnyOrder("DEBUG", "METRICS", "TRACE");
    }

    @Test
    void resolve_fileFlagNegatesPlugin_flagRemoved() throws IOException {
        var flagFile = tempDir.resolve("bowerbird-flags.properties");
        Files.writeString(flagFile, "flags=METRICS,!DEBUG\n");

        var resolver = new FlagResolver(Set.of("DEBUG"), flagFile, null);
        assertThat(resolver.resolve()).containsExactlyInAnyOrder("METRICS");
    }

    @Test
    void resolve_systemFlagsOverride_addedToFinal() throws IOException {
        var flagFile = tempDir.resolve("bowerbird-flags.properties");
        Files.writeString(flagFile, "flags=!DEBUG\n");

        var resolver = new FlagResolver(Set.of("DEBUG"), flagFile, Set.of("DEBUG", "PRODUCTION"));
        var result = resolver.resolve();
        // file negates DEBUG, but system re-adds it
        assertThat(result).contains("DEBUG", "PRODUCTION");
    }

    @Test
    void resolve_allEmpty_returnsEmptySet() {
        var resolver = new FlagResolver(Set.of(), null, Set.of());
        assertThat(resolver.resolve()).isEmpty();
    }

    @Test
    void resolve_yamlFile_parsedCorrectly() throws IOException {
        var flagFile = tempDir.resolve("bowerbird-flags.yaml");
        Files.writeString(flagFile, """
                flags:
                  - DEBUG
                  - FEATURE_AUTH
                  - "!LEGACY"
                """);

        var resolver = new FlagResolver(Set.of("LEGACY"), flagFile, null);
        assertThat(resolver.resolve()).containsExactlyInAnyOrder("DEBUG", "FEATURE_AUTH");
    }

    @Test
    void resolve_fileNotFound_reportsBWB010() {
        var missingFile = tempDir.resolve("nonexistent.properties");
        var resolver = new FlagResolver(Set.of("DEBUG"), missingFile, null);
        resolver.resolve();

        assertThat(resolver.getDiagnostics()).hasSize(1);
        assertThat(resolver.getDiagnostics().getFirst().code()).isEqualTo(DiagnosticCode.BWB_010);
    }

    @Test
    void resolve_malformedFile_reportsBWB011() throws IOException {
        var flagFile = tempDir.resolve("bad.yaml");
        Files.writeString(flagFile, "flags: not_a_list_just_a_string\n");

        var resolver = new FlagResolver(Set.of(), flagFile, null);
        resolver.resolve();

        assertThat(resolver.getDiagnostics()).hasSize(1);
        assertThat(resolver.getDiagnostics().getFirst().code()).isEqualTo(DiagnosticCode.BWB_011);
    }
}
