package io.github.bowerbird.java.core.diagnostic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorReporterTest {

    @Test
    void report_strictMode_preservesErrorSeverity() {
        var reporter = new ErrorReporter(ErrorMode.STRICT);
        reporter.report(Diagnostic.global(DiagnosticCode.BWB_001, Severity.ERROR, "test error"));

        assertThat(reporter.hasErrors()).isTrue();
        assertThat(reporter.getDiagnostics().getFirst().severity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void report_lenientMode_downgradesErrorToWarning() {
        var reporter = new ErrorReporter(ErrorMode.LENIENT);
        reporter.report(Diagnostic.global(DiagnosticCode.BWB_001, Severity.ERROR, "test error"));

        assertThat(reporter.hasErrors()).isFalse();
        assertThat(reporter.getDiagnostics().getFirst().severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void report_lenientMode_preservesWarnings() {
        var reporter = new ErrorReporter(ErrorMode.LENIENT);
        reporter.report(Diagnostic.global(DiagnosticCode.BWB_009, Severity.WARNING, "test warning"));

        assertThat(reporter.getDiagnostics().getFirst().severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void failIfErrors_noErrors_doesNotThrow() {
        var reporter = new ErrorReporter(ErrorMode.STRICT);
        reporter.report(Diagnostic.global(DiagnosticCode.BWB_014, Severity.INFO, "info"));
        reporter.failIfErrors(); // should not throw
    }

    @Test
    void failIfErrors_withErrors_throwsPreprocessorException() {
        var reporter = new ErrorReporter(ErrorMode.STRICT);
        reporter.report(Diagnostic.global(DiagnosticCode.BWB_007, Severity.ERROR, "bad expression"));

        assertThatThrownBy(reporter::failIfErrors)
                .isInstanceOf(PreprocessorException.class)
                .hasMessageContaining("1 error(s)");
    }
}
