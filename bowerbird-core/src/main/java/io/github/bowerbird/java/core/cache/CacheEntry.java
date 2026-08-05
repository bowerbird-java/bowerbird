package io.github.bowerbird.java.core.cache;

import java.util.Objects;

/**
 * Cache entry for a single preprocessed source file.
 *
 * @param relativePath the path relative to the source root (e.g., {@code com/example/Foo.java})
 * @param sourceHash   SHA-256 hex digest of the original source file content
 * @param excluded     {@code true} if the file was wholly excluded (top-level type conditional)
 */
public record CacheEntry(String relativePath, String sourceHash, boolean excluded) {

    public CacheEntry {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Objects.requireNonNull(sourceHash, "sourceHash must not be null");
    }
}
