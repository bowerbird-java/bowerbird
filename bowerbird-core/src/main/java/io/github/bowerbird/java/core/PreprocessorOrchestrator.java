package io.github.bowerbird.java.core;

import io.github.bowerbird.java.core.cache.CacheEntry;
import io.github.bowerbird.java.core.cache.IncrementalCache;
import io.github.bowerbird.java.core.diagnostic.Diagnostic;
import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import io.github.bowerbird.java.core.diagnostic.ErrorReporter;
import io.github.bowerbird.java.core.diagnostic.Severity;
import io.github.bowerbird.java.core.expression.ExpressionEvaluator;
import io.github.bowerbird.java.core.imports.ImportCleaner;
import io.github.bowerbird.java.core.parser.SourceParser;
import io.github.bowerbird.java.core.rewriter.SourceRewriter;
import io.github.bowerbird.java.core.validation.GroupValidator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Orchestrates the full Bowerbird preprocessing pipeline for single files or entire
 * source directories.
 *
 * <p>This is the main entry point for the core engine. It coordinates the source parser,
 * group validator, expression evaluator, source rewriter, and import cleaner.</p>
 *
 * <p>Supports optional incremental caching: when a cache directory is provided, unchanged
 * files are skipped on subsequent runs. A flag configuration change invalidates the entire
 * cache.</p>
 */
public final class PreprocessorOrchestrator {

    private final Set<String> activeFlags;
    private final ExpressionEvaluator evaluator;
    private final SourceParser parser;
    private final GroupValidator groupValidator;
    private final SourceRewriter rewriter;
    private final ImportCleaner importCleaner;
    private final ErrorReporter errorReporter;
    private final Charset charset;
    private final Path cacheDir;

    /**
     * Creates an orchestrator with the given configuration.
     *
     * @param activeFlags   the resolved set of active feature flags
     * @param errorReporter the error reporter (strict or lenient)
     * @param charset       the source file encoding
     */
    public PreprocessorOrchestrator(Set<String> activeFlags, ErrorReporter errorReporter, Charset charset) {
        this(activeFlags, errorReporter, charset, null);
    }

    /**
     * Creates an orchestrator with incremental caching support.
     *
     * @param activeFlags   the resolved set of active feature flags
     * @param errorReporter the error reporter (strict or lenient)
     * @param charset       the source file encoding
     * @param cacheDir      the directory for incremental cache storage, or {@code null} to disable caching
     */
    public PreprocessorOrchestrator(Set<String> activeFlags, ErrorReporter errorReporter, Charset charset, Path cacheDir) {
        Objects.requireNonNull(activeFlags, "activeFlags must not be null");
        this.activeFlags = Set.copyOf(activeFlags);
        this.evaluator = new ExpressionEvaluator(activeFlags);
        this.parser = new SourceParser();
        this.groupValidator = new GroupValidator();
        this.rewriter = new SourceRewriter(evaluator);
        this.importCleaner = new ImportCleaner();
        this.errorReporter = Objects.requireNonNull(errorReporter, "errorReporter must not be null");
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
        this.cacheDir = cacheDir;
    }

    /**
     * Processes a single Java source file.
     *
     * @param source the input source file
     * @param output the output path for the preprocessed file
     * @return the file processing result
     */
    public FileResult processFile(Path source, Path output) {
        // parse
        var parseResult = parser.parse(source, charset);
        errorReporter.reportAll(parseResult.diagnostics());

        // if no conditionals, copy file unchanged
        if (parseResult.hasNoConditionals()) {
            copyFile(source, output);
            return new FileResult(source, output, false, 0, List.of());
        }

        // validate groups
        var groupDiagnostics = groupValidator.validate(parseResult.conditionalGroups(), source);
        var orphanDiagnostics = groupValidator.detectOrphans(
                parseResult.conditionalElements(), parseResult.conditionalGroups(), source);
        errorReporter.reportAll(groupDiagnostics);
        errorReporter.reportAll(orphanDiagnostics);

        // if validation found issues, copy file unchanged
        boolean hasValidationIssues = !groupDiagnostics.isEmpty() || !orphanDiagnostics.isEmpty();
        if (hasValidationIssues) {
            copyFile(source, output);
            var allDiagnostics = new ArrayList<>(groupDiagnostics);
            allDiagnostics.addAll(orphanDiagnostics);
            return new FileResult(source, output, false, 0, allDiagnostics);
        }

        // rewrite
        var rewriteResult = rewriter.rewrite(parseResult);

        if (rewriteResult.fileExcluded()) {
            return new FileResult(source, null, true, rewriteResult.removedElements().size(), List.of());
        }

        // clean imports
        var importResult = importCleaner.clean(parseResult.compilationUnit());
        for (var removedImport : importResult.removedImports()) {
            errorReporter.report(Diagnostic.of(
                    DiagnosticCode.BWB_014, Severity.INFO,
                    "Removed unused import: %s".formatted(removedImport),
                    source, -1, -1
            ));
        }

        // write output (re-serialize after import cleanup)
        var finalSource = parseResult.compilationUnit().toString();
        writeFile(output, finalSource);

        return new FileResult(source, output, false, rewriteResult.removedElements().size(), List.of());
    }

