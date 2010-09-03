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
package org.modeshape.graph.query.parse;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.ChildNode;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DescendantNode;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.Subquery;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.UpperCase;
import org.modeshape.graph.query.model.FullTextSearch.Conjunction;
import org.modeshape.graph.query.model.FullTextSearch.Disjunction;
import org.modeshape.graph.query.model.FullTextSearch.Term;

/**
 * 
 */
public class SqlQueryParserTest {

    private TypeSystem typeSystem;
    private SqlQueryParser parser;

    @Before
    public void beforeEach() {
        typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        parser = new SqlQueryParser();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseQuery
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNominalQueries() {
        parse("SELECT * FROM tableA");
        parse("SELECT column1 FROM tableA");
        parse("SELECT tableA.column1 FROM tableA");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA INNER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA LEFT OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA RIGHT OUTER JOIN tableB ON tableA.id = tableB.id");
    }

    @Test
    public void shouldParseQueriesWithNonSqlColumnNames() {
        parse("SELECT * FROM [dna:tableA]");
        parse("SELECT [jcr:column1] FROM [dna:tableA]");
        parse("SELECT 'jcr:column1' FROM 'dna:tableA'");
        parse("SELECT \"jcr:column1\" FROM \"dna:tableA\"");
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldParseQueriesWithSubqueries() {
        parse("SELECT * FROM tableA WHERE PATH() LIKE (SELECT path FROM tableB)");
        parse("SELECT * FROM tableA WHERE PATH() LIKE (SELECT path FROM tableB) AND tableA.propX = 'foo'");
        parse("SELECT * FROM tableA WHERE PATH() LIKE (((SELECT path FROM tableB)))");
        parse("SELECT * FROM tableA WHERE PATH() LIKE (SELECT path FROM tableB WHERE prop < 2)");
        parse("SELECT * FROM tableA WHERE PATH() IN (SELECT path FROM tableB) AND tableA.propX = 'foo'");
        parse("SELECT * FROM tableA WHERE PATH() NOT IN (SELECT path FROM tableB) AND tableA.propX = 'foo'");
    }

    @Test
    public void shouldParseQueriesSelectingFromAllTables() {
        parse("SELECT * FROM __AllTables__");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQueriesWithNoFromClause() {
        parse("SELECT 'jcr:column1'");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQueriesWithIncompleteFromClause() {
        parse("SELECT 'jcr:column1' FROM  ");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQueriesWithUnmatchedSingleQuoteCharacters() {
        parse("SELECT 'jcr:column1' FROM \"dna:tableA'");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQueriesWithUnmatchedDoubleQuoteCharacters() {
        parse("SELECT \"jcr:column1' FROM \"dna:tableA\"");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseQueriesWithUnmatchedBracketQuoteCharacters() {
        parse("SELECT [jcr:column1' FROM [dna:tableA]");
    }

    @Test
    public void shouldParseQueriesWithSelectStar() {
        parse("SELECT * FROM tableA");
        parse("SELECT tableA.* FROM tableA");
        parse("SELECT tableA.column1, tableB.* FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.*, tableB.column2 FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.*, tableB.* FROM tableA JOIN tableB ON tableA.id = tableB.id");
    }

    @Test
    public void shouldParseQueriesWithAllKindsOfJoins() {
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA INNER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA LEFT OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA RIGHT OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA FULL OUTER JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT tableA.column1, tableB.column2 FROM tableA CROSS JOIN tableB ON tableA.id = tableB.id");
    }

    @Test
    public void shouldParseQueriesWithMultipleJoins() {
        parse("SELECT * FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT * FROM tableA JOIN tableB ON tableA.id = tableB.id JOIN tableC ON tableA.id2 = tableC.id2");
    }

    @Test
    public void shouldParseQueriesWithEquiJoinCriteria() {
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON tableA.id = tableB.id");
        parse("SELECT * FROM tableA JOIN tableB ON tableA.id = tableB.id JOIN tableC ON tableA.id2 = tableC.id2");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseEquiJoinCriteriaMissingPropertyName() {
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON tableA = tableB.id");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseEquiJoinCriteriaMissingTableName() {
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON column1 = tableB.id");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseEquiJoinCriteriaMissingEquals() {
        parse("SELECT tableA.column1, tableB.column2 FROM tableA JOIN tableB ON column1 tableB.id");
    }

    @Test
    public void shouldParseQueriesOnMultpleLines() {
        parse("SELECT * \nFROM tableA");
        parse("SELECT \ncolumn1 \nFROM tableA");
        parse("SELECT \ntableA.column1 \nFROM\n tableA");
        parse("SELECT tableA.\ncolumn1, \ntableB.column2 \nFROM tableA JOIN \ntableB ON tableA.id \n= tableB.\nid");
    }

    @Test
    public void shouldParseQueriesThatUseDifferentCaseForKeywords() {
        parse("select * from tableA");
        parse("SeLeCt * from tableA");
        parse("select column1 from tableA");
        parse("select tableA.column1 from tableA");
        parse("select tableA.column1, tableB.column2 from tableA join tableB on tableA.id = tableB.id");
    }

    @Test
    public void shouldParseUnionQueries() {
        parse("SELECT * FROM tableA UNION SELECT * FROM tableB");
        parse("SELECT * FROM tableA UNION ALL SELECT * FROM tableB");
        parse("SELECT * FROM tableA UNION SELECT * FROM tableB UNION SELECT * FROM tableC");
        parse("SELECT * FROM tableA UNION ALL SELECT * FROM tableB UNION SELECT * FROM tableC");
    }

    @Test
    public void shouldParseIntersectQueries() {
        parse("SELECT * FROM tableA INTERSECT SELECT * FROM tableB");
        parse("SELECT * FROM tableA INTERSECT ALL SELECT * FROM tableB");
        parse("SELECT * FROM tableA INTERSECT SELECT * FROM tableB INTERSECT SELECT * FROM tableC");
        parse("SELECT * FROM tableA INTERSECT ALL SELECT * FROM tableB INTERSECT ALL SELECT * FROM tableC");
    }

    @Test
    public void shouldParseExceptQueries() {
        parse("SELECT * FROM tableA EXCEPT SELECT * FROM tableB");
        parse("SELECT * FROM tableA EXCEPT ALL SELECT * FROM tableB");
        parse("SELECT * FROM tableA EXCEPT SELECT * FROM tableB EXCEPT SELECT * FROM tableC");
        parse("SELECT * FROM tableA EXCEPT ALL SELECT * FROM tableB EXCEPT ALL SELECT * FROM tableC");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseSetQuery
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // parseSelect
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // parseFrom
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // parseJoinCondition
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // parseWhere
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithValidExpressions() {
        assertParseConstraint("ISSAMENODE('/a/b')");
        assertParseConstraint("ISSAMENODE('/a/b') AND NOT(ISCHILDNODE('/parent'))");
        assertParseConstraint("ISSAMENODE('/a/b') AND (NOT(ISCHILDNODE('/parent')))");
        assertParseConstraint("ISSAMENODE('/a/b') AND (NOT(tableA.id < 1234)))");
    }

    protected void assertParseConstraint( String expression ) {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        parser.parseConstraint(tokens(expression), typeSystem, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - between
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithValidBetweenExpressionUsing() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("tableA.id BETWEEN 'lower' AND 'upper'"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(Between.class)));
        Between between = (Between)constraint;
        assertThat(between.isLowerBoundIncluded(), is(true));
        assertThat(between.isUpperBoundIncluded(), is(true));
        assertThat(between.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue operand = (PropertyValue)between.operand();
        assertThat(operand.selectorName(), is(selector.name()));
        assertThat(operand.propertyName(), is("id"));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat((Literal)between.lowerBound(), is(literal("lower")));
        assertThat((Literal)between.upperBound(), is(literal("upper")));
    }

    @Test
    public void shouldParseConstraintFromStringWithValidBetweenExpressionUsingExclusiveAndExclusive() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("tableA.id BETWEEN 'lower' EXCLUSIVE AND 'upper' EXCLUSIVE"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(Between.class)));
        Between between = (Between)constraint;
        assertThat(between.isLowerBoundIncluded(), is(false));
        assertThat(between.isUpperBoundIncluded(), is(false));
        assertThat(between.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue operand = (PropertyValue)between.operand();
        assertThat(operand.selectorName(), is(selector.name()));
        assertThat(operand.propertyName(), is("id"));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat((Literal)between.lowerBound(), is(literal("lower")));
        assertThat((Literal)between.upperBound(), is(literal("upper")));
    }

    @Test
    public void shouldParseConstraintFromStringWithValidBetweenExpressionUsingInclusiveAndExclusive() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("tableA.id BETWEEN 'lower' AND 'upper' EXCLUSIVE"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(Between.class)));
        Between between = (Between)constraint;
        assertThat(between.isLowerBoundIncluded(), is(true));
        assertThat(between.isUpperBoundIncluded(), is(false));
        assertThat(between.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue operand = (PropertyValue)between.operand();
        assertThat(operand.selectorName(), is(selector.name()));
        assertThat(operand.propertyName(), is("id"));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat((Literal)between.lowerBound(), is(literal("lower")));
        assertThat((Literal)between.upperBound(), is(literal("upper")));
    }

    @Test
    public void shouldParseConstraintFromStringWithValidBetweenExpressionUsingExclusiveAndInclusive() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("tableA.id BETWEEN 'lower' EXCLUSIVE AND 'upper'"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(Between.class)));
        Between between = (Between)constraint;
        assertThat(between.isLowerBoundIncluded(), is(false));
        assertThat(between.isUpperBoundIncluded(), is(true));
        assertThat(between.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue operand = (PropertyValue)between.operand();
        assertThat(operand.selectorName(), is(selector.name()));
        assertThat(operand.propertyName(), is("id"));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat((Literal)between.lowerBound(), is(literal("lower")));
        assertThat((Literal)between.upperBound(), is(literal("upper")));
    }

    @Test
    public void shouldParseConstraintFromStringWithValidNotBetweenExpression() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("tableA.id NOT BETWEEN 'lower' AND 'upper'"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(Not.class)));
        constraint = ((Not)constraint).constraint();
        assertThat(constraint, is(instanceOf(Between.class)));
        Between between = (Between)constraint;
        assertThat(between.isLowerBoundIncluded(), is(true));
        assertThat(between.isUpperBoundIncluded(), is(true));
        assertThat(between.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue operand = (PropertyValue)between.operand();
        assertThat(operand.selectorName(), is(selector.name()));
        assertThat(operand.propertyName(), is("id"));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat(between.lowerBound(), is(instanceOf(Literal.class)));
        assertThat((Literal)between.lowerBound(), is(literal("lower")));
        assertThat((Literal)between.upperBound(), is(literal("upper")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - parentheses
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithOuterParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("( ISSAMENODE('/a/b') )"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b"));
    }

    @Test
    public void shouldParseConstraintFromStringWithMultipleOuterParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("((( ISSAMENODE('/a/b') )))"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b"));
    }

    @Test
    public void shouldParseConstraintFromStringWithParenthesesAndConjunctionAndDisjunctions() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b') OR (ISSAMENODE('/c/d') AND ISSAMENODE('/e/f'))"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(Or.class)));
        Or or = (Or)constraint;

        assertThat(or.left(), is(instanceOf(SameNode.class)));
        SameNode first = (SameNode)or.left();
        assertThat(first.selectorName(), is(selectorName("tableA")));
        assertThat(first.path(), is("/a/b"));

        assertThat(or.right(), is(instanceOf(And.class)));
        And and = (And)or.right();

        assertThat(and.left(), is(instanceOf(SameNode.class)));
        SameNode second = (SameNode)and.left();
        assertThat(second.selectorName(), is(selectorName("tableA")));
        assertThat(second.path(), is("/c/d"));

        assertThat(and.right(), is(instanceOf(SameNode.class)));
        SameNode third = (SameNode)and.right();
        assertThat(third.selectorName(), is(selectorName("tableA")));
        assertThat(third.path(), is("/e/f"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - AND
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithAndExpressionWithNoParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND CONTAINS(p1,term1)"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(And.class)));
        And and = (And)constraint;

        assertThat(and.left(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)and.left();
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));

        assertThat(and.right(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)and.right();
        assertThat(search.selectorName(), is(selectorName("tableA")));
        assertThat(search.propertyName(), is("p1"));
        assertThat(search.fullTextSearchExpression(), is("term1"));
    }

    @Test
    public void shouldParseConstraintFromStringWithMultipleAndExpressions() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND CONTAINS(p1,term1) AND CONTAINS(p2,term2)"),
                                                       typeSystem,
                                                       selector);
        assertThat(constraint, is(instanceOf(And.class)));
        And and = (And)constraint;

        assertThat(and.left(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)and.left();
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));

        assertThat(and.right(), is(instanceOf(And.class)));
        And secondAnd = (And)and.right();

        assertThat(secondAnd.left(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search1 = (FullTextSearch)secondAnd.left();
        assertThat(search1.selectorName(), is(selectorName("tableA")));
        assertThat(search1.propertyName(), is("p1"));
        assertThat(search1.fullTextSearchExpression(), is("term1"));

        assertThat(secondAnd.right(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search2 = (FullTextSearch)secondAnd.right();
        assertThat(search2.selectorName(), is(selectorName("tableA")));
        assertThat(search2.propertyName(), is("p2"));
        assertThat(search2.fullTextSearchExpression(), is("term2"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithAndExpressionWithNoSecondConstraint() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND WHAT THE HECK IS THIS"), typeSystem, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - OR
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithOrExpressionWithNoParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') OR CONTAINS(p1,term1)"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(Or.class)));
        Or or = (Or)constraint;

        assertThat(or.left(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)or.left();
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));

        assertThat(or.right(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)or.right();
        assertThat(search.selectorName(), is(selectorName("tableA")));
        assertThat(search.propertyName(), is("p1"));
        assertThat(search.fullTextSearchExpression(), is("term1"));

    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithOrExpressionWithNoSecondConstraint() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') OR WHAT THE HECK IS THIS"), typeSystem, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - NOT
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithNotSameNodeExpression() {
        Constraint constraint = parser.parseConstraint(tokens("NOT(ISSAMENODE(tableA,'/a/b/c'))"), typeSystem, mock(Source.class));
        assertThat(constraint, is(instanceOf(Not.class)));
        Not not = (Not)constraint;
        assertThat(not.constraint(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)not.constraint();
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithNotConstraintWithOutOpeningParenthesis() {
        parser.parseConstraint(tokens("NOT CONTAINS(propertyA 'term1 term2 -term3')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithNotConstraintWithOutClosingParenthesis() {
        parser.parseConstraint(tokens("NOT( CONTAINS(propertyA 'term1 term2 -term3') BLAH"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - CONTAINS
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithPropertyAndNoSelectorNameOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3')"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.selectorName(), is(selectorName("tableA")));
        assertThat(search.propertyName(), is("propertyA"));
        assertThat(search.fullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithSelectorNameAndProperty() {
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(tableA.propertyA,'term1 term2 -term3')"),
                                                       typeSystem,
                                                       mock(Source.class));
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.selectorName(), is(selectorName("tableA")));
        assertThat(search.propertyName(), is("propertyA"));
        assertThat(search.fullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithSelectorNameAndAnyProperty() {
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(tableA.*,'term1 term2 -term3')"),
                                                       typeSystem,
                                                       mock(Source.class));
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.selectorName(), is(selectorName("tableA")));
        assertThat(search.propertyName(), is(nullValue()));
        assertThat(search.fullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("CONTAINS(propertyA 'term1 term2 -term3')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3' OTHER"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("CONTAINS propertyA,'term1 term2 -term3')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3')"), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISSAMENODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsSameNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c')"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsSameNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE(tableA,'/a/b/c')"), typeSystem, mock(Source.class));
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.selectorName(), is(selectorName("tableA")));
        assertThat(same.path(), is("/a/b/c"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISSAMENODE(tableA '/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISSAMENODE(tableA,'/a/b/c' AND"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISSAMENODE tableA,'/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c')"), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISCHILDNODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsChildNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISCHILDNODE('/a/b/c')"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(ChildNode.class)));
        ChildNode child = (ChildNode)constraint;
        assertThat(child.selectorName(), is(selectorName("tableA")));
        assertThat(child.parentPath(), is("/a/b/c"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsChildNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISCHILDNODE(tableA,'/a/b/c')"), typeSystem, mock(Source.class));
        assertThat(constraint, is(instanceOf(ChildNode.class)));
        ChildNode child = (ChildNode)constraint;
        assertThat(child.selectorName(), is(selectorName("tableA")));
        assertThat(child.parentPath(), is("/a/b/c"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISCHILDNODE(tableA '/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISCHILDNODE(tableA,'/a/b/c' AND"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISCHILDNODE tableA,'/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISCHILDNODE('/a/b/c')"), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISDESCENDANTNODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsDescendantNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISDESCENDANTNODE('/a/b/c')"), typeSystem, selector);
        assertThat(constraint, is(instanceOf(DescendantNode.class)));
        DescendantNode descendant = (DescendantNode)constraint;
        assertThat(descendant.selectorName(), is(selectorName("tableA")));
        assertThat(descendant.ancestorPath(), is("/a/b/c"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsDescendantNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA,'/a/b/c')"),
                                                       typeSystem,
                                                       mock(Source.class));
        assertThat(constraint, is(instanceOf(DescendantNode.class)));
        DescendantNode descendant = (DescendantNode)constraint;
        assertThat(descendant.selectorName(), is(selectorName("tableA")));
        assertThat(descendant.ancestorPath(), is("/a/b/c"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA '/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA,'/a/b/c' AND"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE tableA,'/a/b/c')"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE('/a/b/c')"), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseInClause
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseInClauseFromStringWithSingleValidLiteral() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN ('value1')"), typeSystem);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is((StaticOperand)literal("value1")));
    }

    @Test
    public void shouldParseInClauseFromStringWithTwoValidLiteral() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN ('value1','value2')"), typeSystem);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is((StaticOperand)literal("value1")));
        assertThat(result.get(1), is((StaticOperand)literal("value2")));
    }

    @Test
    public void shouldParseInClauseFromStringWithThreeValidLiteral() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN ('value1','value2','value3')"), typeSystem);
        assertThat(result.size(), is(3));
        assertThat(result.get(0), is((StaticOperand)literal("value1")));
        assertThat(result.get(1), is((StaticOperand)literal("value2")));
        assertThat(result.get(2), is((StaticOperand)literal("value3")));
    }

    @Test
    public void shouldParseInClauseFromStringWithSingleValidLiteralCast() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN (CAST('value1' AS STRING))"), typeSystem);
        assertThat(result.size(), is(1));
        assertThat(result.iterator().next(), is((StaticOperand)literal("value1")));

        result = parser.parseInClause(tokens("IN (CAST('3' AS LONG))"), typeSystem);
        assertThat(result.size(), is(1));
        assertThat(result.iterator().next(), is((StaticOperand)literal(new Long(3))));
    }

    @Test
    public void shouldParseInClauseFromStringWithMultipleValidLiteralCasts() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN (CAST('value1' AS STRING),CAST('3' AS LONG),'4')"),
                                                          typeSystem);
        assertThat(result.size(), is(3));
        assertThat(result.get(0), is((StaticOperand)literal("value1")));
        assertThat(result.get(1), is((StaticOperand)literal(new Long(3))));
        assertThat(result.get(2), is((StaticOperand)literal("4")));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldParseInClauseContainingSubqueryWithNoCriteria() {
        List<StaticOperand> result = parser.parseInClause(tokens("IN (SELECT * FROM tableA)"), typeSystem);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is((StaticOperand)subquery("SELECT * FROM tableA")));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldParseInClauseContainingSubqueryWithNestedCriteriaAndParentheses() {
        String expression = "SELECT * FROM tableA WHERE (foo < 3 AND (bar = 22))";
        List<StaticOperand> result = parser.parseInClause(tokens("IN (" + expression + ")"), typeSystem);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is((StaticOperand)subquery(expression)));
    }

    protected Literal literal( Object literalValue ) {
        return new Literal(literalValue);
    }

    protected QueryCommand query( String subquery ) {
        return parser.parseQuery(subquery, typeSystem);
    }

    protected Subquery subquery( String subquery ) {
        return new Subquery(query(subquery));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseFullTextSearchExpression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseFullTextSearchExpressionFromStringWithValidExpression() {
        Position pos = new Position(500, 100, 13);
        FullTextSearch.Term result = parser.parseFullTextSearchExpression("term1 term2 OR -term3 OR -term4 OR term5", pos);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertThat(disjunction.getTerms().size(), is(4));
        Conjunction conjunction1 = (Conjunction)disjunction.getTerms().get(0);
        Term term3 = disjunction.getTerms().get(1);
        Term term4 = disjunction.getTerms().get(2);
        Term term5 = disjunction.getTerms().get(3);
        FullTextSearchParserTest.assertHasSimpleTerms(conjunction1, "term1", "term2");
        FullTextSearchParserTest.assertSimpleTerm(term3, "term3", true, false);
        FullTextSearchParserTest.assertSimpleTerm(term4, "term4", true, false);
        FullTextSearchParserTest.assertSimpleTerm(term5, "term5", false, false);
    }

    @Test
    public void shouldConvertPositionWhenUnableToParseFullTextSearchExpression() {
        try {
            parser.parseFullTextSearchExpression("", new Position(500, 100, 13));
            fail("Should have thrown an exception");
        } catch (ParsingException e) {
            assertThat(e.getPosition().getLine(), is(100));
            assertThat(e.getPosition().getColumn(), is(13));
            assertThat(e.getPosition().getIndexInContent(), is(500));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseComparisonOperator
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseComparisonOperator() {
        // Same case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.symbol())), is(operator));
        }
        // Upper case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.symbol().toUpperCase())), is(operator));
        }
        // Lower case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.symbol().toLowerCase())), is(operator));
        }
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseComparisonOperatorIfOperatorIsUnknown() {
        parser.parseComparisonOperator(tokens("FOO"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseOrderBy
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParserOrderByWithOneOrdering() {
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC"), typeSystem, mock(Source.class));
        assertThat(orderBy.size(), is(1));
        Ordering first = orderBy.get(0);
        assertThat(first.operand(), is(instanceOf(NodeName.class)));
        assertThat(first.order(), is(Order.ASCENDING));
    }

    @Test
    public void shouldParserOrderByWithTwoOrderings() {
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, SCORE(tableB) DESC"),
                                                     typeSystem,
                                                     mock(Source.class));
        assertThat(orderBy.size(), is(2));
        Ordering first = orderBy.get(0);
        assertThat(first.operand(), is(instanceOf(NodeName.class)));
        assertThat(first.order(), is(Order.ASCENDING));
        Ordering second = orderBy.get(1);
        assertThat(second.operand(), is(instanceOf(FullTextSearchScore.class)));
        assertThat(second.order(), is(Order.DESCENDING));
    }

    @Test
    public void shouldParserOrderByWithMultipleOrderings() {
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, SCORE(tableB) DESC, LENGTH(tableC.id) ASC"),
                                                     typeSystem,
                                                     mock(Source.class));
        assertThat(orderBy.size(), is(3));
        Ordering first = orderBy.get(0);
        assertThat(first.operand(), is(instanceOf(NodeName.class)));
        assertThat(first.order(), is(Order.ASCENDING));
        Ordering second = orderBy.get(1);
        assertThat(second.operand(), is(instanceOf(FullTextSearchScore.class)));
        assertThat(second.order(), is(Order.DESCENDING));
        Ordering third = orderBy.get(2);
        assertThat(third.operand(), is(instanceOf(Length.class)));
        assertThat(third.order(), is(Order.ASCENDING));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseOrderByIfCommaNotFollowedByAnotherOrdering() {
        parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, NOT A VALID ORDERING"), typeSystem, mock(Source.class));
    }

    @Test
    public void shouldReturnNullFromParseOrderByWithoutOrderByKeywords() {
        assertThat(parser.parseOrderBy(tokens("NOT ORDER BY"), typeSystem, mock(Source.class)), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseOrdering
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseOrderingFromDynamicOperandFollowedByAscendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) ASC"), typeSystem, mock(Source.class));
        assertThat(ordering.operand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.order(), is(Order.ASCENDING));
    }

    @Test
    public void shouldParseOrderingFromDynamicOperandFollowedByDecendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) DESC"), typeSystem, mock(Source.class));
        assertThat(ordering.operand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.order(), is(Order.DESCENDING));
    }

    @Test
    public void shouldParseOrderingFromDynamicOperandAndDefaultToAscendingWhenNotFollowedByAscendingOrDescendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) OTHER"), typeSystem, mock(Source.class));
        assertThat(ordering.operand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.order(), is(Order.ASCENDING));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePropertyExistance
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePropertyExistanceFromPropertyNameWithSelectorNameAndPropertyNameFollowedByIsNotNull() {
        Constraint constraint = parser.parsePropertyExistance(tokens("tableA.property1 IS NOT NULL"),
                                                              typeSystem,
                                                              mock(Source.class));
        assertThat(constraint, is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)constraint;
        assertThat(p.propertyName(), is("property1"));
        assertThat(p.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParsePropertyExistanceFromPropertyNameWithPropertyNameAndNoSelectorNameFollowedByIsNotNull() {
        NamedSelector source = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parsePropertyExistance(tokens("property1 IS NOT NULL"), typeSystem, source);
        assertThat(constraint, is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)constraint;
        assertThat(p.propertyName(), is("property1"));
        assertThat(p.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyExistanceFromPropertyNameWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parsePropertyExistance(tokens("property1 IS NOT NULL"), typeSystem, mock(Source.class));
    }

    @Test
    public void shouldParseNotPropertyExistanceFromPropertyNameWithSelectorNameAndPropertyNameFollowedByIsNull() {
        Constraint constraint = parser.parsePropertyExistance(tokens("tableA.property1 IS NULL"), typeSystem, mock(Source.class));
        assertThat(constraint, is(instanceOf(Not.class)));
        Not not = (Not)constraint;
        assertThat(not.constraint(), is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)not.constraint();
        assertThat(p.propertyName(), is("property1"));
        assertThat(p.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldReturnNullFromParsePropertyExistanceIfExpressionDoesNotMatchPattern() {
        Source s = mock(Source.class);
        assertThat(parser.parsePropertyExistance(tokens("tableA WILL NOT"), typeSystem, s), is(nullValue()));
        assertThat(parser.parsePropertyExistance(tokens("tableA.property1 NOT NULL"), typeSystem, s), is(nullValue()));
        assertThat(parser.parsePropertyExistance(tokens("tableA.property1 IS NOT SOMETHING"), typeSystem, s), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseStaticOperand
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseStaticOperandFromStringWithBindVariable() {
        StaticOperand operand = parser.parseStaticOperand(tokens("$VAR"), typeSystem);
        assertThat(operand, is(instanceOf(BindVariableName.class)));
        BindVariableName var = (BindVariableName)operand;
        assertThat(var.variableName(), is("VAR"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseStaticOperandFromStringWithBindVariableWithNoVariableName() {
        parser.parseStaticOperand(tokens("$"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseStaticOperandFromStringWithBindVariableWithCharactersThatAreNotFromNCName() {
        parser.parseStaticOperand(tokens("$#2VAR"), typeSystem);
    }

    @Test
    public void shouldParseStaticOperandFromStringWithLiteralValue() {
        StaticOperand operand = parser.parseStaticOperand(tokens("CAST(123 AS DOUBLE)"), typeSystem);
        assertThat(operand, is(instanceOf(Literal.class)));
        Literal literal = (Literal)operand;
        assertThat((Double)literal.value(), is(typeSystem.getDoubleFactory().create("123")));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldParseStaticOperandWithSubquery() {
        QueryCommand expected = parser.parseQuery(tokens("SELECT * FROM tableA"), typeSystem);
        StaticOperand operand = parser.parseStaticOperand(tokens("SELECT * FROM tableA"), typeSystem);
        assertThat(operand, is(instanceOf(Subquery.class)));
        Subquery subquery = (Subquery)operand;
        assertThat(subquery.query(), is(expected));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldParseStaticOperandWithSubqueryWithoutConsumingExtraTokens() {
        QueryCommand expected = parser.parseQuery(tokens("SELECT * FROM tableA"), typeSystem);
        TokenStream tokens = tokens("SELECT * FROM tableA)");
        StaticOperand operand = parser.parseStaticOperand(tokens, typeSystem);
        assertThat(operand, is(instanceOf(Subquery.class)));
        Subquery subquery = (Subquery)operand;
        assertThat(subquery.query(), is(expected));
        assertThat(tokens.canConsume(')'), is(true));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteral
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralFromStringWithCastBooleanLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(true AS STRING)"), typeSystem).value(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(false AS STRING)"), typeSystem).value(), is(Boolean.FALSE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(TRUE AS STRING)"), typeSystem).value(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(FALSE AS STRING)"), typeSystem).value(), is(Boolean.FALSE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST('true' AS stRinG)"), typeSystem).value(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(\"false\" AS string)"), typeSystem).value(),
                   is(Boolean.FALSE.toString()));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastBooleanLiteralToBinary() {
        Binary binaryTrue = (Binary)typeSystem.getTypeFactory(PropertyType.BINARY.getName()).create(true);
        Binary binaryFalse = (Binary)typeSystem.getTypeFactory(PropertyType.BINARY.getName()).create(false);
        assertThat((Binary)parser.parseLiteral(tokens("CAST(true AS BINARY)"), typeSystem).value(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(false AS BINARY)"), typeSystem).value(), is(binaryFalse));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(TRUE AS BINARY)"), typeSystem).value(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(FALSE AS BINARY)"), typeSystem).value(), is(binaryFalse));
        assertThat((Binary)parser.parseLiteral(tokens("CAST('true' AS biNarY)"), typeSystem).value(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(\"false\" AS binary)"), typeSystem).value(), is(binaryFalse));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToLong() {
        parser.parseLiteral(tokens("CAST(true AS LONG)"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToDouble() {
        parser.parseLiteral(tokens("CAST(true AS DOUBLE)"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToDate() {
        parser.parseLiteral(tokens("CAST(true AS DATE)"), typeSystem);
    }

    @Test
    public void shouldParseLiteralFromStringWithCastLongLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(123 AS STRING)"), typeSystem).value(), is("123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(+123 AS STRING)"), typeSystem).value(), is("123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(-123 AS STRING)"), typeSystem).value(), is("-123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(0 AS STRING)"), typeSystem).value(), is("0"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastLongLiteralToLong() {
        assertThat((Long)parser.parseLiteral(tokens("CAST(123 AS LONG)"), typeSystem).value(), is(123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(+123 AS LONG)"), typeSystem).value(), is(123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(-123 AS LONG)"), typeSystem).value(), is(-123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(0 AS LONG)"), typeSystem).value(), is(0L));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastDoubleLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23 AS STRING)"), typeSystem).value(), is("1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(+1.23 AS STRING)"), typeSystem).value(), is("1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(-1.23 AS STRING)"), typeSystem).value(), is("-1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e10 AS STRING)"), typeSystem).value(), is("1.23E10"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e+10 AS STRING)"), typeSystem).value(), is("1.23E10"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e-10 AS STRING)"), typeSystem).value(), is("1.23E-10"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastDateLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345Z AS STRING)"), typeSystem).value(),
                   is("2009-03-22T03:22:45.345Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345UTC AS STRING)"), typeSystem).value(),
                   is("2009-03-22T03:22:45.345Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.3-01:00 AS STRING)"), typeSystem).value(),
                   is("2009-03-22T04:22:45.300Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345+01:00 AS STRING)"), typeSystem).value(),
                   is("2009-03-22T02:22:45.345Z"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastStringLiteralToName() {
        assertThat((Name)parser.parseLiteral(tokens("CAST([mode:name] AS NAME)"), typeSystem).value(), is(name("mode:name")));
        assertThat((Name)parser.parseLiteral(tokens("CAST('mode:name' AS NAME)"), typeSystem).value(), is(name("mode:name")));
        assertThat((Name)parser.parseLiteral(tokens("CAST(\"mode:name\" AS NAME)"), typeSystem).value(), is(name("mode:name")));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastStringLiteralToPath() {
        assertThat((Path)parser.parseLiteral(tokens("CAST([/mode:name/a/b] AS PATH)"), typeSystem).value(),
                   is(path("/mode:name/a/b")));
    }

    @Test
    public void shouldParseLiteralFromStringWithUncastLiteralValueAndRepresentValueAsStringRepresentation() {
        assertThat(parser.parseLiteral(tokens("true"), typeSystem).value(), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteral(tokens("false"), typeSystem).value(), is((Object)Boolean.FALSE.toString()));
        assertThat(parser.parseLiteral(tokens("TRUE"), typeSystem).value(), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteral(tokens("FALSE"), typeSystem).value(), is((Object)Boolean.FALSE.toString()));
        assertThat(parser.parseLiteral(tokens("123"), typeSystem).value(), is((Object)"123"));
        assertThat(parser.parseLiteral(tokens("+123"), typeSystem).value(), is((Object)"123"));
        assertThat(parser.parseLiteral(tokens("-123"), typeSystem).value(), is((Object)"-123"));
        assertThat(parser.parseLiteral(tokens("1.23"), typeSystem).value(), is((Object)"1.23"));
        assertThat(parser.parseLiteral(tokens("+1.23"), typeSystem).value(), is((Object)"1.23"));
        assertThat(parser.parseLiteral(tokens("-1.23"), typeSystem).value(), is((Object)"-1.23"));
        assertThat(parser.parseLiteral(tokens("1.23e10"), typeSystem).value(), is((Object)"1.23E10"));
        assertThat(parser.parseLiteral(tokens("1.23e+10"), typeSystem).value(), is((Object)"1.23E10"));
        assertThat(parser.parseLiteral(tokens("1.23e-10"), typeSystem).value(), is((Object)"1.23E-10"));
        assertThat(parser.parseLiteral(tokens("0"), typeSystem).value(), is((Object)"0"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345Z"), typeSystem).value(),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345UTC"), typeSystem).value(),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.3-01:00"), typeSystem).value(),
                   is((Object)"2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345+01:00"), typeSystem).value(),
                   is((Object)"2009-03-22T02:22:45.345Z"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoEndingParenthesis() {
        parser.parseLiteral(tokens("CAST(123 AS STRING OTHER"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoOpeningParenthesis() {
        parser.parseLiteral(tokens("CAST 123 AS STRING) OTHER"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndInvalidType() {
        parser.parseLiteral(tokens("CAST(123 AS FOOD) OTHER"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoAsKeyword() {
        parser.parseLiteral(tokens("CAST(123 STRING) OTHER"), typeSystem);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoLiteralValueBeforeAs() {
        parser.parseLiteral(tokens("CAST(AS STRING) OTHER"), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteralValue - unquoted
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeIntegerValues() {
        assertThat(parser.parseLiteralValue(tokens("123"), typeSystem), is((Object)"123"));
        assertThat(parser.parseLiteralValue(tokens("-123"), typeSystem), is((Object)"-123"));
        assertThat(parser.parseLiteralValue(tokens("- 123"), typeSystem), is((Object)"-123"));
        assertThat(parser.parseLiteralValue(tokens("+123"), typeSystem), is((Object)"123"));
        assertThat(parser.parseLiteralValue(tokens("+ 123"), typeSystem), is((Object)"123"));
        assertThat(parser.parseLiteralValue(tokens("0"), typeSystem), is((Object)"0"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeDecimalValues() {
        assertThat(parser.parseLiteralValue(tokens("1.23"), typeSystem), is((Object)"1.23"));
        assertThat(parser.parseLiteralValue(tokens("-1.23"), typeSystem), is((Object)"-1.23"));
        assertThat(parser.parseLiteralValue(tokens("+0.123"), typeSystem), is((Object)"0.123"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeDecimalValuesInScientificNotation() {
        assertThat(parser.parseLiteralValue(tokens("1.23"), typeSystem), is((Object)"1.23"));
        assertThat(parser.parseLiteralValue(tokens("1.23e10"), typeSystem), is((Object)"1.23E10"));
        assertThat(parser.parseLiteralValue(tokens("- 1.23e10"), typeSystem), is((Object)"-1.23E10"));
        assertThat(parser.parseLiteralValue(tokens("- 1.23e-10"), typeSystem), is((Object)"-1.23E-10"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithBooleanValues() {
        assertThat(parser.parseLiteralValue(tokens("true"), typeSystem), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteralValue(tokens("false"), typeSystem), is((Object)Boolean.FALSE.toString()));
        assertThat(parser.parseLiteralValue(tokens("TRUE"), typeSystem), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteralValue(tokens("FALSE"), typeSystem), is((Object)Boolean.FALSE.toString()));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithDateValues() {
        // sYYYY-MM-DDThh:mm:ss.sssTZD
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345Z"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345UTC"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.3-01:00"), typeSystem),
                   is((Object)"2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345+01:00"), typeSystem),
                   is((Object)"2009-03-22T02:22:45.345Z"));

        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345Z"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345UTC"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.3-01:00"), typeSystem),
                   is((Object)"-2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345+01:00"), typeSystem),
                   is((Object)"-2009-03-22T02:22:45.345Z"));

        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345Z"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345UTC"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.3-01:00"), typeSystem),
                   is((Object)"2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345+01:00"), typeSystem),
                   is((Object)"2009-03-22T02:22:45.345Z"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteralValue - quoted
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeIntegerValues() {
        assertThat(parser.parseLiteralValue(tokens("'123'"), typeSystem), is((Object)"123"));
        assertThat(parser.parseLiteralValue(tokens("'-123'"), typeSystem), is((Object)"-123"));
        assertThat(parser.parseLiteralValue(tokens("'- 123'"), typeSystem), is((Object)"- 123"));
        assertThat(parser.parseLiteralValue(tokens("'+123'"), typeSystem), is((Object)"+123"));
        assertThat(parser.parseLiteralValue(tokens("'+ 123'"), typeSystem), is((Object)"+ 123"));
        assertThat(parser.parseLiteralValue(tokens("'0'"), typeSystem), is((Object)"0"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeDecimalValues() {
        assertThat(parser.parseLiteralValue(tokens("'1.23'"), typeSystem), is((Object)"1.23"));
        assertThat(parser.parseLiteralValue(tokens("'-1.23'"), typeSystem), is((Object)"-1.23"));
        assertThat(parser.parseLiteralValue(tokens("'+0.123'"), typeSystem), is((Object)"+0.123"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeDecimalValuesInScientificNotation() {
        assertThat(parser.parseLiteralValue(tokens("'1.23'"), typeSystem), is((Object)"1.23"));
        assertThat(parser.parseLiteralValue(tokens("'1.23e10'"), typeSystem), is((Object)"1.23e10"));
        assertThat(parser.parseLiteralValue(tokens("'- 1.23e10'"), typeSystem), is((Object)"- 1.23e10"));
        assertThat(parser.parseLiteralValue(tokens("'- 1.23e-10'"), typeSystem), is((Object)"- 1.23e-10"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithBooleanValues() {
        assertThat(parser.parseLiteralValue(tokens("'true'"), typeSystem), is((Object)"true"));
        assertThat(parser.parseLiteralValue(tokens("'false'"), typeSystem), is((Object)"false"));
        assertThat(parser.parseLiteralValue(tokens("'TRUE'"), typeSystem), is((Object)"TRUE"));
        assertThat(parser.parseLiteralValue(tokens("'FALSE'"), typeSystem), is((Object)"FALSE"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithDateValues() {
        // sYYYY-MM-DDThh:mm:ss.sssTZD
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345Z'"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345UTC'"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.3-01:00'"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345+01:00'"), typeSystem),
                   is((Object)"2009-03-22T03:22:45.345+01:00"));

        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345Z'"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345UTC'"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.3-01:00'"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345+01:00'"), typeSystem),
                   is((Object)"-2009-03-22T03:22:45.345+01:00"));

        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345Z'"), typeSystem),
                   is((Object)"+2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345UTC'"), typeSystem),
                   is((Object)"+2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.3-01:00'"), typeSystem),
                   is((Object)"+2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345+01:00'"), typeSystem),
                   is((Object)"+2009-03-22T03:22:45.345+01:00"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LENGTH
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLengthOfPropertyValue() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LENGTH(tableA.property)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(Length.class)));
        Length length = (Length)operand;
        assertThat(length.propertyValue().propertyName(), is("property"));
        assertThat(length.propertyValue().selectorName(), is(selectorName("tableA")));
        assertThat(length.selectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("LENGTH(property)"), typeSystem, source);
        assertThat(operand, is(instanceOf(Length.class)));
        length = (Length)operand;
        assertThat(length.propertyValue().propertyName(), is("property"));
        assertThat(length.propertyValue().selectorName(), is(selectorName("tableA")));
        assertThat(length.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLengthWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LENGTH(tableA.property other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLengthWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LENGTH tableA.property other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LOWER
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLowerOfAnotherDynamicOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOWER(tableA.property)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)operand;
        assertThat(lower.selectorName(), is(selectorName("tableA")));
        assertThat(lower.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)lower.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("LOWER(property)"), typeSystem, source);
        assertThat(operand, is(instanceOf(LowerCase.class)));
        lower = (LowerCase)operand;
        assertThat(lower.selectorName(), is(selectorName("tableA")));
        assertThat(lower.operand(), is(instanceOf(PropertyValue.class)));
        value = (PropertyValue)lower.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingLowerOfUpperCaseOfAnotherOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOWER(UPPER(tableA.property))"),
                                                            typeSystem,
                                                            mock(Source.class));
        assertThat(operand, is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)operand;
        assertThat(lower.selectorName(), is(selectorName("tableA")));
        assertThat(lower.operand(), is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)lower.operand();
        assertThat(upper.selectorName(), is(selectorName("tableA")));
        assertThat(upper.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)upper.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLowerWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LOWER(tableA.property other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLowerWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LOWER tableA.property other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - UPPER
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingUpperOfAnotherDynamicOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("UPPER(tableA.property)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)operand;
        assertThat(upper.selectorName(), is(selectorName("tableA")));
        assertThat(upper.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)upper.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("UPPER(property)"), typeSystem, source);
        assertThat(operand, is(instanceOf(UpperCase.class)));
        upper = (UpperCase)operand;
        assertThat(upper.selectorName(), is(selectorName("tableA")));
        assertThat(upper.operand(), is(instanceOf(PropertyValue.class)));
        value = (PropertyValue)upper.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingUpperOfLowerCaseOfAnotherOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("UPPER(LOWER(tableA.property))"),
                                                            typeSystem,
                                                            mock(Source.class));
        assertThat(operand, is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)operand;
        assertThat(upper.selectorName(), is(selectorName("tableA")));
        assertThat(upper.operand(), is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)upper.operand();
        assertThat(lower.selectorName(), is(selectorName("tableA")));
        assertThat(lower.operand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)lower.operand();
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingUpperWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("UPPER(tableA.property other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingUpperWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Upper tableA.property other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - DEPTH
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingDepthOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("DEPTH(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(NodeDepth.class)));
        NodeDepth depth = (NodeDepth)operand;
        assertThat(depth.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingDepthWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("DEPTH()"), typeSystem, source);
        assertThat(operand, is(instanceOf(NodeDepth.class)));
        NodeDepth depth = (NodeDepth)operand;
        assertThat(depth.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingDepthWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("DEPTH()"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingDepthWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("DEPTH(tableA.property) other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingDepthWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("DEPTH(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingDepthWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Depth  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - PATH
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingPathOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("PATH(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(NodePath.class)));
        NodePath path = (NodePath)operand;
        assertThat(path.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingPathWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("PATH()"), typeSystem, source);
        assertThat(operand, is(instanceOf(NodePath.class)));
        NodePath path = (NodePath)operand;
        assertThat(path.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingPathWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("PATH()"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingPathWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("PATH(tableA.property) other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingPathWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("PATH(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingPathWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Path  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - NAME
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingNameOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("NAME(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(NodeName.class)));
        NodeName name = (NodeName)operand;
        assertThat(name.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingNameWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("NAME()"), typeSystem, source);
        assertThat(operand, is(instanceOf(NodeName.class)));
        NodeName name = (NodeName)operand;
        assertThat(name.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("NAME()"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("NAME(tableA.property) other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("NAME(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Name  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LOCALNAME
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLocalNameOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOCALNAME(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(NodeLocalName.class)));
        NodeLocalName name = (NodeLocalName)operand;
        assertThat(name.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingLocalNameWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOCALNAME()"), typeSystem, source);
        assertThat(operand, is(instanceOf(NodeLocalName.class)));
        NodeLocalName name = (NodeLocalName)operand;
        assertThat(name.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("LOCALNAME()"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("LOCALNAME(tableA.property) other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LOCALNAME(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LocalName  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - SCORE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingFullTextSearchScoreOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("SCORE(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(FullTextSearchScore.class)));
        FullTextSearchScore score = (FullTextSearchScore)operand;
        assertThat(score.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingFullTextSearchScoreWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("SCORE()"), typeSystem, source);
        assertThat(operand, is(instanceOf(FullTextSearchScore.class)));
        FullTextSearchScore score = (FullTextSearchScore)operand;
        assertThat(score.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("SCORE()"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("SCORE(tableA.property) other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("SCORE(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Score  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - PropertyValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringWithUnquotedSelectorNameAndUnquotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("tableA.property"), typeSystem, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithQuotedSelectorNameAndUnquotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("[mode:tableA].property"), typeSystem, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithQuotedSelectorNameAndQuotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("[mode:tableA].[mode:property]"), typeSystem, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.propertyName(), is("mode:property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithOnlyPropertyNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("property"), typeSystem, source);
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToDynamicOperandValueFromStringWithOnlyPropertyNameIfSourceIsNotSelector() {
        parser.parsePropertyValue(tokens("property"), typeSystem, mock(Join.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringWithOnlySelectorNameAndPeriod() {
        parser.parsePropertyValue(tokens("tableA. "), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - ReferenceValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE(tableA)"), typeSystem, mock(Source.class));
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is(nullValue()));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE()"), typeSystem, source);
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is(nullValue()));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueWithWithOnlyPropertyNameIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE(property) other"), typeSystem, source);
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is("property"));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueWithWithSelectorNameAndPropertyNameIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE(tableA.property) other"), typeSystem, source);
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is("property"));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueWithWithOnlySelectorNameMatchingThatOfOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE(tableA) other"), typeSystem, source);
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is(nullValue()));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingReferenceValueWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("REFERENCE()"), typeSystem, mock(Source.class));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingReferenceValueWithWithSelectorNameAndProperty() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("REFERENCE(tableA.property) other"),
                                                            typeSystem,
                                                            mock(Source.class));
        assertThat(operand, is(instanceOf(ReferenceValue.class)));
        ReferenceValue value = (ReferenceValue)operand;
        assertThat(value.selectorName(), is(selectorName("tableA")));
        assertThat(value.propertyName(), is("property"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingReferenceValueWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("REFERENCE(tableA other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingReferenceValueWithoutSelectorOrPropertyIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("REFERENCE() other"), typeSystem, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingReferenceValueWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Reference  tableA other"), typeSystem, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePropertyValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePropertyValueFromStringWithUnquotedSelectorNameAndUnquotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("tableA.property"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithQuotedSelectorNameAndUnquotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("[mode:tableA].property"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithQuotedSelectorNameAndQuotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("[mode:tableA].[mode:property]"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("mode:property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithOnlyPropertyNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        PropertyValue value = parser.parsePropertyValue(tokens("property"), typeSystem, source);
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyValueFromStringWithOnlyPropertyNameIfSourceIsNotSelector() {
        parser.parsePropertyValue(tokens("property"), typeSystem, mock(Join.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyValueFromStringWithOnlySelectorNameAndPeriod() {
        parser.parsePropertyValue(tokens("tableA. "), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseReferenceValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseReferenceValueFromStringWithUnquotedSelectorNameAndUnquotedPropertyName() {
        ReferenceValue value = parser.parseReferenceValue(tokens("tableA.property"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        value = parser.parseReferenceValue(tokens("tableA.property"), typeSystem, source);
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseReferenceValueFromStringWithQuotedSelectorNameAndUnquotedPropertyName() {
        ReferenceValue value = parser.parseReferenceValue(tokens("[mode:tableA].property"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));

        Source source = new NamedSelector(selectorName("mode:tableA"));
        value = parser.parseReferenceValue(tokens("[mode:tableA].property"), typeSystem, source);
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParseReferenceValueFromStringWithQuotedSelectorNameAndQuotedPropertyName() {
        ReferenceValue value = parser.parseReferenceValue(tokens("[mode:tableA].[mode:property]"), typeSystem, mock(Join.class));
        assertThat(value.propertyName(), is("mode:property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));

        Source source = new NamedSelector(selectorName("mode:tableA"));
        value = parser.parseReferenceValue(tokens("[mode:tableA].[mode:property]"), typeSystem, source);
        assertThat(value.propertyName(), is("mode:property"));
        assertThat(value.selectorName(), is(selectorName("mode:tableA")));
    }

    @Test
    public void shouldParseReferenceValueFromStringWithOnlyPropertyNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        ReferenceValue value = parser.parseReferenceValue(tokens("property)"), typeSystem, source);
        assertThat(value.propertyName(), is("property"));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseReferenceValueFromStringWithMatchingSelectorNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        ReferenceValue value = parser.parseReferenceValue(tokens("tableA)"), typeSystem, source);
        assertThat(value.propertyName(), is(nullValue()));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseReferenceValueFromStringWithOnlySelectorNameIfSourceIsNotSelector() {
        Source source = mock(Join.class);
        ReferenceValue value = parser.parseReferenceValue(tokens("tableA)"), typeSystem, source);
        assertThat(value.propertyName(), is(nullValue()));
        assertThat(value.selectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseReferenceValueFromStringWithOnlySelectorNameAndPeriod() {
        parser.parseReferenceValue(tokens("tableA. "), typeSystem, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLimit
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLimitFromFormWithJustOneNumber() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10"));
        assertThat(limit.rowLimit(), is(10));
        assertThat(limit.offset(), is(0));

        limit = parser.parseLimit(tokens("LIMIT 10 NONOFFSET"));
        assertThat(limit.rowLimit(), is(10));
        assertThat(limit.offset(), is(0));
    }

    @Test
    public void shouldParseLimitFromFormWithRowLimitAndOffset() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10 OFFSET 30"));
        assertThat(limit.rowLimit(), is(10));
        assertThat(limit.offset(), is(30));

        limit = parser.parseLimit(tokens("LIMIT 10 OFFSET 30 OTHER"));
        assertThat(limit.rowLimit(), is(10));
        assertThat(limit.offset(), is(30));
    }

    @Test
    public void shouldParseLimitFromFormWithTwoCommaSeparatedNumbers() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10,30"));
        assertThat(limit.rowLimit(), is(20));
        assertThat(limit.offset(), is(10));
    }

    @Test
    public void shouldReturnNullFromParseLimitWithNoLimitKeyword() {
        assertThat(parser.parseLimit(tokens("OTHER")), is(nullValue()));
        assertThat(parser.parseLimit(tokens("  ")), is(nullValue()));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLimitIfRowLimitNumberTokenIsNotAnInteger() {
        parser.parseLimit(tokens("LIMIT 10a OFFSET 30"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLimitIfOffsetNumberTokenIsNotAnInteger() {
        parser.parseLimit(tokens("LIMIT 10 OFFSET 30a"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLimitIfStartingRowNumberTokenIsNotAnInteger() {
        parser.parseLimit(tokens("LIMIT 10a,20"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLimitIfEndingRowNumberTokenIsNotAnInteger() {
        parser.parseLimit(tokens("LIMIT 10,20a"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseNamedSelector
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNamedSelectorFromUnquotedNameWithNoAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("name"), typeSystem);
        assertThat(selector.name(), is(selectorName("name")));
        assertThat(selector.alias(), is(nullValue()));
        assertThat(selector.aliasOrName(), is(selectorName("name")));
    }

    @Test
    public void shouldParseNamedSelectorFromUnquotedNameWithUnquotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("name AS alias"), typeSystem);
        assertThat(selector.name(), is(selectorName("name")));
        assertThat(selector.alias(), is(selectorName("alias")));
        assertThat(selector.aliasOrName(), is(selectorName("alias")));
    }

    @Test
    public void shouldParseNamedSelectorFromQuotedNameWithUnquotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("'name' AS alias"), typeSystem);
        assertThat(selector.name(), is(selectorName("name")));
        assertThat(selector.alias(), is(selectorName("alias")));
        assertThat(selector.aliasOrName(), is(selectorName("alias")));
    }

    @Test
    public void shouldParseNamedSelectorFromQuotedNameWithQuotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("'name' AS [alias]"), typeSystem);
        assertThat(selector.name(), is(selectorName("name")));
        assertThat(selector.alias(), is(selectorName("alias")));
        assertThat(selector.aliasOrName(), is(selectorName("alias")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParseNamedSelectorIfNoMoreTokens() {
        parser.parseNamedSelector(tokens("  "), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseSelectorName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseSelectorNameFromUnquotedString() {
        assertThat(parser.parseSelectorName(tokens("name"), typeSystem), is(selectorName("name")));
    }

    @Test
    public void shouldParseSelectorNameFromQuotedString() {
        assertThat(parser.parseSelectorName(tokens("'name'"), typeSystem), is(selectorName("name")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParseSelectorNameIfNoMoreTokens() {
        parser.parseSelectorName(tokens("  "), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNameFromSingleQuotedString() {
        assertThat(parser.parseName(tokens("'jcr:name'"), typeSystem), is("jcr:name"));
    }

    @Test
    public void shouldParseNameFromDoubleQuotedString() {
        assertThat(parser.parseName(tokens("\"jcr:name\""), typeSystem), is("jcr:name"));
    }

    @Test
    public void shouldParseNameFromBracketedString() {
        assertThat(parser.parseName(tokens("[jcr:name]"), typeSystem), is("jcr:name"));
    }

    @Test
    public void shouldParseNameFromUnquotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("name"), typeSystem), is("name"));
    }

    @Test
    public void shouldParseNameFromSingleQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("'name'"), typeSystem), is("name"));
    }

    @Test
    public void shouldParseNameFromDoubleQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("\"name\""), typeSystem), is("name"));
    }

    @Test
    public void shouldParseNameFromBracketedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("[name]"), typeSystem), is("name"));
    }

    @Test
    public void shouldParseNameFromBracketedAndQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("['name']"), typeSystem), is("name"));
        assertThat(parser.parseName(tokens("[\"name\"]"), typeSystem), is("name"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNameIfNoMoreTokens() {
        parser.parseName(tokens("  "), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePath
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePathFromUnquotedStringConsistingOfSql92Identifiers() {
        String identifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        assertThat(parser.parsePath(tokens(identifier), typeSystem), is(identifier));
    }

    @Test
    public void shouldParsePathFromSingleQuotedString() {
        assertThat(parser.parsePath(tokens("'/a/b/c/mode:something/d'"), typeSystem), is("/a/b/c/mode:something/d"));
    }

    @Test
    public void shouldParsePathFromDoubleQuotedString() {
        assertThat(parser.parsePath(tokens("\"/a/b/c/mode:something/d\""), typeSystem), is("/a/b/c/mode:something/d"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParsePathIfNoMoreTokens() {
        parser.parsePath(tokens("  "), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // removeBracketsAndQuotes
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldRemoveBracketsAndQuotes() {
        assertThat(parser.removeBracketsAndQuotes("string"), is("string"));
        assertThat(parser.removeBracketsAndQuotes("[string]"), is("string"));
        assertThat(parser.removeBracketsAndQuotes("'string'"), is("string"));
        assertThat(parser.removeBracketsAndQuotes("\"string\""), is("string"));
        assertThat(parser.removeBracketsAndQuotes("word one and two"), is("word one and two"));
        assertThat(parser.removeBracketsAndQuotes("[word one and two]"), is("word one and two"));
        assertThat(parser.removeBracketsAndQuotes("'word one and two'"), is("word one and two"));
        assertThat(parser.removeBracketsAndQuotes("\"word one and two\""), is("word one and two"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected void parse( String query ) {
        parser.parseQuery(query, typeSystem);
    }

    protected SelectorName selectorName( String name ) {
        return new SelectorName(name);
    }

    protected Name name( String name ) {
        return (Name)typeSystem.getTypeFactory(PropertyType.NAME.getName()).create(name);
    }

    protected Path path( String path ) {
        return (Path)typeSystem.getTypeFactory(PropertyType.PATH.getName()).create(path);
    }

    protected DateTime date( String dateTime ) {
        return (DateTime)typeSystem.getDateTimeFactory().create(dateTime);
    }

    protected TokenStream tokens( String content ) {
        return new TokenStream(content, new SqlQueryParser.SqlTokenizer(false), false).start();
    }

}
