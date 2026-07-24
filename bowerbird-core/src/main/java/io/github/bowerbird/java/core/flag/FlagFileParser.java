package io.github.bowerbird.java.core.flag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Parses flag definitions from an external file.
 */
public interface FlagFileParser {

    /**
     * Parses the given flag file and returns all flag definitions found.
     *
     * @param path the path to the flag file
     * @return the list of flag definitions
     * @throws IOException if the file cannot be read
     */
    List<FlagDefinition> parse(Path path) throws IOException;
}
