package io.github.bowerbird.java.core.flag;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Parses flag definitions from a {@code .yaml} or {@code .yml} file.
 *
 * <p>Uses SnakeYAML's {@link SafeConstructor} to prevent arbitrary object deserialization.</p>
 *
 * <p>Expected format:</p>
 * <pre>
 * flags:
 *   - DEBUG
 *   - FEATURE_AUTH
 *   - "!LEGACY"
 * </pre>
 */
public final class YamlFlagFileParser implements FlagFileParser {

    @Override
    @SuppressWarnings("unchecked")
    public List<FlagDefinition> parse(Path path) throws IOException {
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> document;
        try (var reader = Files.newBufferedReader(path)) {
            document = yaml.load(reader);
        }
        if (document == null || !document.containsKey("flags")) {
            return List.of();
        }
        var flagsRaw = document.get("flags");
        if (!(flagsRaw instanceof List<?> flagsList)) {
            throw new IOException("'flags' key must be a list in " + path);
        }
        return flagsList.stream()
                .map(Object::toString)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .map(s -> FlagDefinition.parse(s, FlagSource.FLAG_FILE))
                .toList();
    }
}
