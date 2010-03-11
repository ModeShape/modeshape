/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.xpath;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.jcr.xpath.XPath.And;
import org.modeshape.jcr.xpath.XPath.AnyKindTest;
import org.modeshape.jcr.xpath.XPath.AttributeNameTest;
import org.modeshape.jcr.xpath.XPath.AttributeTest;
import org.modeshape.jcr.xpath.XPath.AxisStep;
import org.modeshape.jcr.xpath.XPath.CommentTest;
import org.modeshape.jcr.xpath.XPath.Comparison;
import org.modeshape.jcr.xpath.XPath.Component;
import org.modeshape.jcr.xpath.XPath.ContextItem;
import org.modeshape.jcr.xpath.XPath.DescendantOrSelf;
import org.modeshape.jcr.xpath.XPath.DocumentTest;
import org.modeshape.jcr.xpath.XPath.ElementTest;
import org.modeshape.jcr.xpath.XPath.FilterStep;
import org.modeshape.jcr.xpath.XPath.FunctionCall;
import org.modeshape.jcr.xpath.XPath.Literal;
import org.modeshape.jcr.xpath.XPath.NameTest;
import org.modeshape.jcr.xpath.XPath.NodeTest;
import org.modeshape.jcr.xpath.XPath.Or;
import org.modeshape.jcr.xpath.XPath.OrderBy;
import org.modeshape.jcr.xpath.XPath.OrderBySpec;
import org.modeshape.jcr.xpath.XPath.ParenthesizedExpression;
import org.modeshape.jcr.xpath.XPath.PathExpression;
import org.modeshape.jcr.xpath.XPath.SchemaAttributeTest;
import org.modeshape.jcr.xpath.XPath.SchemaElementTest;
import org.modeshape.jcr.xpath.XPath.StepExpression;
import org.modeshape.jcr.xpath.XPath.TextTest;

/**
 * 
 */
public class XPathParserTest {

    private TypeSystem typeSystem;
    private XPathParser parser;

    @Before
    public void beforeEach() {
        typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        parser = new XPathParser(typeSystem);
    }

    @After
    public void afterEach() {
        parser = null;
    }

    @Test
    public void shouldParseXPathExpressions() {
        assertParsable("/jcr:root/a/b/c");
        assertParsable("/jcr:root/a/b/c[*]");
        assertParsable("/jcr:root/some[1]/element(nodes, my:type)[2]");
        assertParsable("//element(*,my:type)");
        assertParsable("//element(*,my:type)[@jcr:title='something' and @globalProperty='something else']");
        assertParsable("//element(*,my:type)[@jcr:title | @globalProperty]");
        assertParsable("//element(*,my:type)/(@jcr:title | @globalProperty)");
        assertParsable("//element(*, my:type) order by @my:title");
        assertParsable("//element(*, my:type) [jcr:contains(., 'jcr')] order by jcr:score() descending");
        assertParsable("//element(*, employee)[@secretary and @assistant]");
        assertParsable("//element(*, employee)[@secretary or @assistant]");
    }

