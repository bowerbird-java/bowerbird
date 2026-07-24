package io.github.bowerbird.java.core.expression;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionParserTest {

    @Test
    void parse_singleIdentifier_producesIdentifierNode() {
        var node = ExpressionParser.parse("DEBUG");
        assertThat(node).isInstanceOf(ExprNode.Identifier.class);
        assertThat(((ExprNode.Identifier) node).name()).isEqualTo("DEBUG");
    }

    @Test
    void parse_negation_producesNotNode() {
        var node = ExpressionParser.parse("!DEBUG");
        assertThat(node).isInstanceOf(ExprNode.Not.class);
        var not = (ExprNode.Not) node;
        assertThat(not.operand()).isInstanceOf(ExprNode.Identifier.class);
    }

    @Test
    void parse_doubleNegation_producesNestedNotNodes() {
        var node = ExpressionParser.parse("!!DEBUG");
        assertThat(node).isInstanceOf(ExprNode.Not.class);
        var outer = (ExprNode.Not) node;
        assertThat(outer.operand()).isInstanceOf(ExprNode.Not.class);
        var inner = (ExprNode.Not) outer.operand();
        assertThat(inner.operand()).isInstanceOf(ExprNode.Identifier.class);
    }

    @Test
    void parse_andExpression_producesAndNode() {
        var node = ExpressionParser.parse("A && B");
        assertThat(node).isInstanceOf(ExprNode.And.class);
        var and = (ExprNode.And) node;
        assertThat(and.left()).isInstanceOf(ExprNode.Identifier.class);
        assertThat(and.right()).isInstanceOf(ExprNode.Identifier.class);
    }

    @Test
    void parse_orExpression_producesOrNode() {
        var node = ExpressionParser.parse("A || B");
        assertThat(node).isInstanceOf(ExprNode.Or.class);
    }

    @Test
    void parse_andHasHigherPrecedenceThanOr_producesCorrectTree() {
        // A || B && C should parse as A || (B && C)
        var node = ExpressionParser.parse("A || B && C");
        assertThat(node).isInstanceOf(ExprNode.Or.class);
        var or = (ExprNode.Or) node;
        assertThat(or.left()).isInstanceOf(ExprNode.Identifier.class);
        assertThat(((ExprNode.Identifier) or.left()).name()).isEqualTo("A");
        assertThat(or.right()).isInstanceOf(ExprNode.And.class);
    }

    @Test
    void parse_parenthesesOverridePrecedence_producesCorrectTree() {
        // (A || B) && C should parse as (A || B) && C
        var node = ExpressionParser.parse("(A || B) && C");
        assertThat(node).isInstanceOf(ExprNode.And.class);
        var and = (ExprNode.And) node;
        assertThat(and.left()).isInstanceOf(ExprNode.Or.class);
        assertThat(and.right()).isInstanceOf(ExprNode.Identifier.class);
    }

    @Test
    void parse_chainedAnd_isLeftAssociative() {
        // A && B && C should parse as (A && B) && C
        var node = ExpressionParser.parse("A && B && C");
        assertThat(node).isInstanceOf(ExprNode.And.class);
        var outer = (ExprNode.And) node;
        assertThat(outer.left()).isInstanceOf(ExprNode.And.class);
        assertThat(outer.right()).isInstanceOf(ExprNode.Identifier.class);
        assertThat(((ExprNode.Identifier) outer.right()).name()).isEqualTo("C");
    }

    @Test
    void parse_complexExpression_producesCorrectTree() {
        // !(A && B) || C
        var node = ExpressionParser.parse("!(A && B) || C");
        assertThat(node).isInstanceOf(ExprNode.Or.class);
        var or = (ExprNode.Or) node;
        assertThat(or.left()).isInstanceOf(ExprNode.Not.class);
        var not = (ExprNode.Not) or.left();
        assertThat(not.operand()).isInstanceOf(ExprNode.And.class);
    }

    @Test
    void parse_unmatchedLeftParen_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("(A && B"))
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("expected closing ')'");
    }

    @Test
    void parse_unmatchedRightParen_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("A && B)"))
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("unexpected token after expression");
    }

    @Test
    void parse_trailingOperator_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("A &&"))
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("expected identifier or '('");
    }

    @Test
    void parse_leadingOperator_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("&& A"))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    void parse_emptyParens_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("()"))
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("expected identifier or '('");
    }

    @Test
    void parse_consecutiveIdentifiers_throwsException() {
        assertThatThrownBy(() -> ExpressionParser.parse("A B"))
                .isInstanceOf(ExpressionParseException.class)
                .hasMessageContaining("unexpected token after expression");
    }
}
