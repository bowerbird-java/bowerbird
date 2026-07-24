package io.github.bowerbird.java.core.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import io.github.bowerbird.java.core.diagnostic.Diagnostic;
import io.github.bowerbird.java.core.diagnostic.DiagnosticCode;
import io.github.bowerbird.java.core.diagnostic.Severity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses Java source files and extracts Bowerbird conditional annotation metadata.
 *
 * <p>Uses JavaParser for AST construction. Each call to {@link #parse} creates a fresh
 * parser instance, making this class safe for use across threads (each thread parses
 * independently).</p>
 */
public final class SourceParser {

    private static final Set<String> BOWERBIRD_ANNOTATIONS = Set.of(
            "IfDef", "IfNotDef", "ElseIfDef", "ElseDef"
    );

    /**
     * Parses a Java source file and extracts all Bowerbird conditional annotations.
     *
     * @param sourceFile the path to the Java source file
     * @param charset    the file encoding
     * @return the parse result containing the AST and conditional metadata
     */
    public ParseResult parse(Path sourceFile, Charset charset) {
        CompilationUnit cu;
        try {
            var source = Files.readString(sourceFile, charset);
            cu = StaticJavaParser.parse(source);
        } catch (IOException e) {
            return errorResult(sourceFile, DiagnosticCode.BWB_012,
                    "Failed to read source file %s: %s".formatted(sourceFile, e.getMessage()));
        } catch (Exception e) {
            return errorResult(sourceFile, DiagnosticCode.BWB_012,
                    "Failed to parse source file %s: %s".formatted(sourceFile, e.getMessage()));
        }

        var elements = new ArrayList<ConditionalElement>();
        var diagnostics = new ArrayList<Diagnostic>();

        // scan types
        cu.findAll(TypeDeclaration.class).forEach(type ->
                extractFromNode(type, TargetElementKind.TYPE, sourceFile, elements, diagnostics));

        // scan methods
        cu.findAll(MethodDeclaration.class).forEach(method ->
                extractFromNode(method, TargetElementKind.METHOD, sourceFile, elements, diagnostics));

        // scan fields
        cu.findAll(FieldDeclaration.class).forEach(field ->
                extractFromNode(field, TargetElementKind.FIELD, sourceFile, elements, diagnostics));

        // partition into groups and standalone
        var groups = new LinkedHashMap<String, List<ConditionalElement>>();
        var standalone = new ArrayList<ConditionalElement>();
        for (var element : elements) {
            if (element.isGrouped()) {
                groups.computeIfAbsent(element.group(), __ -> new ArrayList<>()).add(element);
            } else if (element.isHead()) {
                standalone.add(element);
            }
            // @ElseIfDef/@ElseDef without a group is caught during validation
        }

        var conditionalGroups = new LinkedHashMap<String, ConditionalGroup>();
        groups.forEach((key, members) -> {
            var enclosingType = resolveEnclosingType(members.getFirst().node());
            conditionalGroups.put(key, new ConditionalGroup(key, enclosingType, members));
        });

        return new ParseResult(cu, List.copyOf(elements), Map.copyOf(conditionalGroups), List.copyOf(standalone), List.copyOf(diagnostics));
    }

    private void extractFromNode(Node node, TargetElementKind kind, Path sourceFile,
                                 List<ConditionalElement> elements, List<Diagnostic> diagnostics) {
        if (node instanceof com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> annotatable) {
            for (var annotation : annotatable.getAnnotations()) {
                var simpleName = annotation.getNameAsString();
                if (BOWERBIRD_ANNOTATIONS.contains(simpleName)) {
                    var element = buildConditionalElement(annotation, kind, node, sourceFile, diagnostics);
                    if (element != null) {
                        elements.add(element);
                    }
                }
            }
        }
    }

    private ConditionalElement buildConditionalElement(AnnotationExpr annotation, TargetElementKind kind,
                                                       Node node, Path sourceFile, List<Diagnostic> diagnostics) {
        var simpleName = annotation.getNameAsString();
        var annotationType = switch (simpleName) {
            case "IfDef" -> ConditionalAnnotationType.IF_DEF;
            case "IfNotDef" -> ConditionalAnnotationType.IF_NOT_DEF;
            case "ElseIfDef" -> ConditionalAnnotationType.ELSE_IF_DEF;
            case "ElseDef" -> ConditionalAnnotationType.ELSE_DEF;
            default -> null;
        };
        if (annotationType == null) {
            return null;
        }

        var expression = extractAttribute(annotation, "value", "");
        var group = extractAttribute(annotation, "group", "");
        var range = buildSourceRange(node);

        return new ConditionalElement(annotationType, expression, group, kind, node, range);
    }

    private String extractAttribute(AnnotationExpr annotation, String attributeName, String defaultValue) {
        // handle @IfDef("VALUE") — single-value shorthand
        if (annotation.isSingleMemberAnnotationExpr()) {
            if ("value".equals(attributeName)) {
                var expr = annotation.asSingleMemberAnnotationExpr().getMemberValue();
                return stripQuotes(expr.toString());
            }
            return defaultValue;
        }
        // handle @IfDef(value = "VALUE", group = "GROUP")
        if (annotation.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : annotation.asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals(attributeName)) {
                    return stripQuotes(pair.getValue().toString());
                }
            }
        }
        return defaultValue;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static SourceRange buildSourceRange(Node node) {
        var begin = node.getBegin().orElse(null);
        var end = node.getEnd().orElse(null);
        if (begin != null && end != null) {
            return new SourceRange(begin.line, end.line, begin.column, end.column);
        }
        return new SourceRange(-1, -1, -1, -1);
    }

    private static String resolveEnclosingType(Node node) {
        var parent = node.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof TypeDeclaration<?> type) {
                return type.getFullyQualifiedName().orElse(type.getNameAsString());
            }
            parent = parent.getParentNode().orElse(null);
        }
        return "<unknown>";
    }

    private static ParseResult errorResult(Path sourceFile, DiagnosticCode code, String message) {
        var diagnostic = Diagnostic.of(code, Severity.ERROR, message, sourceFile, -1, -1);
        return new ParseResult(
                new CompilationUnit(), List.of(), Map.of(), List.of(), List.of(diagnostic)
        );
    }
}