    @Test
    public void shouldParseXPathExpressions2() {
        assertParsable("/jcr:root/a/b/c", pathExpr(axisStep(nameTest("jcr", "root")),
                                                   axisStep(nameTest("a")),
                                                   axisStep(nameTest("b")),
                                                   axisStep(nameTest("c"))));
        assertParsable("/jcr:root/a/b/c[*]", pathExpr(axisStep(nameTest("jcr", "root")),
                                                      axisStep(nameTest("a")),
                                                      axisStep(nameTest("b")),
                                                      axisStep(nameTest("c"), wildcard())));
        assertParsable("/jcr:root/some[1]", pathExpr(axisStep(nameTest("jcr", "root")), axisStep(nameTest("some"), literal("1"))));
        assertParsable("/jcr:root/element(*)", pathExpr(axisStep(nameTest("jcr", "root")), axisStep(element(wildcard(),
                                                                                                            wildcard()))));
        assertParsable("/jcr:root/element(name)", pathExpr(axisStep(nameTest("jcr", "root")), axisStep(element(nameTest("name"),
                                                                                                               wildcard()))));
        assertParsable("/jcr:root/element(*, *)", pathExpr(axisStep(nameTest("jcr", "root")), axisStep(element(wildcard(),
                                                                                                               wildcard()))));
        assertParsable("/jcr:root/element(*, my:type)", pathExpr(axisStep(nameTest("jcr", "root")),
                                                                 axisStep(element(wildcard(), nameTest("my", "type")))));
        assertParsable("/jcr:root/element(ex:name, my:type)",
                       pathExpr(axisStep(nameTest("jcr", "root")), axisStep(element(nameTest("ex", "name"),
                                                                                    nameTest("my", "type")))));
        assertParsable("/jcr:root/element(name, my:type)", pathExpr(axisStep(nameTest("jcr", "root")),
                                                                    axisStep(element(nameTest("name"), nameTest("my", "type")))));
        assertParsable("/jcr:root/element(name, type)", pathExpr(axisStep(nameTest("jcr", "root")),
                                                                 axisStep(element(nameTest("name"), nameTest("type")))));
        assertParsable("/jcr:root/some[1]/element(nodes, my:type)[1]", pathExpr(axisStep(nameTest("jcr", "root")),
                                                                                axisStep(nameTest("some"), literal("1")),
                                                                                axisStep(element(nameTest("nodes"),
                                                                                                 nameTest("my", "type")),
                                                                                         literal("1"))));
        assertParsable("/jcr:root/some[1]/element(*, my:type)[1]", pathExpr(axisStep(nameTest("jcr", "root")),
                                                                            axisStep(nameTest("some"), literal("1")),
                                                                            axisStep(element(wildcard(), nameTest("my", "type")),
                                                                                     literal("1"))));
    }

    @Test
    public void shouldParseXPathExpressionWithOrderBy() {
        assertParsable("//element(*, my:type) order by @a1,@a2", pathExpr(orderBy(asc(nameTest("a1")), asc(nameTest("a2"))),
                                                                          descendantOrSelf(),
                                                                          axisStep(element(wildcard(), nameTest("my", "type")))));
        assertParsable("//element(*, my:type) order by @p:a1, @a2",
                       pathExpr(orderBy(asc(nameTest("p", "a1")), asc(nameTest("a2"))),
                                descendantOrSelf(),
                                axisStep(element(wildcard(), nameTest("my", "type")))));
        assertParsable("/jcr:root/element(name, my:type) order by @p:a1", pathExpr(orderBy(asc(nameTest("p", "a1"))),
                                                                                   axisStep(nameTest("jcr", "root")),
                                                                                   axisStep(element(nameTest("name"),
                                                                                                    nameTest("my", "type")))));
        assertParsable("/jcr:root order by @p:a1", pathExpr(orderBy(asc(nameTest("p", "a1"))), axisStep(nameTest("jcr", "root"))));
    }

    @Ignore
    @Test
    public void shouldParseXPathExpressionsThatCombineSeparateExpressions() {
        assertParsable("/jcr:root/a/b/c union /jcr:root/c/d/e");
        assertParsable("/jcr:root/a/b/c union /jcr:root/c/d/e intersect /jcr:root/f/g/h");
    }

    @Test
    public void shouldFailToParseInvalidXPathExpressions() {
        assertNotParsable("//"); // http://www.w3.org/XML/2007/qt-errata/xpath20-errata.html#E3
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Path expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePathExpressionWithSpaceInPath() {
        assertThat(parser.parsePathExpr(tokenize("/a/b/c_x0020_d")), is(pathExpr(axisStep(nameTest("a")),
                                                                                 axisStep(nameTest("b")),
                                                                                 axisStep(nameTest("c d")))));
    }

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithNameTestAfter() {
        assertThat(parser.parsePathExpr(tokenize("//b:c")), is(pathExpr(descendantOrSelf(), axisStep(nameTest("b", "c")))));
    }

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNamePathPredicate() {
        assertThat(parser.parsePathExpr(tokenize("//.[c]")), is(pathExpr(descendantOrSelf(), filterStep(contextItem(),
                                                                                                        nameTest("c")))));
    }

    @Test
    public void shouldParsePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNumericLiteralPredicate() {
        assertThat(parser.parsePathExpr(tokenize("//.[3]")), is(pathExpr(descendantOrSelf(), filterStep(contextItem(),
                                                                                                        literal("3")))));
    }

    @Test
    public void shouldParsePathExpressionWithNameTestsAndWildcard() {
        assertThat(parser.parsePathExpr(tokenize("/jcr:root/a/b/*")), is(pathExpr(axisStep(nameTest("jcr", "root")),
                                                                                  axisStep(nameTest("a")),
                                                                                  axisStep(nameTest("b")),
                                                                                  axisStep(wildcard()))));
    }

    @Test
    public void shouldParsePathExpressionWithNameTestsAndWildcardAndPropertyExistence() {
        assertThat(parser.parsePathExpr(tokenize("/jcr:root/a/b/*[@prop]")),
                   is(pathExpr(axisStep(nameTest("jcr", "root")),
                               axisStep(nameTest("a")),
                               axisStep(nameTest("b")),
                               axisStep(wildcard(), attributeNameTest(nameTest("prop"))))));
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
                   is(relativePathExpr(axisStep(nameTest("a")), descendantOrSelf(), filterStep(contextItem(), nameTest("c")))));
    }

