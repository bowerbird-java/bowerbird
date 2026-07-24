package io.github.bowerbird.java.core.diagnostic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticCodeTest {

    @ParameterizedTest
    @EnumSource(DiagnosticCode.class)
    void formatted_allCodes_followBWBPattern(DiagnosticCode code) {
        assertThat(code.formatted()).matches("BWB-\\d{3}");
    }

    @ParameterizedTest
    @EnumSource(DiagnosticCode.class)
    void description_allCodes_nonEmpty(DiagnosticCode code) {
        assertThat(code.description()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(DiagnosticCode.class)
    void defaultSeverity_allCodes_nonNull(DiagnosticCode code) {
        assertThat(code.defaultSeverity()).isNotNull();
    }

    @Test
    void formatted_BWB001_correctFormat() {
        assertThat(DiagnosticCode.BWB_001.formatted()).isEqualTo("BWB-001");
    }

    @Test
    void formatted_BWB014_correctFormat() {
        assertThat(DiagnosticCode.BWB_014.formatted()).isEqualTo("BWB-014");
    }

    @Test
    void defaultSeverity_validationErrors_areError() {
        assertThat(DiagnosticCode.BWB_001.defaultSeverity()).isEqualTo(Severity.ERROR);
        assertThat(DiagnosticCode.BWB_002.defaultSeverity()).isEqualTo(Severity.ERROR);
        assertThat(DiagnosticCode.BWB_003.defaultSeverity()).isEqualTo(Severity.ERROR);
        assertThat(DiagnosticCode.BWB_004.defaultSeverity()).isEqualTo(Severity.ERROR);
        assertThat(DiagnosticCode.BWB_005.defaultSeverity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void defaultSeverity_BWB006_isWarning() {
        assertThat(DiagnosticCode.BWB_006.defaultSeverity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void defaultSeverity_BWB014_isInfo() {
        assertThat(DiagnosticCode.BWB_014.defaultSeverity()).isEqualTo(Severity.INFO);
    }
}
