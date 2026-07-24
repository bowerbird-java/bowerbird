package io.github.bowerbird.java.core.diagnostic;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticTest {

    @Test
    void format_withFileAndLocation_includesAllParts() {
        var diag = Diagnostic.of(DiagnosticCode.BWB_007, Severity.ERROR,
                "Malformed expression \"&&\"", Path.of("Foo.java"), 12, 5);

        var formatted = diag.format();
        assertThat(formatted).contains("[BWB-007]");
        assertThat(formatted).contains("ERROR");
        assertThat(formatted).contains("Malformed expression \"&&\"");
        assertThat(formatted).contains("Foo.java:12:5");
    }

    @Test
    void format_withFileNoColumn_omitsColumn() {
        var diag = Diagnostic.of(DiagnosticCode.BWB_014, Severity.INFO,
                "Removed import", Path.of("Bar.java"), 3, -1);

        var formatted = diag.format();
        assertThat(formatted).contains("Bar.java:3");
        assertThat(formatted).doesNotContain("3:-1");
    }

    @Test
    void format_global_noFileInfo() {
        var diag = Diagnostic.global(DiagnosticCode.BWB_010, Severity.ERROR, "File not found");

        var formatted = diag.format();
        assertThat(formatted).contains("[BWB-010]");
        assertThat(formatted).contains("File not found");
        assertThat(formatted).doesNotContain("→");
    }

    @Test
    void constructor_nullCode_throwsException() {
        assertThatThrownBy(() -> Diagnostic.global(null, Severity.ERROR, "test"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullSeverity_throwsException() {
        assertThatThrownBy(() -> Diagnostic.global(DiagnosticCode.BWB_001, null, "test"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullMessage_throwsException() {
        assertThatThrownBy(() -> Diagnostic.global(DiagnosticCode.BWB_001, Severity.ERROR, null))
                .isInstanceOf(NullPointerException.class);
    }
}
