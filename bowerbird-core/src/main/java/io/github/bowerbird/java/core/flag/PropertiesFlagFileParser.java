package io.github.bowerbird.java.core.flag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Parses flag definitions from a {@code .properties} file.
 *
 * <p>Expected format:</p>
 * <pre>
 * flags=DEBUG,FEATURE_AUTH,!LEGACY
 * </pre>
 */
public final class PropertiesFlagFileParser implements FlagFileParser {

    @Override
    public List<FlagDefinition> parse(Path path) throws IOException {
        var props = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            props.load(reader);
        }
        var flagsValue = props.getProperty("flags", "");
        if (flagsValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(flagsValue.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .map(s -> FlagDefinition.parse(s, FlagSource.FLAG_FILE))
                .toList();
    }
}
