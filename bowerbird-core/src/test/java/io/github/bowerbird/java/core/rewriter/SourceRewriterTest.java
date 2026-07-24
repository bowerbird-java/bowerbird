package io.github.bowerbird.java.core.rewriter;

import io.github.bowerbird.java.core.expression.ExpressionEvaluator;
import io.github.bowerbird.java.core.parser.SourceParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SourceRewriterTest {

    private final SourceParser parser = new SourceParser();

    private Path fixture(String name) {
        var url = getClass().getClassLoader().getResource("fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("fixture not found: " + name);
        }
        return Path.of(url.getPath());
    }

    private RewriteResult rewrite(String fixtureName, Set<String> flags) {
        var parseResult = parser.parse(fixture(fixtureName), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(flags);
        var rewriter = new SourceRewriter(evaluator);
        return rewriter.rewrite(parseResult);
    }

    // ── standalone @IfDef ──────────────────────────────────

    @Test
    void rewrite_standaloneIfDef_flagPresent_methodRetained() {
        var result = rewrite("StandaloneIfDef.java", Set.of("DEBUG"));
        assertThat(result.fileExcluded()).isFalse();
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).isEmpty();
        assertThat(result.modifiedSource()).contains("debugMethod");
        assertThat(result.modifiedSource()).contains("normalMethod");
        // annotation should be stripped from retained element
        assertThat(result.modifiedSource()).doesNotContain("@IfDef");
    }

    @Test
    void rewrite_standaloneIfDef_flagAbsent_methodRemoved() {
        var result = rewrite("StandaloneIfDef.java", Set.of());
        assertThat(result.fileExcluded()).isFalse();
        assertThat(result.removedElements()).hasSize(1);
        assertThat(result.modifiedSource()).doesNotContain("debugMethod");
        assertThat(result.modifiedSource()).contains("normalMethod");
    }

    // ── type-level @IfDef (whole-file exclusion) ──────────

    @Test
    void rewrite_typeLevelIfDef_flagPresent_fileIncluded() {
        var result = rewrite("TypeLevelIfDef.java", Set.of("DEBUG"));
        assertThat(result.fileExcluded()).isFalse();
        assertThat(result.modifiedSource()).contains("TypeLevelIfDef");
        assertThat(result.modifiedSource()).doesNotContain("@IfDef");
    }

    @Test
    void rewrite_typeLevelIfDef_flagAbsent_fileExcluded() {
        var result = rewrite("TypeLevelIfDef.java", Set.of());
        assertThat(result.fileExcluded()).isTrue();
        assertThat(result.modifiedSource()).isEmpty();
    }

    // ── grouped @IfDef / @ElseDef ─────────────────────────

    @Test
    void rewrite_groupedIfElse_headTrue_headRetainedElseRemoved() {
        var result = rewrite("GroupedIfElse.java", Set.of("DEBUG"));
        assertThat(result.fileExcluded()).isFalse();
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).hasSize(1);
        assertThat(result.modifiedSource()).contains("TRACE");
        assertThat(result.modifiedSource()).doesNotContain("WARN");
        assertThat(result.modifiedSource()).contains("unrelated");
    }

    @Test
    void rewrite_groupedIfElse_headFalse_elseRetainedHeadRemoved() {
        var result = rewrite("GroupedIfElse.java", Set.of());
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).hasSize(1);
        assertThat(result.modifiedSource()).contains("WARN");
        assertThat(result.modifiedSource()).doesNotContain("TRACE");
    }

    // ── multi-branch group ────────────────────────────────

    @Test
    void rewrite_multiBranch_headTrue_headRetained() {
        var result = rewrite("MultiBranchGroup.java", Set.of("USE_REDIS"));
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).hasSize(2);
        assertThat(result.modifiedSource()).contains("redis");
        assertThat(result.modifiedSource()).doesNotContain("memcached");
        assertThat(result.modifiedSource()).doesNotContain("in-memory");
    }

    @Test
    void rewrite_multiBranch_elseIfTrue_elseIfRetained() {
        var result = rewrite("MultiBranchGroup.java", Set.of("USE_MEMCACHED"));
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).hasSize(2);
        assertThat(result.modifiedSource()).contains("memcached");
        assertThat(result.modifiedSource()).doesNotContain("redis");
        assertThat(result.modifiedSource()).doesNotContain("in-memory");
    }

    @Test
    void rewrite_multiBranch_noneMatch_elseBranchRetained() {
        var result = rewrite("MultiBranchGroup.java", Set.of());
        assertThat(result.retainedElements()).hasSize(1);
        assertThat(result.removedElements()).hasSize(2);
        assertThat(result.modifiedSource()).contains("in-memory");
        assertThat(result.modifiedSource()).doesNotContain("redis");
        assertThat(result.modifiedSource()).doesNotContain("memcached");
    }

    @Test
    void rewrite_multiBranch_headAndElseIfBothMatch_headWins() {
        // both USE_REDIS and USE_MEMCACHED are active — head wins (first match)
        var result = rewrite("MultiBranchGroup.java", Set.of("USE_REDIS", "USE_MEMCACHED"));
        assertThat(result.modifiedSource()).contains("redis");
        assertThat(result.modifiedSource()).doesNotContain("memcached");
    }

    // ── boolean expressions ───────────────────────────────

    @Test
    void rewrite_booleanExpression_bothConditionsMet_methodRetained() {
        var result = rewrite("BooleanExpression.java", Set.of("FEATURE_A"));
        assertThat(result.modifiedSource()).contains("conditionalMethod");
    }

    @Test
    void rewrite_booleanExpression_productionFlag_methodRemoved() {
        var result = rewrite("BooleanExpression.java", Set.of("FEATURE_A", "PRODUCTION"));
        assertThat(result.modifiedSource()).doesNotContain("conditionalMethod");
        assertThat(result.modifiedSource()).contains("alwaysPresent");
    }

    // ── field conditionals (grouped) ─────────────────────

    @Test
    void rewrite_fieldConditionals_metricsActive_realRegistryRetained() {
        var result = rewrite("FieldConditionals.java", Set.of("METRICS"));
        assertThat(result.modifiedSource()).contains("\"real\"");
        assertThat(result.modifiedSource()).doesNotContain("\"noop\"");
        assertThat(result.modifiedSource()).contains("alwaysPresent");
    }

    @Test
    void rewrite_fieldConditionals_metricsAbsent_noopRegistryRetained() {
        var result = rewrite("FieldConditionals.java", Set.of());
        assertThat(result.modifiedSource()).contains("\"noop\"");
        assertThat(result.modifiedSource()).doesNotContain("\"real\"");
    }

    // ── standalone @IfNotDef on field ─────────────────────

    @Test
    void rewrite_fieldIfNotDef_flagAbsent_fieldRetained() {
        var result = rewrite("FieldConditionals.java", Set.of());
        assertThat(result.modifiedSource()).contains("\"modern\"");
    }

    @Test
    void rewrite_fieldIfNotDef_flagPresent_fieldRemoved() {
        var result = rewrite("FieldConditionals.java", Set.of("LEGACY"));
        assertThat(result.modifiedSource()).doesNotContain("\"modern\"");
    }

    // ── no annotations (passthrough) ──────────────────────

    @Test
    void rewrite_noAnnotations_noChanges() {
        var parseResult = parser.parse(fixture("NoAnnotations.java"), StandardCharsets.UTF_8);
        assertThat(parseResult.hasNoConditionals()).isTrue();
        // no rewrite needed — the orchestrator would copy the file unchanged
    }

    // ── annotation stripping ──────────────────────────────

    @Test
    void rewrite_retainedElements_allBowerbirdAnnotationsStripped() {
        var result = rewrite("MultiBranchGroup.java", Set.of("USE_REDIS"));
        assertThat(result.modifiedSource()).doesNotContain("@IfDef");
        assertThat(result.modifiedSource()).doesNotContain("@ElseIfDef");
        assertThat(result.modifiedSource()).doesNotContain("@ElseDef");
    }
}
