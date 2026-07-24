package io.github.bowerbird.java.core.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceParserTest {

    private SourceParser parser;

    @BeforeEach
    void setUp() {
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
    void parse_standaloneIfDef_extractsOneElement() {
        var result = parser.parse(fixture("StandaloneIfDef.java"), StandardCharsets.UTF_8);

        assertThat(result.conditionalElements()).hasSize(1);
        assertThat(result.standaloneConditions()).hasSize(1);
        assertThat(result.conditionalGroups()).isEmpty();

        var element = result.standaloneConditions().getFirst();
        assertThat(element.annotationType()).isEqualTo(ConditionalAnnotationType.IF_DEF);
        assertThat(element.expression()).isEqualTo("DEBUG");
        assertThat(element.group()).isEmpty();
        assertThat(element.elementKind()).isEqualTo(TargetElementKind.METHOD);
    }

    @Test
    void parse_groupedIfElse_extractsGroupWithTwoMembers() {
        var result = parser.parse(fixture("GroupedIfElse.java"), StandardCharsets.UTF_8);

        assertThat(result.conditionalGroups()).containsKey("logging");
        var group = result.conditionalGroups().get("logging");
        assertThat(group.members()).hasSize(2);
        assertThat(group.head()).isPresent();
        assertThat(group.head().get().annotationType()).isEqualTo(ConditionalAnnotationType.IF_DEF);
        assertThat(group.elseBranch()).isPresent();
    }

    @Test
    void parse_multiBranchGroup_extractsThreeMembers() {
        var result = parser.parse(fixture("MultiBranchGroup.java"), StandardCharsets.UTF_8);

        assertThat(result.conditionalGroups()).containsKey("cache");
        var group = result.conditionalGroups().get("cache");
        assertThat(group.members()).hasSize(3);
        assertThat(group.head()).isPresent();
        assertThat(group.elseIfBranches()).hasSize(1);
        assertThat(group.elseBranch()).isPresent();
    }

    @Test
    void parse_typeLevelIfDef_extractsTypeElement() {
        var result = parser.parse(fixture("TypeLevelIfDef.java"), StandardCharsets.UTF_8);

        assertThat(result.standaloneConditions()).hasSize(1);
        var element = result.standaloneConditions().getFirst();
        assertThat(element.elementKind()).isEqualTo(TargetElementKind.TYPE);
        assertThat(element.expression()).isEqualTo("DEBUG");
    }

    @Test
    void parse_fieldConditionals_extractsGroupAndStandalone() {
        var result = parser.parse(fixture("FieldConditionals.java"), StandardCharsets.UTF_8);

        // the group "registry" should have @IfDef and @ElseDef
        assertThat(result.conditionalGroups()).containsKey("registry");
        var group = result.conditionalGroups().get("registry");
        assertThat(group.members()).hasSize(2);
        assertThat(group.head()).isPresent();
        assertThat(group.elseBranch()).isPresent();
        assertThat(group.members().getFirst().elementKind()).isEqualTo(TargetElementKind.FIELD);

        // standalone @IfNotDef("LEGACY") on the mode field
        assertThat(result.standaloneConditions()).hasSize(1);
        assertThat(result.standaloneConditions().getFirst().annotationType())
                .isEqualTo(ConditionalAnnotationType.IF_NOT_DEF);
    }

    @Test
    void parse_booleanExpression_extractsExpression() {
        var result = parser.parse(fixture("BooleanExpression.java"), StandardCharsets.UTF_8);

        assertThat(result.standaloneConditions()).hasSize(1);
        var element = result.standaloneConditions().getFirst();
        assertThat(element.expression()).isEqualTo("FEATURE_A && !PRODUCTION");
    }

    @Test
    void parse_noAnnotations_hasNoConditionals() {
        var result = parser.parse(fixture("NoAnnotations.java"), StandardCharsets.UTF_8);

        assertThat(result.hasNoConditionals()).isTrue();
        assertThat(result.conditionalElements()).isEmpty();
        assertThat(result.conditionalGroups()).isEmpty();
        assertThat(result.standaloneConditions()).isEmpty();
    }

    @Test
    void parse_nonexistentFile_returnsErrorDiagnostic() {
        var result = parser.parse(Path.of("nonexistent.java"), StandardCharsets.UTF_8);

        assertThat(result.diagnostics()).hasSize(1);
        assertThat(result.diagnostics().getFirst().code())
                .isEqualTo(io.github.bowerbird.java.core.diagnostic.DiagnosticCode.BWB_012);
    }

    @Test
    void parse_standaloneIfDef_sourceRangeIsPopulated() {
        var result = parser.parse(fixture("StandaloneIfDef.java"), StandardCharsets.UTF_8);

        var element = result.standaloneConditions().getFirst();
        assertThat(element.sourceRange().startLine()).isGreaterThan(0);
        assertThat(element.sourceRange().endLine()).isGreaterThanOrEqualTo(element.sourceRange().startLine());
    }
}
