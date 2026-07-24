package io.github.bowerbird.java.core.flag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagDefinitionTest {

    @Test
    void parse_simpleFlag_notNegated() {
        var def = FlagDefinition.parse("DEBUG", FlagSource.PLUGIN_CONFIG);
        assertThat(def.name()).isEqualTo("DEBUG");
        assertThat(def.negated()).isFalse();
        assertThat(def.source()).isEqualTo(FlagSource.PLUGIN_CONFIG);
    }

    @Test
    void parse_negatedFlag_negatedTrue() {
        var def = FlagDefinition.parse("!DEBUG", FlagSource.FLAG_FILE);
        assertThat(def.name()).isEqualTo("DEBUG");
        assertThat(def.negated()).isTrue();
        assertThat(def.source()).isEqualTo(FlagSource.FLAG_FILE);
    }

    @Test
    void parse_whitespace_trimmed() {
        var def = FlagDefinition.parse("  DEBUG  ", FlagSource.SYSTEM_PROPERTY);
        assertThat(def.name()).isEqualTo("DEBUG");
        assertThat(def.negated()).isFalse();
    }

    @Test
    void parse_negatedWithWhitespace_trimmed() {
        var def = FlagDefinition.parse("  ! DEBUG  ", FlagSource.FLAG_FILE);
        assertThat(def.name()).isEqualTo("DEBUG");
        assertThat(def.negated()).isTrue();
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThatThrownBy(() -> new FlagDefinition("", false, FlagSource.PLUGIN_CONFIG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThatThrownBy(() -> new FlagDefinition(null, false, FlagSource.PLUGIN_CONFIG))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullSource_throwsException() {
        assertThatThrownBy(() -> new FlagDefinition("DEBUG", false, null))
                .isInstanceOf(NullPointerException.class);
    }
}
