/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr.xpath;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.TokenStream;
import org.jboss.dna.common.text.TokenStream.Tokenizer;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.jcr.xpath.XPath.AnyKindTest;
import org.jboss.dna.jcr.xpath.XPath.AttributeNameTest;
import org.jboss.dna.jcr.xpath.XPath.AttributeTest;
import org.jboss.dna.jcr.xpath.XPath.AxisStep;
import org.jboss.dna.jcr.xpath.XPath.CommentTest;
import org.jboss.dna.jcr.xpath.XPath.Component;
import org.jboss.dna.jcr.xpath.XPath.ContextItem;
import org.jboss.dna.jcr.xpath.XPath.DescendantOrSelf;
import org.jboss.dna.jcr.xpath.XPath.DocumentTest;
import org.jboss.dna.jcr.xpath.XPath.ElementTest;
import org.jboss.dna.jcr.xpath.XPath.FilterStep;
import org.jboss.dna.jcr.xpath.XPath.FunctionCall;
import org.jboss.dna.jcr.xpath.XPath.Literal;
import org.jboss.dna.jcr.xpath.XPath.NameTest;
import org.jboss.dna.jcr.xpath.XPath.NodeTest;
import org.jboss.dna.jcr.xpath.XPath.ParenthesizedExpression;
import org.jboss.dna.jcr.xpath.XPath.PathExpression;
import org.jboss.dna.jcr.xpath.XPath.SchemaAttributeTest;
import org.jboss.dna.jcr.xpath.XPath.SchemaElementTest;
import org.jboss.dna.jcr.xpath.XPath.StepExpression;
import org.jboss.dna.jcr.xpath.XPath.TextTest;
import org.jboss.dna.jcr.xpath.XPathParser.XPathTokenizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class XPathParserTest {

    private ExecutionContext context;
    private XPathParser.Parser parser;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        parser = new XPathParser.Parser(context);
    }

    @After
    public void afterEach() {
        parser = null;
    }

    @Test
    public void shouldParseXPathExpressions() {
        assertParsable("/jcr:root/a/b/c");
        assertParsable("/jcr:root/a/b/c[*]");
        assertParsable("/jcr:root/some[1]/element(nodes, my:type)[1]");
        assertParsable("//element(*,my:type)");
        assertParsable("//element(*,my:type)[@jcr:title='something' and @globalProperty='something else']");
        assertParsable("//element(*,my:type)[@jcr:title | @globalProperty]");
        assertParsable("//element(*, my:type) order by @my:title");
        assertParsable("//element(*, my:type) [jcr:contains(., 'jcr')] order by jcr:score() descending");
        assertParsable("//element(*, employee)[@secretary and @assistant]");
    }

    @Test
    public void shouldParseXPathExpressionsThatCombineSeparateExpressions() {
        assertParsable("/jcr:root/a/b/c and /jcr:root/c/d/e");
        assertParsable("/jcr:root/a/b/c and /jcr:root/c/d/e or /jcr:root/f/g/h");
    }

    @Test
    public void shouldFailToParseInvalidXPathExpressions() {
        assertNotParsable("//"); // http://www.w3.org/XML/2007/qt-errata/xpath20-errata.html#E3
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Path expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithNameTestAfter() {
        assertThat(parser.parsePathExpr(tokenize("//b:c")), is(pathExpr(descendantOrSelf(), axisStep(nameTest("b", "c")))));
    }

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNamePathPredicate() {
        assertThat(parser.parsePathExpr(tokenize("//.[c]")), is(pathExpr(descendantOrSelf(),
                                                                         filterStep(contextItem(),
                                                                                    relativePathExpr(axisStep(nameTest("c")))))));
    }

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNumericLiteralPredicate() {
        assertThat(parser.parsePathExpr(tokenize("//.[3]")), is(pathExpr(descendantOrSelf(),
                                                                         filterStep(contextItem(),
                                                                                    relativePathExpr(filterStep(literal("3")))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Relative path expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseRelativePathExpressionWithAbbreviatedDescendantOrSelfWithNameTestAfter() {
        assertThat(parser.parseRelativePathExpr(tokenize("a//b:c")), is(relativePathExpr(axisStep(nameTest("a")),
                                                                                         descendantOrSelf(),
                                                                                         axisStep(nameTest("b", "c")))));
    }

    @Test
    public void shouldParseRelativePathExpressionWithAbbreviatedDescendantOrSelfAtEnd() {
        assertThat(parser.parseRelativePathExpr(tokenize("a//")),
                   is(relativePathExpr(axisStep(nameTest("a")), descendantOrSelf())));
    }

    @Test
    public void shouldParseRelativePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNamePathPredicate() {
        assertThat(parser.parseRelativePathExpr(tokenize("a//.[c]")),
                   is(relativePathExpr(axisStep(nameTest("a")),
                                       descendantOrSelf(),
                                       filterStep(contextItem(), relativePathExpr(axisStep(nameTest("c")))))));
    }

    @Test
    public void shouldParseRelativePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNumericLiteralPredicate() {
        assertThat(parser.parseRelativePathExpr(tokenize("a//.[3]")),
                   is(relativePathExpr(axisStep(nameTest("a")),
                                       descendantOrSelf(),
                                       filterStep(contextItem(), relativePathExpr(filterStep(literal("3")))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Step Expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseStepExpressionFromParenthesizedLiteral() {
        assertThat(parser.parseStepExpr(tokenize("('foo')")),
                   is((StepExpression)filterStep(paren(relativePathExpr(filterStep(literal("foo")))))));
    }

    @Test
    public void shouldParseStepExpressionFromQuotedStringLiteral() {
        assertThat(parser.parseStepExpr(tokenize("'foo'")), is((StepExpression)filterStep(literal("foo"))));
    }

    @Test
    public void shouldParseStepExpressionFromFunctionCallWithUnqualifiedName() {
        assertThat(parser.parseStepExpr(tokenize("element(*,*)")),
                   is((StepExpression)filterStep(functionCall(nameTest("element"),
                                                              relativePathExpr(axisStep(wildcard())),
                                                              relativePathExpr(axisStep(wildcard()))))));
    }

    @Test
    public void shouldParseStepExpressionFromFunctionCallWithQualifiedName() {
        assertThat(parser.parseStepExpr(tokenize("foo:bar(*)")),
                   is((StepExpression)filterStep(functionCall(nameTest("foo", "bar"), relativePathExpr(axisStep(wildcard()))))));
    }

    @Test
    public void shouldParseStepExpressionFromQualifiedNameWithPredicate() {
        assertThat(parser.parseStepExpr(tokenize("foo:bar[3]")),
                   is((StepExpression)axisStep(nameTest("foo", "bar"), relativePathExpr(filterStep(literal("3"))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Axis Step - attribute
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseAxisStepFromAttributeWithNoPrefix() {
        assertThat(parser.parseAxisStep(tokenize("@foo")), is(axisStep(attributeNameTest(nameTest("foo")))));
    }

    @Test
    public void shouldParseAxisStepFromAttributeWithPrefix() {
        assertThat(parser.parseAxisStep(tokenize("@foo:bar")), is(axisStep(attributeNameTest(nameTest("foo", "bar")))));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseAxisStepFromInvalidAttributeName() {
        parser.parseAxisStep(tokenize("@3:invalidName"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Axis Step - name
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseAxisStepFromNameWithNoPrefix() {
        assertThat(parser.parseAxisStep(tokenize("foo")), is(axisStep(nameTest("foo"))));
    }

    @Test
    public void shouldParseAxisStepFromNameWithPrefix() {
        assertThat(parser.parseAxisStep(tokenize("foo:bar")), is(axisStep(nameTest("foo", "bar"))));
    }

    @Test
    public void shouldReturnNullFromParseAxisStepIfInvalidName() {
        assertThat(parser.parseAxisStep(tokenize("3:invalidName")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Axis Step - wildcard
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseAxisStepFromWildcard() {
        assertThat(parser.parseAxisStep(tokenize("*")), is(axisStep(wildcard())));
    }

    @Test
    public void shouldParseAxisStepFromWildcardPrefixAndNonWildcardLocalName() {
        assertThat(parser.parseAxisStep(tokenize("*:foo")), is(axisStep(nameTest(null, "foo"))));
    }

    @Test
    public void shouldParseAxisStepFromWithPrefixAndWildcardLocalName() {
        assertThat(parser.parseAxisStep(tokenize("foo:*")), is(axisStep(nameTest("foo", null))));
    }

    @Test
    public void shouldParseAxisStepFromWildcardPrefixAndNonWildcardLocalNameWithPredicates() {
        assertThat(parser.parseAxisStep(tokenize("*:name[3]")), is(axisStep(nameTest(null, "name"),
                                                                            relativePathExpr(filterStep(literal("3"))))));
    }

    @Test
    public void shouldParseAxisStepFromWildcardLocalNameWithPredicates() {
        assertThat(parser.parseAxisStep(tokenize("*[3]")), is(axisStep(wildcard(), relativePathExpr(filterStep(literal("3"))))));
    }

    @Test
    public void shouldParseAxisStepFromWildcardPrefixAndLocalNameWithPredicates() {
        assertThat(parser.parseAxisStep(tokenize("*:*[3]")), is(axisStep(wildcard(), relativePathExpr(filterStep(literal("3"))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Axis Step - self::, child::, descendant::, descendant-or-self::, following::, following-sibling::, namespace::
    // ----------------------------------------------------------------------------------------------------------------

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedSelfAxis() {
        parser.parseAxisStep(tokenize("self::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedChildAxis() {
        parser.parseAxisStep(tokenize("child::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedDescendantAxis() {
        parser.parseAxisStep(tokenize("descendant::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedDescendantOrSelfAxis() {
        parser.parseAxisStep(tokenize("descendant-or-self::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedFollowingAxis() {
        parser.parseAxisStep(tokenize("following::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedFollowingSiblingAxis() {
        parser.parseAxisStep(tokenize("following-sibling::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedNamespaceAxis() {
        parser.parseAxisStep(tokenize("namespace::x"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Axis Step - parent::, ancestor::, ancestor-or-self::, preceding::, preceding-sibling::
    // ----------------------------------------------------------------------------------------------------------------

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedParentAxis() {
        parser.parseAxisStep(tokenize("parent::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedAncestorAxis() {
        parser.parseAxisStep(tokenize("ancestor::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedAncestorOrSelfAxis() {
        parser.parseAxisStep(tokenize("ancestor-or-self::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedPrecedingAxis() {
        parser.parseAxisStep(tokenize("preceding::x"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNonAbbreviatedPrecedingSiblingAxis() {
        parser.parseAxisStep(tokenize("preceding-sibling::x"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Predicates
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePredicatesWhenThereIsOnlyOnePredicate() {
        assertThat(parser.parsePredicates(tokenize("[foo]")), is(predicates(relativePathExpr(axisStep(nameTest("foo"))))));
    }

    @Test
    public void shouldParsePredicatesWhenThereAreMultiplePredicates() {
        assertThat(parser.parsePredicates(tokenize("['foo']['bar']")),
                   is(predicates(relativePathExpr(filterStep(literal("foo"))), relativePathExpr(filterStep(literal("bar"))))));
        assertThat(parser.parsePredicates(tokenize("[foo][bar]")), is(predicates(relativePathExpr(axisStep(nameTest("foo"))),
                                                                                 relativePathExpr(axisStep(nameTest("bar"))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Context item
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseContextItemExpr() {
        assertThat(parser.parseContextItemExpr(tokenize(".")), is(new ContextItem()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Parenthesized expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseParenthesizedExpression() {
        assertThat(parser.parseParenthesizedExpr(tokenize("('foo')")), is(paren(relativePathExpr(filterStep(literal("foo"))))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Number literal
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNumberLiteralContainingUnquotedIntegralValuesIntoCanonicalStringRepresentation() {
        assertThat(parser.parseNumericLiteral(tokenize("1")), is(literal("1")));
        assertThat(parser.parseNumericLiteral(tokenize("-1")), is(literal("-1")));
        assertThat(parser.parseNumericLiteral(tokenize("0")), is(literal("0")));
        assertThat(parser.parseNumericLiteral(tokenize("+1001")), is(literal("1001")));
    }

    @Test
    public void shouldParseNumberLiteralContainingUnquotedDecimalValuesIntoCanonicalStringRepresentation() {
        assertThat(parser.parseNumericLiteral(tokenize("1.2")), is(literal("1.2")));
        assertThat(parser.parseNumericLiteral(tokenize("-1.2")), is(literal("-1.2")));
        assertThat(parser.parseNumericLiteral(tokenize("0.2")), is(literal("0.2")));
        assertThat(parser.parseNumericLiteral(tokenize("+1001.2")), is(literal("1001.2")));
        assertThat(parser.parseNumericLiteral(tokenize("1.2e10")), is(literal("1.2E10")));
        assertThat(parser.parseNumericLiteral(tokenize("-1.2e10")), is(literal("-1.2E10")));
        assertThat(parser.parseNumericLiteral(tokenize("0.2e10")), is(literal("2.0E9")));
        assertThat(parser.parseNumericLiteral(tokenize("1.2e+10")), is(literal("1.2E10")));
        assertThat(parser.parseNumericLiteral(tokenize("-1.2e+10")), is(literal("-1.2E10")));
        assertThat(parser.parseNumericLiteral(tokenize("0.2e+10")), is(literal("2.0E9")));
        assertThat(parser.parseNumericLiteral(tokenize("1.2e-10")), is(literal("1.2E-10")));
        assertThat(parser.parseNumericLiteral(tokenize("-1.2e-10")), is(literal("-1.2E-10")));
        assertThat(parser.parseNumericLiteral(tokenize("0.2e-10")), is(literal("2.0E-11")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // String literal
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseStringLiteral() {
        assertThat(parser.parseStringLiteral(tokenize("one")), is(literal("one")));
        assertThat(parser.parseStringLiteral(tokenize("'one'")), is(literal("one")));
        assertThat(parser.parseStringLiteral(tokenize("'one word as a quote'")), is(literal("one word as a quote")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Function call
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseFunctionCallWithZeroParameters() {
        FunctionCall func = parser.parseFunctionCall(tokenize("a()"));
        assertThat(func, is(notNullValue()));
        assertThat(func.getName(), is(nameTest("a")));
        assertThat(func.getParameters().isEmpty(), is(true));
    }

    @Test
    public void shouldParseFunctionCallWithOneQuotedStringLiteralParameter() {
        FunctionCall func = parser.parseFunctionCall(tokenize("a('foo')"));
        assertThat(func, is(notNullValue()));
        assertThat(func.getName(), is(nameTest("a")));
        assertThat(func.getParameters().size(), is(1));
        assertThat(func.getParameters().get(0), is((Component)relativePathExpr(filterStep(literal("foo")))));
    }

    @Test
    public void shouldParseFunctionCallWithOneUnquotedStringLiteralParameter() {
        FunctionCall func = parser.parseFunctionCall(tokenize("a(foo)"));
        assertThat(func, is(notNullValue()));
        assertThat(func.getName(), is(nameTest("a")));
        assertThat(func.getParameters().size(), is(1));
        assertThat(func.getParameters().get(0), is((Component)relativePathExpr(axisStep(nameTest("foo")))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // General comparison
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseGeneralComp() {
        assertThat(parser.parseGeneralComp(tokenize("!=")), is(Operator.NOT_EQUAL_TO));
        assertThat(parser.parseGeneralComp(tokenize("=")), is(Operator.EQUAL_TO));
        assertThat(parser.parseGeneralComp(tokenize("<")), is(Operator.LESS_THAN));
        assertThat(parser.parseGeneralComp(tokenize("<=")), is(Operator.LESS_THAN_OR_EQUAL_TO));
        assertThat(parser.parseGeneralComp(tokenize(">")), is(Operator.GREATER_THAN));
        assertThat(parser.parseGeneralComp(tokenize(">=")), is(Operator.GREATER_THAN_OR_EQUAL_TO));
        assertThat(parser.parseGeneralComp(tokenize("<5")), is(Operator.LESS_THAN));
        assertThat(parser.parseGeneralComp(tokenize(">5")), is(Operator.GREATER_THAN));
    }

    @Test
    public void shouldReturnNullFromParseGeneralCompIfOperatorPatternIsNotFound() {
        assertThat(parser.parseGeneralComp(tokenize("name")), is(nullValue()));
        assertThat(parser.parseGeneralComp(tokenize("+")), is(nullValue()));
        assertThat(parser.parseGeneralComp(tokenize("!+")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Name
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseName() {
        assertThat(parser.parseNameTest(tokenize("*")), is(wildcard()));
        assertThat(parser.parseNameTest(tokenize("*:*")), is(wildcard()));
        assertThat(parser.parseNameTest(tokenize("*:name")), is(nameTest(null, "name")));
        assertThat(parser.parseNameTest(tokenize("name:*")), is(nameTest("name", null)));
        assertThat(parser.parseNameTest(tokenize("abc")), is(nameTest("abc")));
        assertThat(parser.parseNameTest(tokenize("abc:def")), is(nameTest("abc", "def")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNameIfNotValidWildcardOrQName() {
        parser.parseNameTest(tokenize("3ABC:def"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // QName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseQName() {
        assertThat(parser.parseQName(tokenize("abc")), is(nameTest("abc")));
        assertThat(parser.parseQName(tokenize("abc:def")), is(nameTest("abc", "def")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQNameIfStartsWithDigit() {
        parser.parseNCName(tokenize("3ABC:def"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // NCName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNCName() {
        assertThat(parser.parseNCName(tokenize("ABCDEFGHIJKLMNOPQRSTUVWXYZ")), is("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertThat(parser.parseNCName(tokenize("abcdefghijklmnopqrstuvwxyz")), is("abcdefghijklmnopqrstuvwxyz"));
        assertThat(parser.parseNCName(tokenize("a0123456789")), is("a0123456789"));
        assertThat(parser.parseNCName(tokenize("a_-3b")), is("a_-3b"));
        assertThat(parser.parseNCName(tokenize("abc:def")), is("abc"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNCNameIfStartsWithDigit() {
        parser.parseNCName(tokenize("3ABC"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // wildcard
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseWildcard() {
        assertThat(parser.parseWildcard(tokenize("*")), is(wildcard()));
        assertThat(parser.parseWildcard(tokenize("*:*")), is(wildcard()));
        assertThat(parser.parseWildcard(tokenize("*:name")), is(nameTest(null, "name")));
        assertThat(parser.parseWildcard(tokenize("name:*")), is(nameTest("name", null)));
    }

    @Test
    public void shouldReturnNullFromParseWildcardIfNotWildcard() {
        assertThat(parser.parseWildcard(tokenize("name")), is(nullValue()));
        assertThat(parser.parseWildcard(tokenize("name:foo")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // kind test
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseKindTest() {
        assertThat(parser.parseKindTest(tokenize("node()")), is(instanceOf(AnyKindTest.class)));
        assertThat(parser.parseKindTest(tokenize("comment()")), is(instanceOf(CommentTest.class)));
        assertThat(parser.parseKindTest(tokenize("text()")), is(instanceOf(TextTest.class)));
        assertThat(parser.parseKindTest(tokenize("document-node(element(foo:bar))")), is(instanceOf(DocumentTest.class)));
        assertThat(parser.parseKindTest(tokenize("element(foo:bar)")), is(instanceOf(ElementTest.class)));
        assertThat(parser.parseKindTest(tokenize("schema-element(foo:bar)")), is(instanceOf(SchemaElementTest.class)));
        assertThat(parser.parseKindTest(tokenize("attribute(foo:bar)")), is(instanceOf(AttributeTest.class)));
        assertThat(parser.parseKindTest(tokenize("schema-attribute(foo:bar)")), is(instanceOf(SchemaAttributeTest.class)));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // node
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseAnyKindTest() {
        assertThat(parser.parseAnyKindTest(tokenize("node()")), is(notNullValue()));
    }

    @Test
    public void shouldReturnNullFromParseAnyKindTestIfNotFollowedByOpenAndCloseParenthesis() {
        assertThat(parser.parseAnyKindTest(tokenize("node x )")), is(nullValue()));
        assertThat(parser.parseAnyKindTest(tokenize("node(x )")), is(nullValue()));
        assertThat(parser.parseAnyKindTest(tokenize("node(x")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // comment
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseCommentTest() {
        assertThat(parser.parseCommentTest(tokenize("comment()")), is(notNullValue()));
    }

    @Test
    public void shouldReturnNullFromParseCommentTestIfNotFollowedByOpenAndCloseParenthesis() {
        assertThat(parser.parseCommentTest(tokenize("comment x )")), is(nullValue()));
        assertThat(parser.parseCommentTest(tokenize("comment(x )")), is(nullValue()));
        assertThat(parser.parseCommentTest(tokenize("comment(x")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // text
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseTextTest() {
        assertThat(parser.parseTextTest(tokenize("text()")), is(notNullValue()));
    }

    @Test
    public void shouldReturnNullFromParseTextTestIfNotFollowedByOpenAndCloseParenthesis() {
        assertThat(parser.parseTextTest(tokenize("text x )")), is(nullValue()));
        assertThat(parser.parseTextTest(tokenize("text(x )")), is(nullValue()));
        assertThat(parser.parseTextTest(tokenize("text(x")), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // document-node
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDocumentTest() {
        DocumentTest result = parser.parseDocumentTest(tokenize("document-node(element(foo:bar))"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementTest().getElementName(), is(nameTest("foo", "bar")));
        assertThat(result.getElementTest().getTypeName(), is(wildcard()));
        assertThat(result.getSchemaElementTest(), is(nullValue()));

        result = parser.parseDocumentTest(tokenize("document-node(schema-element(foo))"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getSchemaElementTest().getElementDeclarationName(), is(nameTest("foo")));
        assertThat(result.getElementTest(), is(nullValue()));
    }

    @Test
    public void shouldReturnNullFromParseDocumentTestIfOpenParenthesisIsNotIncluded() {
        assertThat(parser.parseDocumentTest(tokenize("document-node foo")), is(nullValue()));
        assertThat(parser.parseDocumentTest(tokenize("document-node foo")), is(nullValue()));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDocumentTestIfDocumentNodeDoesNotContainElementOrSchemaElement() {
        parser.parseDocumentTest(tokenize("document-node(foo)"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // element
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseElementTest() {
        ElementTest result = parser.parseElementTest(tokenize("element(foo)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(nameTest("foo")));
        assertThat(result.getTypeName(), is(wildcard()));

        result = parser.parseElementTest(tokenize("element(foo:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(wildcard()));

        result = parser.parseElementTest(tokenize("element(foo:bar,baz)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(nameTest("baz")));

        result = parser.parseElementTest(tokenize("element(foo:bar,baz:bam)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(nameTest("baz", "bam")));

        result = parser.parseElementTest(tokenize("element(foo:bar,*)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(wildcard()));

        result = parser.parseElementTest(tokenize("element(*,foo:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(wildcard()));
        assertThat(result.getTypeName(), is(nameTest("foo", "bar")));

        result = parser.parseElementTest(tokenize("element(*,*)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementName(), is(wildcard()));
        assertThat(result.getTypeName(), is(wildcard()));
    }

    @Test
    public void shouldReturnNullFromParseElementTestIfOpenParenthesisIsNotIncluded() {
        ElementTest result = parser.parseElementTest(tokenize("attribute foo"));
        assertThat(result, is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // schema-element
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseSchemaElementTest() {
        SchemaElementTest result = parser.parseSchemaElementTest(tokenize("schema-element(foo)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementDeclarationName(), is(nameTest("foo")));

        result = parser.parseSchemaElementTest(tokenize("schema-element(foo:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementDeclarationName(), is(nameTest("foo", "bar")));

        result = parser.parseSchemaElementTest(tokenize("schema-element(*)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getElementDeclarationName(), is(wildcard()));
    }

    @Test
    public void shouldReturnNullFromParseSchemaElementTestIfOpenParenthesisIsNotIncluded() {
        SchemaElementTest result = parser.parseSchemaElementTest(tokenize("schema-element foo"));
        assertThat(result, is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // attribute
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseAttributeTest() {
        AttributeTest result = parser.parseAttributeTest(tokenize("attribute(foo)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(nameTest("foo")));
        assertThat(result.getTypeName(), is(wildcard()));

        result = parser.parseAttributeTest(tokenize("attribute(foo:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(wildcard()));

        result = parser.parseAttributeTest(tokenize("attribute(foo:bar,baz)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(nameTest("baz")));

        result = parser.parseAttributeTest(tokenize("attribute(foo:bar,baz:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(nameTest("baz", "bar")));

        result = parser.parseAttributeTest(tokenize("attribute(*,baz:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(wildcard()));
        assertThat(result.getTypeName(), is(nameTest("baz", "bar")));

        result = parser.parseAttributeTest(tokenize("attribute(foo:bar,*)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeName(), is(nameTest("foo", "bar")));
        assertThat(result.getTypeName(), is(wildcard()));
    }

    @Test
    public void shouldReturnNullFromParseAttributeTestIfOpenParenthesisIsNotIncluded() {
        AttributeTest result = parser.parseAttributeTest(tokenize("attribute foo"));
        assertThat(result, is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // schema-attribute
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseSchemaAttributeTest() {
        SchemaAttributeTest result = parser.parseSchemaAttributeTest(tokenize("schema-attribute(foo)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeDeclarationName(), is(nameTest("foo")));

        result = parser.parseSchemaAttributeTest(tokenize("schema-attribute(foo:bar)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeDeclarationName(), is(nameTest("foo", "bar")));

        result = parser.parseSchemaAttributeTest(tokenize("schema-attribute(*)"));
        assertThat(result, is(notNullValue()));
        assertThat(result.getAttributeDeclarationName(), is(wildcard()));
    }

    @Test
    public void shouldReturnNullFromParseSchemaAttributeTestIfOpenParenthesisIsNotIncluded() {
        SchemaAttributeTest result = parser.parseSchemaAttributeTest(tokenize("schema-attribute foo"));
        assertThat(result, is(nullValue()));
    }

    protected TokenStream tokenize( String xpath ) {
        Tokenizer tokenizer = new XPathTokenizer(false); // skip comments
        return new TokenStream(xpath, tokenizer, true).start(); // case sensitive!!
    }

    protected DescendantOrSelf descendantOrSelf() {
        return new DescendantOrSelf();
    }

    protected AxisStep axisStep( NodeTest nodeTest,
                                 Component... predicates ) {
        return new AxisStep(nodeTest, predicates(predicates));
    }

    protected FilterStep filterStep( Component primaryExpression,
                                     Component... predicates ) {
        return new FilterStep(primaryExpression, predicates(predicates));
    }

    protected PathExpression pathExpr( StepExpression... steps ) {
        return pathExpr(Arrays.asList(steps));
    }

    protected PathExpression pathExpr( List<StepExpression> steps ) {
        return new PathExpression(false, steps);
    }

    protected PathExpression relativePathExpr( StepExpression... steps ) {
        return relativePathExpr(Arrays.asList(steps));
    }

    protected PathExpression relativePathExpr( List<StepExpression> steps ) {
        return new PathExpression(true, steps);
    }

    protected Literal literal( String value ) {
        return new Literal(value);
    }

    protected ContextItem contextItem() {
        return new ContextItem();
    }

    protected List<Component> predicates( Component... predicates ) {
        return Arrays.asList(predicates);
    }

    protected ParenthesizedExpression paren( Component value ) {
        return new ParenthesizedExpression(value);
    }

    protected AttributeNameTest attributeNameTest( NameTest nameTest ) {
        return new AttributeNameTest(nameTest);
    }

    protected AttributeTest attribute( NameTest name ) {
        return new AttributeTest(name, null);
    }

    protected AttributeTest attribute( NameTest name,
                                       NameTest type ) {
        return new AttributeTest(name, type);
    }

    protected FunctionCall functionCall( NameTest name,
                                         Component... parameters ) {
        return new FunctionCall(name, Arrays.asList(parameters));
    }

    protected NameTest wildcard() {
        return new NameTest(null, null);
    }

    protected NameTest nameTest( String localPart ) {
        return new NameTest(null, localPart);
    }

    protected NameTest nameTest( String prefix,
                                 String local ) {
        return new NameTest(prefix, local);
    }

    protected void assertParsable( String xpath ) {
        new XPathParser().parseQuery(xpath, context);
    }

    protected void assertNotParsable( String xpath ) {
        try {
            new XPathParser().parseQuery(xpath, context);
            fail("Expected an invalid XPath:  " + xpath);
        } catch (ParsingException e) {
            // expected
        }
    }

}
