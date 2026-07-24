package io.github.bowerbird.java.core.flag;

import java.nio.file.Path;

/**
 * Factory that selects the appropriate {@link FlagFileParser} based on file extension.
 */
public final class FlagFileParserFactory {

    private FlagFileParserFactory() {
        // utility class
    }

    /**
     * Returns a parser for the given flag file path, auto-detected by extension.
     *
     * @param path the flag file path
     * @return the appropriate parser
     * @throws IllegalArgumentException if the extension is not recognized
     */
    public static FlagFileParser forPath(Path path) {
        var fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return new YamlFlagFileParser();
        }
        if (fileName.endsWith(".properties")) {
            return new PropertiesFlagFileParser();
        }
        throw new IllegalArgumentException(
                "unsupported flag file extension: %s (expected .yaml, .yml, or .properties)".formatted(fileName)
        );
    }
}
