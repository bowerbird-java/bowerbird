package io.github.bowerbird.java.core.validation;

import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import io.github.bowerbird.java.core.parser.SourceParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GroupValidatorTest {

    private GroupValidator validator;
    private SourceParser parser;

    @BeforeEach
    void setUp() {
        validator = new GroupValidator();
        parser = new SourceParser();
    }

    private Path fixture(String name) {
        var url = getClass().getClassLoader().getResource("fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("fixture not found: " + name);
        }
        return Path.of(url.getPath());
    }

    @Test
    void validate_validIfElseGroup_noDiagnostics() {
        var result = parser.parse(fixture("GroupedIfElse.java"), StandardCharsets.UTF_8);
        var diagnostics = validator.validate(result.conditionalGroups(), fixture("GroupedIfElse.java"));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void validate_validMultiBranchGroup_noDiagnostics() {
        var result = parser.parse(fixture("MultiBranchGroup.java"), StandardCharsets.UTF_8);
        var diagnostics = validator.validate(result.conditionalGroups(), fixture("MultiBranchGroup.java"));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void validate_duplicateElseDef_reportsBWB002() {
        var result = parser.parse(fixture("DuplicateElseDef.java"), StandardCharsets.UTF_8);
        var diagnostics = validator.validate(result.conditionalGroups(), fixture("DuplicateElseDef.java"));
        assertThat(diagnostics).anyMatch(d -> d.code() == DiagnosticCode.BWB_002);
    }

    @Test
    void validate_mixedKinds_reportsBWB003() {
        var result = parser.parse(fixture("MixedKinds.java"), StandardCharsets.UTF_8);
        var diagnostics = validator.validate(result.conditionalGroups(), fixture("MixedKinds.java"));
        assertThat(diagnostics).anyMatch(d -> d.code() == DiagnosticCode.BWB_003);
    }

    @Test
    void detectOrphans_orphanedElseDef_reportsBWB004() {
        var result = parser.parse(fixture("OrphanedElseDef.java"), StandardCharsets.UTF_8);

        // the OrphanedElseDef has @ElseDef with group="nonexistent" but no @IfDef head
        var orphanDiagnostics = validator.detectOrphans(
                result.conditionalElements(), result.conditionalGroups(), fixture("OrphanedElseDef.java"));
        assertThat(orphanDiagnostics).anyMatch(d -> d.code() == DiagnosticCode.BWB_004);
    }

    @Test
    void validate_noAnnotations_noDiagnostics() {
        var result = parser.parse(fixture("NoAnnotations.java"), StandardCharsets.UTF_8);
        var diagnostics = validator.validate(result.conditionalGroups(), fixture("NoAnnotations.java"));
        assertThat(diagnostics).isEmpty();
    }
}
