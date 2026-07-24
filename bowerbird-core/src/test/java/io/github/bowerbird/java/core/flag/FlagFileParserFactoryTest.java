package io.github.bowerbird.java.core.flag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagFileParserFactoryTest {

    @ParameterizedTest
    @ValueSource(strings = {"flags.yaml", "flags.yml", "FLAGS.YAML", "config.YML"})
    void forPath_yamlExtensions_returnsYamlParser(String filename) {
        var parser = FlagFileParserFactory.forPath(Path.of(filename));
        assertThat(parser).isInstanceOf(YamlFlagFileParser.class);
    }

    @Test
    void forPath_propertiesExtension_returnsPropertiesParser() {
        var parser = FlagFileParserFactory.forPath(Path.of("flags.properties"));
        assertThat(parser).isInstanceOf(PropertiesFlagFileParser.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"flags.json", "flags.xml", "flags.txt", "flags"})
    void forPath_unsupportedExtension_throwsException(String filename) {
        assertThatThrownBy(() -> FlagFileParserFactory.forPath(Path.of(filename)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported flag file extension");
    }
}
