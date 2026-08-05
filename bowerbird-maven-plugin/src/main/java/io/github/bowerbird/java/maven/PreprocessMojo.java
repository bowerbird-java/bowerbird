package io.github.bowerbird.java.maven;

import io.github.bowerbird.java.core.PreprocessorOrchestrator;
import io.github.bowerbird.java.core.diagnostic.ErrorMode;
import io.github.bowerbird.java.core.diagnostic.ErrorReporter;
import io.github.bowerbird.java.core.diagnostic.Severity;
import io.github.bowerbird.java.core.flag.FlagDefinition;
import io.github.bowerbird.java.core.flag.FlagResolver;
import io.github.bowerbird.java.core.flag.FlagSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Preprocesses Java source files by evaluating Bowerbird conditional annotations
 * ({@code @IfDef}, {@code @IfNotDef}, {@code @ElseIfDef}, {@code @ElseDef}) and
 * producing a modified source tree with excluded elements removed.
 *
 * <p>Binds to the {@code generate-sources} phase. The preprocessed sources are written
 * to {@code target/generated-sources/bowerbird/} and registered as a compile source root,
 * replacing the original source directory.</p>
 */
@Mojo(name = "preprocess", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class PreprocessMojo extends AbstractMojo {

    /** the Maven project, injected automatically. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** the source directory to preprocess. */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDirectory;

    /** the output directory for preprocessed sources. */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/bowerbird", required = true)
    private File outputDirectory;

    /** inline flag definitions (lowest precedence). */
    @Parameter
    private List<String> flags;

    /** path to the external flag file (.properties or .yaml). */
    @Parameter
    private File flagFile;

    /** source file encoding. */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;

    /** error handling mode: strict or lenient. */
    @Parameter(defaultValue = "strict")
    private String errorMode;

    /** enable incremental preprocessing cache. */
    @Parameter(defaultValue = "true")
    private boolean incremental;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!sourceDirectory.exists()) {
            getLog().info("Source directory does not exist, skipping: " + sourceDirectory);
            return;
        }

        var charset = resolveCharset();
        var mode = resolveErrorMode();
        var errorReporter = new ErrorReporter(mode);

        // resolve flags
        var pluginFlags = resolvePluginFlags();
        var systemFlags = resolveSystemFlags();
        var flagFilePath = flagFile != null ? flagFile.toPath() : null;
        var flagResolver = new FlagResolver(pluginFlags, flagFilePath, systemFlags);
        var activeFlags = flagResolver.resolve();

        errorReporter.reportAll(flagResolver.getDiagnostics());

        getLog().info("Bowerbird preprocessing with active flags: " + activeFlags);

        // resolve cache directory
        var cacheDir = incremental
                ? outputDirectory.toPath().resolveSibling("bowerbird-cache")
                : null;

        // run the preprocessor
        var orchestrator = new PreprocessorOrchestrator(activeFlags, errorReporter, charset, cacheDir);
        var result = orchestrator.processDirectory(sourceDirectory.toPath(), outputDirectory.toPath());

        // log diagnostics
        for (var diagnostic : result.diagnostics()) {
            switch (diagnostic.severity()) {
                case ERROR -> getLog().error(diagnostic.format());
                case WARNING -> getLog().warn(diagnostic.format());
                case INFO -> getLog().info(diagnostic.format());
            }
        }

        getLog().info("Bowerbird processed %d files (%d skipped, %d excluded, %d elements removed) in %dms"
                .formatted(result.processedFiles(), result.skippedFiles(), result.excludedFiles(),
                        result.totalRemovedElements(), result.durationMs()));

        // replace source root
        project.getCompileSourceRoots().remove(sourceDirectory.getAbsolutePath());
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        // fail build if strict mode and errors
        if (mode == ErrorMode.STRICT && errorReporter.hasErrors()) {
            throw new MojoFailureException("Bowerbird preprocessing failed. See errors above.");
        }
    }

    private Set<String> resolvePluginFlags() {
        if (flags == null || flags.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(flags);
    }

    private Set<String> resolveSystemFlags() {
        var sysProp = System.getProperty("bowerbird.flags");
        if (sysProp == null || sysProp.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(sysProp.split(",")));
    }

    private Charset resolveCharset() {
        if (sourceEncoding != null && !sourceEncoding.isBlank()) {
            return Charset.forName(sourceEncoding);
        }
        return StandardCharsets.UTF_8;
    }

    private ErrorMode resolveErrorMode() {
        if ("lenient".equalsIgnoreCase(errorMode)) {
            return ErrorMode.LENIENT;
        }
        return ErrorMode.STRICT;
    }
}