    @Test
    public void shouldParseRelativePathExpressionWithAbbreviatedDescendantOrSelfWithRelativeNumericLiteralPredicate() {
        assertThat(parser.parseRelativePathExpr(tokenize("a//.[3]")),
                   is(relativePathExpr(axisStep(nameTest("a")), descendantOrSelf(), filterStep(contextItem(), literal("3")))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Step Expression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseStepExpressionFromParenthesizedLiteral() {
        assertThat(parser.parseStepExpr(tokenize("('foo')")), is((StepExpression)filterStep(paren(literal("foo")))));
    }

    @Test
    public void shouldParseStepExpressionFromQuotedStringLiteral() {
        assertThat(parser.parseStepExpr(tokenize("'foo'")), is((StepExpression)filterStep(literal("foo"))));
    }

    @Test
    public void shouldParseStepExpressionFromFunctionCallWithUnqualifiedName() {
        assertThat(parser.parseStepExpr(tokenize("element2(*,*)")),
                   is((StepExpression)filterStep(functionCall(nameTest("element2"), wildcard(), wildcard()))));
    }

    @Test
    public void shouldParseStepExpressionFromFunctionCallWithQualifiedName() {
        assertThat(parser.parseStepExpr(tokenize("foo:bar(*)")),
                   is((StepExpression)filterStep(functionCall(nameTest("foo", "bar"), wildcard()))));
    }

    @Test
    public void shouldParseStepExpressionFromQualifiedNameWithPredicate() {
        assertThat(parser.parseStepExpr(tokenize("foo:bar[3]")),
                   is((StepExpression)axisStep(nameTest("foo", "bar"), literal("3"))));
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
        assertThat(parser.parseAxisStep(tokenize("*:name[3]")), is(axisStep(nameTest(null, "name"), literal("3"))));
    }

    @Test
    public void shouldParseAxisStepFromWildcardLocalNameWithPredicates() {
        assertThat(parser.parseAxisStep(tokenize("*[3]")), is(axisStep(wildcard(), literal("3"))));
    }

    @Test
    public void shouldParseAxisStepFromWildcardPrefixAndLocalNameWithPredicates() {
        assertThat(parser.parseAxisStep(tokenize("*:*[3]")), is(axisStep(wildcard(), literal("3"))));
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
    public void shouldParsePredicatesWithAttributeEqualsStringLiteral() {
        assertThat(parser.parsePredicates(tokenize("[@jcr:title='something']")),
                   is(predicates(comparison(attributeNameTest(nameTest("jcr", "title")), Operator.EQUAL_TO, literal("something")))));
    }

    @Test
    public void shouldParsePredicatesWithAttributeLessThanIntegerLiteral() {
        assertThat(parser.parsePredicates(tokenize("[@ex:age<3]")), is(predicates(comparison(attributeNameTest(nameTest("ex",
                                                                                                                        "age")),
                                                                                             Operator.LESS_THAN,
                                                                                             literal("3")))));
    }

    @Test
    public void shouldParsePredicatesWithAttributeLikeStringLiteral() {
        assertThat(parser.parsePredicates(tokenize("[jcr:like(@jcr:title,'%something%')]")),
                   is(predicates(functionCall(nameTest("jcr", "like"),
                                              attributeNameTest(nameTest("jcr", "title")),
                                              literal("%something%")))));
    }

    @Test
    public void shouldParsePredicatesWithAndedExpressions() {
        assertThat(parser.parsePredicates(tokenize("[@ex:age<3 and jcr:like(@jcr:title,'%something%')]")),
                   is(predicates(and(comparison(attributeNameTest(nameTest("ex", "age")), Operator.LESS_THAN, literal("3")),
                                     functionCall(nameTest("jcr", "like"),
                                                  attributeNameTest(nameTest("jcr", "title")),
                                                  literal("%something%"))))));
    }

    @Test
    public void shouldParsePredicatesWithOredExpressions() {
        assertThat(parser.parsePredicates(tokenize("[@ex:age<3 or jcr:like(@jcr:title,'%something%')]")),
                   is(predicates(or(comparison(attributeNameTest(nameTest("ex", "age")), Operator.LESS_THAN, literal("3")),
                                    functionCall(nameTest("jcr", "like"),
                                                 attributeNameTest(nameTest("jcr", "title")),
                                                 literal("%something%"))))));
    }

    @Test
    public void shouldParsePredicatesWithMultipleSeparatePredicates() {
        assertThat(parser.parsePredicates(tokenize("[@ex:age<3][jcr:like(@jcr:title,'%something%')]")),
                   is(predicates(comparison(attributeNameTest(nameTest("ex", "age")), Operator.LESS_THAN, literal("3")),
                                 functionCall(nameTest("jcr", "like"),
                                              attributeNameTest(nameTest("jcr", "title")),
                                              literal("%something%")))));
    }

    @Test
    public void shouldParsePredicatesWhenThereIsOnlyOnePredicate() {
        assertThat(parser.parsePredicates(tokenize("[foo]")), is(predicates(nameTest("foo"))));
    }

    @Test
    public void shouldParsePredicatesWhenThereAreMultiplePredicates() {
        assertThat(parser.parsePredicates(tokenize("['foo']['bar']")), is(predicates(literal("foo"), literal("bar"))));
        assertThat(parser.parsePredicates(tokenize("[foo][bar]")), is(predicates(nameTest("foo"), nameTest("bar"))));
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
        assertThat(parser.parseParenthesizedExpr(tokenize("('foo')")), is(paren(literal("foo"))));
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
        assertThat(func.getParameters().get(0), is((Component)literal("foo")));
    }

    @Test
    public void shouldParseFunctionCallWithOneUnquotedStringLiteralParameter() {
        FunctionCall func = parser.parseFunctionCall(tokenize("a(foo)"));
        assertThat(func, is(notNullValue()));
        assertThat(func.getName(), is(nameTest("a")));
        assertThat(func.getParameters().size(), is(1));
        assertThat(func.getParameters().get(0), is((Component)nameTest("foo")));
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
        assertThat(parser.parseAnyKindTest(tokenize("node()")), is(instanceOf(AnyKindTest.class)));
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
        assertThat(parser.parseCommentTest(tokenize("comment()")), is(instanceOf(CommentTest.class)));
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
        assertThat(parser.parseTextTest(tokenize("text()")), is(instanceOf(TextTest.class)));
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

    // ----------------------------------------------------------------------------------------------------------------
    // order-by
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseOrderByOneAttributeNameClause() {
        assertThat(parser.parseOrderBy(tokenize("order by @a1")), is(orderBy(asc(nameTest("a1")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 ascending")), is(orderBy(asc(nameTest("a1")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 descending")), is(orderBy(desc(nameTest("a1")))));
        assertThat(parser.parseOrderBy(tokenize("order by @pre:a1")), is(orderBy(asc(nameTest("pre", "a1")))));
        assertThat(parser.parseOrderBy(tokenize("order by @pre:a1 ascending")), is(orderBy(asc(nameTest("pre", "a1")))));
        assertThat(parser.parseOrderBy(tokenize("order by @pre:a1 descending")), is(orderBy(desc(nameTest("pre", "a1")))));
    }

    @Test
    public void shouldParseOrderByMultipleAttributeNameClauses() {
        assertThat(parser.parseOrderBy(tokenize("order by @a1,@a2")), is(orderBy(asc(nameTest("a1")), asc(nameTest("a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 ascending , @a2 ascending")), is(orderBy(asc(nameTest("a1")),
                                                                                                       asc(nameTest("a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 descending, @a2 ascending")), is(orderBy(desc(nameTest("a1")),
                                                                                                       asc(nameTest("a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 ascending , @a2 descending")), is(orderBy(asc(nameTest("a1")),
                                                                                                        desc(nameTest("a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 descending, @a2 descending")), is(orderBy(desc(nameTest("a1")),
                                                                                                        desc(nameTest("a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @pre:a1, @pre:a2")), is(orderBy(asc(nameTest("pre", "a1")),
                                                                                          asc(nameTest("pre", "a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 ascending, @pre:a2 ascending")), is(orderBy(asc(nameTest("a1")),
                                                                                                          asc(nameTest("pre",
                                                                                                                       "a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 descending, @pre:a2 ascending")), is(orderBy(desc(nameTest("a1")),
                                                                                                           asc(nameTest("pre",
                                                                                                                        "a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 ascending, @pre:a2 descending")), is(orderBy(asc(nameTest("a1")),
                                                                                                           desc(nameTest("pre",
                                                                                                                         "a2")))));
        assertThat(parser.parseOrderBy(tokenize("order by @a1 descending, @pre:a2 descending")),
                   is(orderBy(desc(nameTest("a1")), desc(nameTest("pre", "a2")))));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected XPath.OrderBy orderBy( OrderBySpec... specs ) {
        return new XPath.OrderBy(Arrays.asList(specs));
    }

    protected OrderBySpec asc( FunctionCall scoreFunction ) {
        return new OrderBySpec(Order.ASCENDING, scoreFunction);
    }

    protected OrderBySpec desc( FunctionCall scoreFunction ) {
        return new OrderBySpec(Order.DESCENDING, scoreFunction);
    }

    protected OrderBySpec asc( NameTest attributeName ) {
        return new OrderBySpec(Order.ASCENDING, attributeName);
    }

    protected OrderBySpec desc( NameTest attributeName ) {
        return new OrderBySpec(Order.DESCENDING, attributeName);
    }

    protected TokenStream tokenize( String xpath ) {
        Tokenizer tokenizer = new XPathParser.XPathTokenizer(false); // skip comments
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
        return new PathExpression(false, steps, null);
    }

    protected PathExpression pathExpr( List<StepExpression> steps,
                                       OrderBy orderBy ) {
        return new PathExpression(false, steps, orderBy);
    }

    protected PathExpression pathExpr( OrderBy orderBy,
                                       StepExpression... steps ) {
        return pathExpr(Arrays.asList(steps), orderBy);
    }

    protected PathExpression relativePathExpr( StepExpression... steps ) {
        return relativePathExpr(Arrays.asList(steps));
    }

    protected PathExpression relativePathExpr( List<StepExpression> steps ) {
        return new PathExpression(true, steps, null);
    }

    protected PathExpression relativePathExpr( List<StepExpression> steps,
                                               OrderBy orderBy ) {
        return new PathExpression(true, steps, orderBy);
    }

    protected Literal literal( String value ) {
        return new Literal(value);
    }

    protected ContextItem contextItem() {
        return new ContextItem();
    }

    protected And and( Component left,
                       Component right ) {
        return new And(left, right);
    }

    protected Or or( Component left,
                     Component right ) {
        return new Or(left, right);
    }

    protected Comparison comparison( Component left,
                                     Operator operator,
                                     Component right ) {
        return new Comparison(left, operator, right);
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

    protected ElementTest element( NameTest name ) {
        return new ElementTest(name, null);
    }

    protected ElementTest element( NameTest name,
                                   NameTest type ) {
        return new ElementTest(name, type);
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
        new XPathQueryParser().parseQuery(xpath, typeSystem);
    }

    protected void assertParsable( String xpath,
                                   Component component ) {
        Component actual = parser.parseExpr(tokenize(xpath));
        if (component != null) {
            assertThat(actual, is(component));
        } else {
            assertThat(actual, is(nullValue()));
        }
    }

    protected void assertNotParsable( String xpath ) {
        try {
            new XPathQueryParser().parseQuery(xpath, typeSystem);
            fail("Expected an invalid XPath:  " + xpath);
        } catch (ParsingException e) {
            // expected
        }
    }

}
