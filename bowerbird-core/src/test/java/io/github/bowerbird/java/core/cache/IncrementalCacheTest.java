package io.github.bowerbird.java.core.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IncrementalCacheTest {

    @TempDir
    Path tempDir;

    private Path cacheDir;
    private Path sourceDir;

    @BeforeEach
    void setUp() throws IOException {
        cacheDir = tempDir.resolve("cache");
        sourceDir = tempDir.resolve("sources");
        Files.createDirectories(sourceDir);
    }

    private Path createSourceFile(String relativePath, String content) throws IOException {
        var file = sourceDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    // ── first run (no cache) ──────────────────────────────

    @Test
    void load_noCacheFile_fullRebuildRequired() {
        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        assertThat(cache.isFullRebuildRequired()).isTrue();
    }

    @Test
    void load_noCacheFile_getEntryReturnsEmpty() {
        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        assertThat(cache.getEntry("Foo.java")).isEmpty();
    }

    // ── save and reload ───────────────────────────────────

    @Test
    void saveAndReload_sameFlags_entriesPreserved() throws IOException {
        var flags = Set.of("DEBUG", "FEATURE_A");

        // first run: save entries
        var cache = new IncrementalCache(cacheDir, flags).load();
        cache.putEntry(new CacheEntry("com/example/Foo.java", "abc123", false));
        cache.putEntry(new CacheEntry("com/example/Bar.java", "def456", true));
        cache.save();

        // second run: reload
        var reloaded = new IncrementalCache(cacheDir, flags).load();
        assertThat(reloaded.isFullRebuildRequired()).isFalse();
        assertThat(reloaded.getEntry("com/example/Foo.java")).isPresent();
        assertThat(reloaded.getEntry("com/example/Foo.java").get().sourceHash()).isEqualTo("abc123");
        assertThat(reloaded.getEntry("com/example/Foo.java").get().excluded()).isFalse();
        assertThat(reloaded.getEntry("com/example/Bar.java")).isPresent();
        assertThat(reloaded.getEntry("com/example/Bar.java").get().excluded()).isTrue();
    }

    // ── flag change invalidation ──────────────────────────

    @Test
    void load_flagsChanged_fullRebuildRequired() throws IOException {
        // save with one flag set
        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        cache.putEntry(new CacheEntry("Foo.java", "abc123", false));
        cache.save();

        // reload with different flags
        var reloaded = new IncrementalCache(cacheDir, Set.of("PRODUCTION")).load();
        assertThat(reloaded.isFullRebuildRequired()).isTrue();
        assertThat(reloaded.getEntry("Foo.java")).isEmpty();
    }

    @Test
    void load_flagAdded_fullRebuildRequired() throws IOException {
        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        cache.putEntry(new CacheEntry("Foo.java", "abc123", false));
        cache.save();

        // add a flag
        var reloaded = new IncrementalCache(cacheDir, Set.of("DEBUG", "EXTRA")).load();
        assertThat(reloaded.isFullRebuildRequired()).isTrue();
    }

    // ── needsProcessing ───────────────────────────────────

    @Test
    void needsProcessing_noCacheEntry_returnsTrue() throws IOException {
        var file = createSourceFile("Foo.java", "class Foo {}");
        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();

        // no prior entry — needs processing
        assertThat(cache.isFullRebuildRequired()).isTrue();
        assertThat(cache.needsProcessing("Foo.java", file)).isTrue();
    }

    @Test
    void needsProcessing_unchangedFile_returnsFalse() throws IOException {
        var file = createSourceFile("Foo.java", "class Foo {}");
        var sourceHash = IncrementalCache.computeSourceHash(file);
        var flags = Set.of("DEBUG");

        // save cache with current hash
        var cache = new IncrementalCache(cacheDir, flags).load();
        cache.putEntry(new CacheEntry("Foo.java", sourceHash, false));
        cache.save();

        // reload and check — should not need processing
        var reloaded = new IncrementalCache(cacheDir, flags).load();
        assertThat(reloaded.needsProcessing("Foo.java", file)).isFalse();
    }

    @Test
    void needsProcessing_changedFile_returnsTrue() throws IOException {
        var file = createSourceFile("Foo.java", "class Foo {}");
        var flags = Set.of("DEBUG");

        // save cache with current hash
        var cache = new IncrementalCache(cacheDir, flags).load();
        cache.putEntry(new CacheEntry("Foo.java", IncrementalCache.computeSourceHash(file), false));
        cache.save();

        // modify the file
        Files.writeString(file, "class Foo { int x; }", StandardCharsets.UTF_8);

        // reload and check — should need processing
        var reloaded = new IncrementalCache(cacheDir, flags).load();
        assertThat(reloaded.needsProcessing("Foo.java", file)).isTrue();
    }

    // ── stale entry cleanup ───────────────────────────────

    @Test
    void cleanupStaleEntries_deletedFile_entryAndOutputRemoved() throws IOException {
        var outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        var flags = Set.of("DEBUG");
        var cache = new IncrementalCache(cacheDir, flags).load();
        cache.putEntry(new CacheEntry("Foo.java", "abc123", false));
        cache.putEntry(new CacheEntry("Deleted.java", "def456", false));
        cache.save();

        // create output for the "deleted" file so cleanup can remove it
        var staleOutput = outputDir.resolve("Deleted.java");
        Files.createDirectories(staleOutput.getParent());
        Files.writeString(staleOutput, "stale");

        // reload and cleanup — only Foo.java exists now
        var reloaded = new IncrementalCache(cacheDir, flags).load();
        var stale = reloaded.cleanupStaleEntries(Set.of("Foo.java"), outputDir);

        assertThat(stale).containsExactly("Deleted.java");
        assertThat(reloaded.getEntry("Deleted.java")).isEmpty();
        assertThat(reloaded.getEntry("Foo.java")).isPresent();
        assertThat(Files.exists(staleOutput)).isFalse();
    }

    // ── hash consistency ──────────────────────────────────

    @Test
    void computeSourceHash_sameContent_sameHash() throws IOException {
        var file1 = createSourceFile("A.java", "class A {}");
        var file2 = createSourceFile("B.java", "class A {}");

        assertThat(IncrementalCache.computeSourceHash(file1))
                .isEqualTo(IncrementalCache.computeSourceHash(file2));
    }

    @Test
    void computeSourceHash_differentContent_differentHash() throws IOException {
        var file1 = createSourceFile("A.java", "class A {}");
        var file2 = createSourceFile("B.java", "class B {}");

        assertThat(IncrementalCache.computeSourceHash(file1))
                .isNotEqualTo(IncrementalCache.computeSourceHash(file2));
    }

    @Test
    void computeSourceHash_flagOrderDoesNotMatter() throws IOException {
        var cache1 = new IncrementalCache(cacheDir, Set.of("A", "B", "C"));
        var cache2 = new IncrementalCache(cacheDir, Set.of("C", "A", "B"));

        // both should produce the same flag hash (sorted internally)
        // we can verify indirectly: save with one, load with the other
        cache1.load();
        cache1.putEntry(new CacheEntry("Foo.java", "abc", false));
        cache1.save();

        cache2.load();
        assertThat(cache2.isFullRebuildRequired()).isFalse();
    }

    // ── corrupted cache ───────────────────────────────────

    @Test
    void load_corruptedCacheFile_fullRebuildRequired() throws IOException {
        Files.createDirectories(cacheDir);
        Files.writeString(cacheDir.resolve("incremental.cache"), "garbage data\n");

        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        assertThat(cache.isFullRebuildRequired()).isTrue();
    }

    @Test
    void load_emptyCacheFile_fullRebuildRequired() throws IOException {
        Files.createDirectories(cacheDir);
        Files.writeString(cacheDir.resolve("incremental.cache"), "");

        var cache = new IncrementalCache(cacheDir, Set.of("DEBUG")).load();
        assertThat(cache.isFullRebuildRequired()).isTrue();
    }
}
