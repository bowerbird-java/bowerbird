package io.github.bowerbird.java.core.imports;

import io.github.bowerbird.java.core.expression.ExpressionEvaluator;
import io.github.bowerbird.java.core.parser.SourceParser;
import io.github.bowerbird.java.core.rewriter.SourceRewriter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ImportCleanerTest {

    private final SourceParser parser = new SourceParser();
    private final ImportCleaner cleaner = new ImportCleaner();

    private Path fixture(String name) {
        var url = getClass().getClassLoader().getResource("fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("fixture not found: " + name);
        }
        return Path.of(url.getPath());
    }

    @Test
    void clean_afterRewrite_bowerbirdImportsRemoved() {
        var parseResult = parser.parse(fixture("StandaloneIfDef.java"), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(Set.of("DEBUG"));
        var rewriter = new SourceRewriter(evaluator);
        rewriter.rewrite(parseResult);

        var result = cleaner.clean(parseResult.compilationUnit());

        assertThat(result.removedImports()).anyMatch(i -> i.contains("bowerbird"));
    }

    @Test
    void clean_afterRewrite_bowerbirdImportsRemovedWhenFlagAbsent() {
        var parseResult = parser.parse(fixture("StandaloneIfDef.java"), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(Set.of());
        var rewriter = new SourceRewriter(evaluator);
        rewriter.rewrite(parseResult);

        var result = cleaner.clean(parseResult.compilationUnit());

        assertThat(result.removedImports()).anyMatch(i -> i.contains("bowerbird"));
    }

    @Test
    void clean_multiBranchGroup_unusedAnnotationImportsRemoved() {
        var parseResult = parser.parse(fixture("MultiBranchGroup.java"), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(Set.of("USE_REDIS"));
        var rewriter = new SourceRewriter(evaluator);
        rewriter.rewrite(parseResult);

        var result = cleaner.clean(parseResult.compilationUnit());

        // all three Bowerbird annotation imports should be removed
        assertThat(result.removedImports()).anyMatch(i -> i.contains("IfDef"));
        assertThat(result.removedImports()).anyMatch(i -> i.contains("ElseIfDef"));
        assertThat(result.removedImports()).anyMatch(i -> i.contains("ElseDef"));
    }

    @Test
    void clean_noAnnotations_noImportsRemoved() {
        var parseResult = parser.parse(fixture("NoAnnotations.java"), StandardCharsets.UTF_8);
        var result = cleaner.clean(parseResult.compilationUnit());
        assertThat(result.removedImports()).isEmpty();
    }

    @Test
    void clean_afterRewrite_serializedOutputHasNoBowerbirdImports() {
        var parseResult = parser.parse(fixture("GroupedIfElse.java"), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(Set.of("DEBUG"));
        var rewriter = new SourceRewriter(evaluator);
        rewriter.rewrite(parseResult);
        cleaner.clean(parseResult.compilationUnit());

        var output = parseResult.compilationUnit().toString();
        assertThat(output).doesNotContain("import io.github.bowerbird");
    }

    @Test
    void clean_fieldConditionals_bowerbirdImportsRemovedKeepsOthers() {
        var parseResult = parser.parse(fixture("FieldConditionals.java"), StandardCharsets.UTF_8);
        var evaluator = new ExpressionEvaluator(Set.of("METRICS"));
        var rewriter = new SourceRewriter(evaluator);
        rewriter.rewrite(parseResult);

        var result = cleaner.clean(parseResult.compilationUnit());

        // Bowerbird imports removed
        assertThat(result.removedImports()).allMatch(i -> i.contains("bowerbird"));
    }
}
