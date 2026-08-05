package io.github.bowerbird.java.core.cache;

import com.github.javaparser.utils.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the incremental preprocessing cache.
 *
 * <p>The cache tracks source file hashes and flag configuration hashes to skip
 * re-preprocessing of unchanged files. The cache is stored as a simple text-based
 * format in {@code target/bowerbird-cache/incremental.cache}.</p>
 *
 * <p>Invalidation rules:</p>
 * <ul>
 *   <li>If the flag hash changes, the entire cache is invalidated.</li>
 *   <li>If a source file's content hash changes, that file is re-preprocessed.</li>
 *   <li>New files are preprocessed and added to the cache.</li>
 *   <li>Deleted source files have their cache entries and output files removed.</li>
 * </ul>
 *
 * <p>This class is thread-safe: concurrent calls to {@link #getEntry} and
 * {@link #putEntry} from virtual threads during parallel file processing are safe.</p>
 */
public final class IncrementalCache {

    private static final String CACHE_FILE_NAME = "incremental.cache";
    private static final int CACHE_VERSION = 1;
    private static final String VERSION_PREFIX = "# bowerbird-cache-v";
    private static final String FLAG_HASH_PREFIX = "flag-hash:";
    private static final String ENTRY_SEPARATOR = "|";

    private final Path cacheDir;
    private final String currentFlagHash;
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private boolean fullRebuildRequired = false;

    /**
     * Creates an incremental cache.
     *
     * @param cacheDir    the directory for cache storage (e.g., {@code target/bowerbird-cache})
     * @param activeFlags the resolved set of active flags (used to compute the flag hash)
     */
    public IncrementalCache(Path cacheDir, Set<String> activeFlags) {
        this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir must not be null");
        this.currentFlagHash = computeFlagHash(activeFlags);
    }

    /**
     * Loads the cache from disk. If the cache file does not exist or the flag hash
     * has changed, a full rebuild is required.
     *
     * @return this cache instance (for chaining)
     */
    public IncrementalCache load() {
        var cacheFile = cacheDir.resolve(CACHE_FILE_NAME);
        if (!Files.exists(cacheFile)) {
            fullRebuildRequired = true;
            return this;
        }

        try {
            var lines = Files.readAllLines(cacheFile, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                fullRebuildRequired = true;
                return this;
            }

            // validate version
            if (!lines.getFirst().equals(VERSION_PREFIX + CACHE_VERSION)) {
                fullRebuildRequired = true;
                return this;
            }

            // validate flag hash
            var storedFlagHash = lines.get(1);
            if (!storedFlagHash.startsWith(FLAG_HASH_PREFIX)) {
                fullRebuildRequired = true;
                return this;
            }
            var cachedHash = storedFlagHash.substring(FLAG_HASH_PREFIX.length());
            if (!cachedHash.equals(currentFlagHash)) {
                fullRebuildRequired = true;
                return this;
            }

            // parse entries
            for (int i = 2; i < lines.size(); i++) {
                var line = lines.get(i).strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                var parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    var entry = new CacheEntry(parts[0], parts[1], Boolean.parseBoolean(parts[2]));
                    entries.put(entry.relativePath(), entry);
                }
            }
        } catch (IOException e) {
            fullRebuildRequired = true;
        }

        return this;
    }

    /**
     * Returns {@code true} if a full rebuild is required (cache missing, corrupted,
     * or flag configuration changed).
     *
     * @return whether all files must be reprocessed
     */
    public boolean isFullRebuildRequired() {
        return fullRebuildRequired;
    }

    /**
     * Returns the cached entry for the given relative path, or empty if not cached.
     *
     * @param relativePath the path relative to the source root
     * @return the cached entry, or empty
     */
    public Optional<CacheEntry> getEntry(String relativePath) {
        if (fullRebuildRequired) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(relativePath));
    }

    /**
     * Adds or updates a cache entry.
     *
     * @param entry the cache entry to store
     */
    public void putEntry(CacheEntry entry) {
        entries.put(entry.relativePath(), entry);
    }

    /**
     * Removes the cache entry for the given path.
     *
     * @param relativePath the path to remove
     */
    public void removeEntry(String relativePath) {
        entries.remove(relativePath);
    }

    /**
     * Returns all cached relative paths (for stale entry detection).
     *
     * @return unmodifiable set of cached paths
     */
    public Set<String> cachedPaths() {
        return Collections.unmodifiableSet(new HashSet<>(entries.keySet()));
    }

    /**
     * Computes the SHA-256 hash of a source file's content.
     *
     * @param sourceFile the file to hash
     * @return the hex-encoded SHA-256 digest
     * @throws IOException if the file cannot be read
     */
    public static String computeSourceHash(Path sourceFile) throws IOException {
        var bytes = Files.readAllBytes(sourceFile);
        return sha256Hex(bytes);
    }

    /**
     * Checks if a source file has changed compared to its cached entry.
     *
     * @param relativePath the path relative to the source root
     * @param sourceFile   the current source file path
     * @return {@code true} if the file needs reprocessing (changed, new, or full rebuild)
     */
    public boolean needsProcessing(String relativePath, Path sourceFile) {
        if (fullRebuildRequired) {
            return true;
        }
        var cached = entries.get(relativePath);
        if (cached == null) {
            return true; // new file
        }
        try {
            var currentHash = computeSourceHash(sourceFile);
            return !currentHash.equals(cached.sourceHash());
        } catch (IOException e) {
            return true; // can't read → reprocess
        }
    }

    /**
     * Saves the cache to disk.
     *
     * @throws IOException if the cache file cannot be written
     */
    public void save() throws IOException {
        Files.createDirectories(cacheDir);
        var lines = new ArrayList<String>();
        lines.add(VERSION_PREFIX + CACHE_VERSION);
        lines.add(FLAG_HASH_PREFIX + currentFlagHash);

        // sort entries for deterministic output
        entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    var entry = e.getValue();
                    lines.add(entry.relativePath()
                            + ENTRY_SEPARATOR + entry.sourceHash()
                            + ENTRY_SEPARATOR + entry.excluded());
                });

        Files.write(cacheDir.resolve(CACHE_FILE_NAME), lines, StandardCharsets.UTF_8);
    }

    /**
     * Removes output files for source files that no longer exist, and cleans
     * their cache entries.
     *
     * @param currentSourceFiles the set of relative paths that currently exist
     * @param outputRoot         the output directory
     * @return the list of stale paths that were cleaned up
     */
    public List<String> cleanupStaleEntries(Set<String> currentSourceFiles, Path outputRoot) {
        var stale = new ArrayList<String>();
        for (var cachedPath : new ArrayList<>(entries.keySet())) {
            if (!currentSourceFiles.contains(cachedPath)) {
                stale.add(cachedPath);
                entries.remove(cachedPath);
                // delete stale output file
                var outputFile = outputRoot.resolve(cachedPath);
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e) {
                    // best effort — log and continue
                }
            }
        }
        return stale;
    }

    private static String computeFlagHash(Set<String> flags) {
        var sorted = new TreeSet<>(flags);
        var joined = String.join(",", sorted);
        return sha256Hex(joined.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(data);
            var sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
