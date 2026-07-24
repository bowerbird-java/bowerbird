package io.github.bowerbird.java.core.imports;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects and removes imports that are no longer referenced after source rewriting.
 *
 * <p>Also unconditionally removes Bowerbird annotation imports
 * ({@code io.github.bowerbird.java.api.*}), since these are not needed after
 * preprocessing.</p>
 */
public final class ImportCleaner {

    private static final String BOWERBIRD_API_PACKAGE = "io.github.bowerbird.java.api";

    /**
     * Cleans orphaned imports from the compilation unit.
     *
     * @param compilationUnit the AST (already rewritten — excluded nodes removed)
     * @return the cleanup result
     */
    public ImportCleanResult clean(CompilationUnit compilationUnit) {
        var usedTypes = collectUsedTypeNames(compilationUnit);
        var removedImports = new ArrayList<String>();
        var remainingImports = new ArrayList<String>();
        var importsToRemove = new ArrayList<ImportDeclaration>();

        for (var importDecl : compilationUnit.getImports()) {
            var fqn = importDecl.getNameAsString();

            // always remove Bowerbird annotation imports
            if (isBowerbirdImport(fqn)) {
                importsToRemove.add(importDecl);
                removedImports.add(fqn);
                continue;
            }

            // keep wildcard imports (conservative — we cannot reliably determine usage)
            if (importDecl.isAsterisk()) {
                remainingImports.add(fqn + ".*");
                continue;
            }

            // check if the imported simple name is still used
            var simpleName = extractSimpleName(importDecl);
            if (usedTypes.contains(simpleName)) {
                remainingImports.add(fqn);
            } else {
                importsToRemove.add(importDecl);
                removedImports.add(fqn);
            }
        }

        importsToRemove.forEach(ImportDeclaration::remove);
        return new ImportCleanResult(removedImports, remainingImports);
    }

    private Set<String> collectUsedTypeNames(CompilationUnit cu) {
        var names = new HashSet<String>();
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(SimpleName n, Void arg) {
                names.add(n.getIdentifier());
                super.visit(n, arg);
            }

            @Override
            public void visit(Name n, Void arg) {
                names.add(n.getIdentifier());
                super.visit(n, arg);
            }
        }, null);
        return names;
    }

    private static boolean isBowerbirdImport(String fqn) {
        return fqn.startsWith(BOWERBIRD_API_PACKAGE);
    }

    private static String extractSimpleName(ImportDeclaration importDecl) {
        var fqn = importDecl.getNameAsString();
        var lastDot = fqn.lastIndexOf('.');
        return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
    }
}