    /**
     * Processes all {@code .java} files under the source root using virtual threads.
     *
     * <p>When a cache directory is configured, unchanged files are skipped. A flag
     * configuration change invalidates the entire cache.</p>
     *
     * @param sourceRoot the input source root directory
     * @param outputRoot the output directory for preprocessed sources
     * @return the aggregated directory result
     */
    public DirectoryResult processDirectory(Path sourceRoot, Path outputRoot) {
        var startTime = System.currentTimeMillis();
        var processedCount = new AtomicInteger();
        var skippedCount = new AtomicInteger();
        var excludedCount = new AtomicInteger();
        var removedElementCount = new AtomicLong();
        var allDiagnostics = new java.util.concurrent.ConcurrentLinkedQueue<Diagnostic>();

        List<Path> javaFiles;
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            errorReporter.report(Diagnostic.global(
                    DiagnosticCode.BWB_012, Severity.ERROR,
                    "Failed to scan source directory %s: %s".formatted(sourceRoot, e.getMessage())
            ));
            return new DirectoryResult(0, 0, 0, 0, errorReporter.getDiagnostics(),
                    System.currentTimeMillis() - startTime);
        }

        // build set of current relative paths for stale entry detection
        var currentRelativePaths = new HashSet<String>();
        for (var file : javaFiles) {
            currentRelativePaths.add(sourceRoot.relativize(file).toString());
        }

        // load incremental cache (if configured)
        IncrementalCache cache = null;
        if (cacheDir != null) {
            cache = new IncrementalCache(cacheDir, activeFlags).load();
        }

        final IncrementalCache effectiveCache = cache;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = javaFiles.stream().map(sourceFile -> executor.submit(() -> {
                var relativePath = sourceRoot.relativize(sourceFile);
                var relativePathStr = relativePath.toString();
                var outputFile = outputRoot.resolve(relativePath);

                // check cache — skip if unchanged
                if (effectiveCache != null && !effectiveCache.needsProcessing(relativePathStr, sourceFile)) {
                    // ensure output exists (may have been deleted by mvn clean on target/)
                    if (!effectiveCache.getEntry(relativePathStr).map(CacheEntry::excluded).orElse(false)
                            && !Files.exists(outputFile)) {
                        // output was cleaned — need to reprocess
                    } else {
                        skippedCount.incrementAndGet();
                        return;
                    }
                }

                var result = processFile(sourceFile, outputFile);

                processedCount.incrementAndGet();
                if (result.excluded()) {
                    excludedCount.incrementAndGet();
                }
                removedElementCount.addAndGet(result.removedElements());
                allDiagnostics.addAll(result.diagnostics());

                // update cache
                if (effectiveCache != null) {
                    try {
                        var sourceHash = IncrementalCache.computeSourceHash(sourceFile);
                        effectiveCache.putEntry(new CacheEntry(relativePathStr, sourceHash, result.excluded()));
                    } catch (IOException e) {
                        // cache update failure is non-fatal — file was still processed
                    }
                }
            })).toList();

            // wait for all to complete
            for (var future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    errorReporter.report(Diagnostic.global(
                            DiagnosticCode.BWB_013, Severity.ERROR,
                            "Error processing file: %s".formatted(e.getMessage())
                    ));
                }
            }
        }

        // cleanup stale entries and save cache
        if (cache != null) {
            var staleEntries = cache.cleanupStaleEntries(currentRelativePaths, outputRoot);
            try {
                cache.save();
            } catch (IOException e) {
                errorReporter.report(Diagnostic.global(
                        DiagnosticCode.BWB_013, Severity.WARNING,
                        "Failed to save incremental cache: %s".formatted(e.getMessage())
                ));
            }
        }

        var elapsed = System.currentTimeMillis() - startTime;
        return new DirectoryResult(
                processedCount.get(),
                skippedCount.get(),
                excludedCount.get(),
                removedElementCount.get(),
                errorReporter.getDiagnostics(),
                elapsed
        );
    }

    private void copyFile(Path source, Path output) {
        try {
            Files.createDirectories(output.getParent());
            Files.copy(source, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorReporter.report(Diagnostic.of(
                    DiagnosticCode.BWB_013, Severity.ERROR,
                    "Failed to copy file %s to %s: %s".formatted(source, output, e.getMessage()),
                    source, -1, -1
            ));
        }
    }

    private void writeFile(Path output, String content) {
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, content, charset);
        } catch (IOException e) {
            errorReporter.report(Diagnostic.of(
                    DiagnosticCode.BWB_013, Severity.ERROR,
                    "Failed to write preprocessed source to %s: %s".formatted(output, e.getMessage()),
                    output, -1, -1
            ));
        }
    }

    /**
     * Result of processing a single file.
     *
     * @param sourceFile      the original source file
     * @param outputFile      the preprocessed output file (null if excluded)
     * @param excluded        whether the file was wholly excluded
     * @param removedElements the count of elements removed
     * @param diagnostics     file-level diagnostics
     */
    public record FileResult(Path sourceFile, Path outputFile, boolean excluded,
                             int removedElements, List<Diagnostic> diagnostics) {
    }

    /**
     * Result of processing an entire directory.
     *
     * @param processedFiles       total files actually processed
     * @param skippedFiles         files skipped (unchanged, from cache)
     * @param excludedFiles        files wholly excluded
     * @param totalRemovedElements total elements removed across all files
     * @param diagnostics          all diagnostics
     * @param durationMs           wall-clock duration in milliseconds
     */
    public record DirectoryResult(int processedFiles, int skippedFiles, int excludedFiles,
                                  long totalRemovedElements, List<Diagnostic> diagnostics,
                                  long durationMs) {
    }
}
