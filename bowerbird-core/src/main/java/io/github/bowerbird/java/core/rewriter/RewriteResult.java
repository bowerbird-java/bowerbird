package io.github.bowerbird.java.core.rewriter;

import io.github.bowerbird.java.core.parser.ConditionalElement;

import java.util.List;

/**
 * The result of rewriting a single Java source file.
 *
 * @param modifiedSource   the rewritten source code (empty if file is excluded)
 * @param removedElements  elements that were stripped
 * @param retainedElements elements that survived preprocessing
 * @param fileExcluded     {@code true} if the top-level type was excluded, meaning the
 *                         entire file should be omitted from the output directory
 */
public record RewriteResult(
        String modifiedSource,
        List<ConditionalElement> removedElements,
        List<ConditionalElement> retainedElements,
        boolean fileExcluded
) {
}
