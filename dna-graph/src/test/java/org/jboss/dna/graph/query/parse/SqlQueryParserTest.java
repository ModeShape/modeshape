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
package org.jboss.dna.graph.query.parse;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.Position;
import org.jboss.dna.common.text.TokenStream;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.BindVariableName;
import org.jboss.dna.graph.query.model.ChildNode;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.DescendantNode;
import org.jboss.dna.graph.query.model.DynamicOperand;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.FullTextSearchScore;
import org.jboss.dna.graph.query.model.Join;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.Limit;
import org.jboss.dna.graph.query.model.Literal;
import org.jboss.dna.graph.query.model.LowerCase;
import org.jboss.dna.graph.query.model.NamedSelector;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.Not;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.Or;
import org.jboss.dna.graph.query.model.Order;
import org.jboss.dna.graph.query.model.Ordering;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.SameNode;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Source;
import org.jboss.dna.graph.query.model.StaticOperand;
import org.jboss.dna.graph.query.model.UpperCase;
import org.jboss.dna.graph.query.model.FullTextSearch.Conjunction;
import org.jboss.dna.graph.query.model.FullTextSearch.Disjunction;
import org.jboss.dna.graph.query.model.FullTextSearch.SimpleTerm;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SqlQueryParserTest {

    private ExecutionContext context;
    private SqlQueryParser parser;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
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
        parser.parseConstraint(tokens(expression), context, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - parentheses
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithOuterParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("( ISSAMENODE('/a/b') )"), context, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b")));
    }

    @Test
    public void shouldParseConstraintFromStringWithMultipleOuterParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("((( ISSAMENODE('/a/b') )))"), context, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b")));
    }

    @Test
    public void shouldParseConstraintFromStringWithParenthesesAndConjunctionAndDisjunctions() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b') OR (ISSAMENODE('/c/d') AND ISSAMENODE('/e/f'))"),
                                                       context,
                                                       selector);
        assertThat(constraint, is(instanceOf(Or.class)));
        Or or = (Or)constraint;

        assertThat(or.getLeft(), is(instanceOf(SameNode.class)));
        SameNode first = (SameNode)or.getLeft();
        assertThat(first.getSelectorName(), is(selectorName("tableA")));
        assertThat(first.getPath(), is(path("/a/b")));

        assertThat(or.getRight(), is(instanceOf(And.class)));
        And and = (And)or.getRight();

        assertThat(and.getLeft(), is(instanceOf(SameNode.class)));
        SameNode second = (SameNode)and.getLeft();
        assertThat(second.getSelectorName(), is(selectorName("tableA")));
        assertThat(second.getPath(), is(path("/c/d")));

        assertThat(and.getRight(), is(instanceOf(SameNode.class)));
        SameNode third = (SameNode)and.getRight();
        assertThat(third.getSelectorName(), is(selectorName("tableA")));
        assertThat(third.getPath(), is(path("/e/f")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - AND
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithAndExpressionWithNoParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND CONTAINS(p1,term1)"), context, selector);
        assertThat(constraint, is(instanceOf(And.class)));
        And and = (And)constraint;

        assertThat(and.getLeft(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)and.getLeft();
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));

        assertThat(and.getRight(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)and.getRight();
        assertThat(search.getSelectorName(), is(selectorName("tableA")));
        assertThat(search.getPropertyName(), is(name("p1")));
        assertThat(search.getFullTextSearchExpression(), is("term1"));
    }

    @Test
    public void shouldParseConstraintFromStringWithMultipleAndExpressions() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND CONTAINS(p1,term1) AND CONTAINS(p2,term2)"),
                                                       context,
                                                       selector);
        assertThat(constraint, is(instanceOf(And.class)));
        And and = (And)constraint;

        assertThat(and.getLeft(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)and.getLeft();
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));

        assertThat(and.getRight(), is(instanceOf(And.class)));
        And secondAnd = (And)and.getRight();

        assertThat(secondAnd.getLeft(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search1 = (FullTextSearch)secondAnd.getLeft();
        assertThat(search1.getSelectorName(), is(selectorName("tableA")));
        assertThat(search1.getPropertyName(), is(name("p1")));
        assertThat(search1.getFullTextSearchExpression(), is("term1"));

        assertThat(secondAnd.getRight(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search2 = (FullTextSearch)secondAnd.getRight();
        assertThat(search2.getSelectorName(), is(selectorName("tableA")));
        assertThat(search2.getPropertyName(), is(name("p2")));
        assertThat(search2.getFullTextSearchExpression(), is("term2"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithAndExpressionWithNoSecondConstraint() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') AND WHAT THE HECK IS THIS"), context, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - OR
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithOrExpressionWithNoParentheses() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') OR CONTAINS(p1,term1)"), context, selector);
        assertThat(constraint, is(instanceOf(Or.class)));
        Or or = (Or)constraint;

        assertThat(or.getLeft(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)or.getLeft();
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));

        assertThat(or.getRight(), is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)or.getRight();
        assertThat(search.getSelectorName(), is(selectorName("tableA")));
        assertThat(search.getPropertyName(), is(name("p1")));
        assertThat(search.getFullTextSearchExpression(), is("term1"));

    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithOrExpressionWithNoSecondConstraint() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c') OR WHAT THE HECK IS THIS"), context, selector);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - NOT
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithNotSameNodeExpression() {
        Constraint constraint = parser.parseConstraint(tokens("NOT(ISSAMENODE(tableA,'/a/b/c'))"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(Not.class)));
        Not not = (Not)constraint;
        assertThat(not.getConstraint(), is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)not.getConstraint();
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithNotConstraintWithOutOpeningParenthesis() {
        parser.parseConstraint(tokens("NOT CONTAINS(propertyA 'term1 term2 -term3')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithNotConstraintWithOutClosingParenthesis() {
        parser.parseConstraint(tokens("NOT( CONTAINS(propertyA 'term1 term2 -term3') BLAH"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - CONTAINS
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithPropertyAndNoSelectorNameOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3')"), context, selector);
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.getSelectorName(), is(selectorName("tableA")));
        assertThat(search.getPropertyName(), is(name("propertyA")));
        assertThat(search.getFullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithSelectorNameAndProperty() {
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(tableA.propertyA,'term1 term2 -term3')"),
                                                       context,
                                                       mock(Source.class));
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.getSelectorName(), is(selectorName("tableA")));
        assertThat(search.getPropertyName(), is(name("propertyA")));
        assertThat(search.getFullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsContainsExpressionWithSelectorNameAndAnyProperty() {
        Constraint constraint = parser.parseConstraint(tokens("CONTAINS(tableA.*,'term1 term2 -term3')"),
                                                       context,
                                                       mock(Source.class));
        assertThat(constraint, is(instanceOf(FullTextSearch.class)));
        FullTextSearch search = (FullTextSearch)constraint;
        assertThat(search.getSelectorName(), is(selectorName("tableA")));
        assertThat(search.getPropertyName(), is(nullValue()));
        assertThat(search.getFullTextSearchExpression(), is("term1 term2 -term3"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("CONTAINS(propertyA 'term1 term2 -term3')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3' OTHER"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("CONTAINS propertyA,'term1 term2 -term3')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithContainsExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("CONTAINS(propertyA,'term1 term2 -term3')"), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISSAMENODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsSameNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE('/a/b/c')"), context, selector);
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsSameNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISSAMENODE(tableA,'/a/b/c')"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(SameNode.class)));
        SameNode same = (SameNode)constraint;
        assertThat(same.getSelectorName(), is(selectorName("tableA")));
        assertThat(same.getPath(), is(path("/a/b/c")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISSAMENODE(tableA '/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISSAMENODE(tableA,'/a/b/c' AND"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISSAMENODE tableA,'/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsSameNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISSAMENODE('/a/b/c')"), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISCHILDNODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsChildNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISCHILDNODE('/a/b/c')"), context, selector);
        assertThat(constraint, is(instanceOf(ChildNode.class)));
        ChildNode child = (ChildNode)constraint;
        assertThat(child.getSelectorName(), is(selectorName("tableA")));
        assertThat(child.getParentPath(), is(path("/a/b/c")));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsChildNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISCHILDNODE(tableA,'/a/b/c')"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(ChildNode.class)));
        ChildNode child = (ChildNode)constraint;
        assertThat(child.getSelectorName(), is(selectorName("tableA")));
        assertThat(child.getParentPath(), is(path("/a/b/c")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISCHILDNODE(tableA '/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISCHILDNODE(tableA,'/a/b/c' AND"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISCHILDNODE tableA,'/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsChildNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISCHILDNODE('/a/b/c')"), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseConstraint - ISDESCENDANTNODE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseConstraintFromStringWithIsDescendantNodeExpressionWithPathOnlyIfThereIsOneSelectorSource() {
        NamedSelector selector = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parseConstraint(tokens("ISDESCENDANTNODE('/a/b/c')"), context, selector);
        assertThat(constraint, is(instanceOf(DescendantNode.class)));
        DescendantNode descendant = (DescendantNode)constraint;
        assertThat(descendant.getSelectorName(), is(selectorName("tableA")));
        assertThat(descendant.getAncestorPath(), is(path("/a/b/c")));
    }

    @Test
    public void shouldParseConstraintFromStringWithIsDescendantNodeExpressionWithSelectorNameAndPath() {
        Constraint constraint = parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA,'/a/b/c')"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(DescendantNode.class)));
        DescendantNode descendant = (DescendantNode)constraint;
        assertThat(descendant.getSelectorName(), is(selectorName("tableA")));
        assertThat(descendant.getAncestorPath(), is(path("/a/b/c")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoCommaAfterSelectorName() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA '/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoClosingParenthesis() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE(tableA,'/a/b/c' AND"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoOpeningParenthesis() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE tableA,'/a/b/c')"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseConstraintFromStringWithIsDescendantNodeExpressionWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parseConstraint(tokens("ISDESCENDANTNODE('/a/b/c')"), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseFullTextSearchExpression
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseFullTextSearchExpressionFromStringWithValidExpression() {
        Position pos = new Position(100, 13);
        FullTextSearch.Term result = parser.parseFullTextSearchExpression("term1 term2 OR -term3 OR -term4 OR term5", pos);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertThat(disjunction.getTerms().size(), is(4));
        Conjunction conjunction1 = (Conjunction)disjunction.getTerms().get(0);
        SimpleTerm term3 = (SimpleTerm)disjunction.getTerms().get(1);
        SimpleTerm term4 = (SimpleTerm)disjunction.getTerms().get(2);
        SimpleTerm term5 = (SimpleTerm)disjunction.getTerms().get(3);
        FullTextSearchParserTest.assertHasSimpleTerms(conjunction1, "term1", "term2");
        FullTextSearchParserTest.assertSimpleTerm(term3, "term3", true, false);
        FullTextSearchParserTest.assertSimpleTerm(term4, "term4", true, false);
        FullTextSearchParserTest.assertSimpleTerm(term5, "term5", false, false);
    }

    @Test
    public void shouldConvertPositionWhenUnableToParseFullTextSearchExpression() {
        try {
            parser.parseFullTextSearchExpression("", new Position(100, 13));
            fail("Should have thrown an exception");
        } catch (ParsingException e) {
            assertThat(e.getPosition().getLine(), is(100));
            assertThat(e.getPosition().getColumn(), is(13));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseComparisonOperator
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseComparisonOperator() {
        // Same case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.getSymbol())), is(operator));
        }
        // Upper case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.getSymbol().toUpperCase())), is(operator));
        }
        // Lower case
        for (Operator operator : Operator.values()) {
            assertThat(parser.parseComparisonOperator(tokens(operator.getSymbol().toLowerCase())), is(operator));
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
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC"), context, mock(Source.class));
        assertThat(orderBy.size(), is(1));
        Ordering first = orderBy.get(0);
        assertThat(first.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(first.getOrder(), is(Order.ASCENDING));
    }

    @Test
    public void shouldParserOrderByWithTwoOrderings() {
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, SCORE(tableB) DESC"),
                                                     context,
                                                     mock(Source.class));
        assertThat(orderBy.size(), is(2));
        Ordering first = orderBy.get(0);
        assertThat(first.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(first.getOrder(), is(Order.ASCENDING));
        Ordering second = orderBy.get(1);
        assertThat(second.getOperand(), is(instanceOf(FullTextSearchScore.class)));
        assertThat(second.getOrder(), is(Order.DESCENDING));
    }

    @Test
    public void shouldParserOrderByWithMultipleOrderings() {
        List<Ordering> orderBy = parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, SCORE(tableB) DESC, LENGTH(tableC.id) ASC"),
                                                     context,
                                                     mock(Source.class));
        assertThat(orderBy.size(), is(3));
        Ordering first = orderBy.get(0);
        assertThat(first.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(first.getOrder(), is(Order.ASCENDING));
        Ordering second = orderBy.get(1);
        assertThat(second.getOperand(), is(instanceOf(FullTextSearchScore.class)));
        assertThat(second.getOrder(), is(Order.DESCENDING));
        Ordering third = orderBy.get(2);
        assertThat(third.getOperand(), is(instanceOf(Length.class)));
        assertThat(third.getOrder(), is(Order.ASCENDING));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseOrderByIfCommaNotFollowedByAnotherOrdering() {
        parser.parseOrderBy(tokens("ORDER BY NAME(tableA) ASC, NOT A VALID ORDERING"), context, mock(Source.class));
    }

    @Test
    public void shouldReturnNullFromParseOrderByWithoutOrderByKeywords() {
        assertThat(parser.parseOrderBy(tokens("NOT ORDER BY"), context, mock(Source.class)), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseOrdering
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseOrderingFromDynamicOperandFollowedByAscendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) ASC"), context, mock(Source.class));
        assertThat(ordering.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.getOrder(), is(Order.ASCENDING));
    }

    @Test
    public void shouldParseOrderingFromDynamicOperandFollowedByDecendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) DESC"), context, mock(Source.class));
        assertThat(ordering.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.getOrder(), is(Order.DESCENDING));
    }

    @Test
    public void shouldParseOrderingFromDynamicOperandAndDefaultToAscendingWhenNotFollowedByAscendingOrDescendingKeyword() {
        Ordering ordering = parser.parseOrdering(tokens("NAME(tableA) OTHER"), context, mock(Source.class));
        assertThat(ordering.getOperand(), is(instanceOf(NodeName.class)));
        assertThat(ordering.getOrder(), is(Order.ASCENDING));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePropertyExistance
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePropertyExistanceFromPropertyNameWithSelectorNameAndPropertyNameFollowedByIsNotNull() {
        Constraint constraint = parser.parsePropertyExistance(tokens("tableA.property1 IS NOT NULL"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)constraint;
        assertThat(p.getPropertyName(), is(name("property1")));
        assertThat(p.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParsePropertyExistanceFromPropertyNameWithPropertyNameAndNoSelectorNameFollowedByIsNotNull() {
        NamedSelector source = new NamedSelector(selectorName("tableA"));
        Constraint constraint = parser.parsePropertyExistance(tokens("property1 IS NOT NULL"), context, source);
        assertThat(constraint, is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)constraint;
        assertThat(p.getPropertyName(), is(name("property1")));
        assertThat(p.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyExistanceFromPropertyNameWithNoSelectorNameIfSourceIsNotSelector() {
        parser.parsePropertyExistance(tokens("property1 IS NOT NULL"), context, mock(Source.class));
    }

    @Test
    public void shouldParseNotPropertyExistanceFromPropertyNameWithSelectorNameAndPropertyNameFollowedByIsNull() {
        Constraint constraint = parser.parsePropertyExistance(tokens("tableA.property1 IS NULL"), context, mock(Source.class));
        assertThat(constraint, is(instanceOf(Not.class)));
        Not not = (Not)constraint;
        assertThat(not.getConstraint(), is(instanceOf(PropertyExistence.class)));
        PropertyExistence p = (PropertyExistence)not.getConstraint();
        assertThat(p.getPropertyName(), is(name("property1")));
        assertThat(p.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldReturnNullFromParsePropertyExistanceIfExpressionDoesNotMatchPattern() {
        Source s = mock(Source.class);
        assertThat(parser.parsePropertyExistance(tokens("tableA WILL NOT"), context, s), is(nullValue()));
        assertThat(parser.parsePropertyExistance(tokens("tableA.property1 NOT NULL"), context, s), is(nullValue()));
        assertThat(parser.parsePropertyExistance(tokens("tableA.property1 IS NOT SOMETHING"), context, s), is(nullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseStaticOperand
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseStaticOperandFromStringWithBindVariable() {
        StaticOperand operand = parser.parseStaticOperand(tokens("$VAR"), context);
        assertThat(operand, is(instanceOf(BindVariableName.class)));
        BindVariableName var = (BindVariableName)operand;
        assertThat(var.getVariableName(), is("VAR"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseStaticOperandFromStringWithBindVariableWithNoVariableName() {
        parser.parseStaticOperand(tokens("$"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseStaticOperandFromStringWithBindVariableWithCharactersThatAreNotFromNCName() {
        parser.parseStaticOperand(tokens("$#2VAR"), context);
    }

    @Test
    public void shouldParseStaticOperandFromStringWithLiteralValue() {
        StaticOperand operand = parser.parseStaticOperand(tokens("CAST(123 AS DOUBLE)"), context);
        assertThat(operand, is(instanceOf(Literal.class)));
        Literal literal = (Literal)operand;
        assertThat((Double)literal.getValue(), is(context.getValueFactories().getDoubleFactory().create("123")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteral
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralFromStringWithCastBooleanLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(true AS STRING)"), context).getValue(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(false AS STRING)"), context).getValue(), is(Boolean.FALSE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(TRUE AS STRING)"), context).getValue(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(FALSE AS STRING)"), context).getValue(), is(Boolean.FALSE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST('true' AS stRinG)"), context).getValue(), is(Boolean.TRUE.toString()));
        assertThat((String)parser.parseLiteral(tokens("CAST(\"false\" AS string)"), context).getValue(),
                   is(Boolean.FALSE.toString()));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastBooleanLiteralToBinary() {
        Binary binaryTrue = context.getValueFactories().getBinaryFactory().create(true);
        Binary binaryFalse = context.getValueFactories().getBinaryFactory().create(false);
        assertThat((Binary)parser.parseLiteral(tokens("CAST(true AS BINARY)"), context).getValue(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(false AS BINARY)"), context).getValue(), is(binaryFalse));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(TRUE AS BINARY)"), context).getValue(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(FALSE AS BINARY)"), context).getValue(), is(binaryFalse));
        assertThat((Binary)parser.parseLiteral(tokens("CAST('true' AS biNarY)"), context).getValue(), is(binaryTrue));
        assertThat((Binary)parser.parseLiteral(tokens("CAST(\"false\" AS binary)"), context).getValue(), is(binaryFalse));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToLong() {
        parser.parseLiteral(tokens("CAST(true AS LONG)"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToDouble() {
        parser.parseLiteral(tokens("CAST(true AS DOUBLE)"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastBooleanLiteralToDate() {
        parser.parseLiteral(tokens("CAST(true AS DATE)"), context);
    }

    @Test
    public void shouldParseLiteralFromStringWithCastLongLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(123 AS STRING)"), context).getValue(), is("123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(+123 AS STRING)"), context).getValue(), is("123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(-123 AS STRING)"), context).getValue(), is("-123"));
        assertThat((String)parser.parseLiteral(tokens("CAST(0 AS STRING)"), context).getValue(), is("0"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastLongLiteralToLong() {
        assertThat((Long)parser.parseLiteral(tokens("CAST(123 AS LONG)"), context).getValue(), is(123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(+123 AS LONG)"), context).getValue(), is(123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(-123 AS LONG)"), context).getValue(), is(-123L));
        assertThat((Long)parser.parseLiteral(tokens("CAST(0 AS LONG)"), context).getValue(), is(0L));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastDoubleLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23 AS STRING)"), context).getValue(), is("1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(+1.23 AS STRING)"), context).getValue(), is("1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(-1.23 AS STRING)"), context).getValue(), is("-1.23"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e10 AS STRING)"), context).getValue(), is("1.23E10"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e+10 AS STRING)"), context).getValue(), is("1.23E10"));
        assertThat((String)parser.parseLiteral(tokens("CAST(1.23e-10 AS STRING)"), context).getValue(), is("1.23E-10"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastDateLiteralToString() {
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345Z AS STRING)"), context).getValue(),
                   is("2009-03-22T03:22:45.345Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345UTC AS STRING)"), context).getValue(),
                   is("2009-03-22T03:22:45.345Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.3-01:00 AS STRING)"), context).getValue(),
                   is("2009-03-22T04:22:45.300Z"));
        assertThat((String)parser.parseLiteral(tokens("CAST(2009-03-22T03:22:45.345+01:00 AS STRING)"), context).getValue(),
                   is("2009-03-22T02:22:45.345Z"));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastStringLiteralToName() {
        assertThat((Name)parser.parseLiteral(tokens("CAST([dna:name] AS NAME)"), context).getValue(), is(name("dna:name")));
        assertThat((Name)parser.parseLiteral(tokens("CAST('dna:name' AS NAME)"), context).getValue(), is(name("dna:name")));
        assertThat((Name)parser.parseLiteral(tokens("CAST(\"dna:name\" AS NAME)"), context).getValue(), is(name("dna:name")));
    }

    @Test
    public void shouldParseLiteralFromStringWithCastStringLiteralToPath() {
        assertThat((Path)parser.parseLiteral(tokens("CAST([/dna:name/a/b] AS PATH)"), context).getValue(),
                   is(path("/dna:name/a/b")));
    }

    @Test
    public void shouldParseLiteralFromStringWithUncastLiteralValueAndRepresentValueAsStringRepresentation() {
        assertThat(parser.parseLiteral(tokens("true"), context).getValue(), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteral(tokens("false"), context).getValue(), is((Object)Boolean.FALSE.toString()));
        assertThat(parser.parseLiteral(tokens("TRUE"), context).getValue(), is((Object)Boolean.TRUE.toString()));
        assertThat(parser.parseLiteral(tokens("FALSE"), context).getValue(), is((Object)Boolean.FALSE.toString()));
        assertThat(parser.parseLiteral(tokens("123"), context).getValue(), is((Object)"123"));
        assertThat(parser.parseLiteral(tokens("+123"), context).getValue(), is((Object)"123"));
        assertThat(parser.parseLiteral(tokens("-123"), context).getValue(), is((Object)"-123"));
        assertThat(parser.parseLiteral(tokens("1.23"), context).getValue(), is((Object)"1.23"));
        assertThat(parser.parseLiteral(tokens("+1.23"), context).getValue(), is((Object)"1.23"));
        assertThat(parser.parseLiteral(tokens("-1.23"), context).getValue(), is((Object)"-1.23"));
        assertThat(parser.parseLiteral(tokens("1.23e10"), context).getValue(), is((Object)"1.23E10"));
        assertThat(parser.parseLiteral(tokens("1.23e+10"), context).getValue(), is((Object)"1.23E10"));
        assertThat(parser.parseLiteral(tokens("1.23e-10"), context).getValue(), is((Object)"1.23E-10"));
        assertThat(parser.parseLiteral(tokens("0"), context).getValue(), is((Object)"0"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345Z"), context).getValue(),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345UTC"), context).getValue(),
                   is((Object)"2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.3-01:00"), context).getValue(),
                   is((Object)"2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteral(tokens("2009-03-22T03:22:45.345+01:00"), context).getValue(),
                   is((Object)"2009-03-22T02:22:45.345Z"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoEndingParenthesis() {
        parser.parseLiteral(tokens("CAST(123 AS STRING OTHER"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoOpeningParenthesis() {
        parser.parseLiteral(tokens("CAST 123 AS STRING) OTHER"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndInvalidType() {
        parser.parseLiteral(tokens("CAST(123 AS FOOD) OTHER"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoAsKeyword() {
        parser.parseLiteral(tokens("CAST(123 STRING) OTHER"), context);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseLiteralFromStringWithCastAndNoLiteralValueBeforeAs() {
        parser.parseLiteral(tokens("CAST(AS STRING) OTHER"), context);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteralValue - unquoted
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeIntegerValues() {
        assertThat(parser.parseLiteralValue(tokens("123"), context), is("123"));
        assertThat(parser.parseLiteralValue(tokens("-123"), context), is("-123"));
        assertThat(parser.parseLiteralValue(tokens("- 123"), context), is("-123"));
        assertThat(parser.parseLiteralValue(tokens("+123"), context), is("123"));
        assertThat(parser.parseLiteralValue(tokens("+ 123"), context), is("123"));
        assertThat(parser.parseLiteralValue(tokens("0"), context), is("0"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeDecimalValues() {
        assertThat(parser.parseLiteralValue(tokens("1.23"), context), is("1.23"));
        assertThat(parser.parseLiteralValue(tokens("-1.23"), context), is("-1.23"));
        assertThat(parser.parseLiteralValue(tokens("+0.123"), context), is("0.123"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithPositiveAndNegativeDecimalValuesInScientificNotation() {
        assertThat(parser.parseLiteralValue(tokens("1.23"), context), is("1.23"));
        assertThat(parser.parseLiteralValue(tokens("1.23e10"), context), is("1.23E10"));
        assertThat(parser.parseLiteralValue(tokens("- 1.23e10"), context), is("-1.23E10"));
        assertThat(parser.parseLiteralValue(tokens("- 1.23e-10"), context), is("-1.23E-10"));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithBooleanValues() {
        assertThat(parser.parseLiteralValue(tokens("true"), context), is(Boolean.TRUE.toString()));
        assertThat(parser.parseLiteralValue(tokens("false"), context), is(Boolean.FALSE.toString()));
        assertThat(parser.parseLiteralValue(tokens("TRUE"), context), is(Boolean.TRUE.toString()));
        assertThat(parser.parseLiteralValue(tokens("FALSE"), context), is(Boolean.FALSE.toString()));
    }

    @Test
    public void shouldParseLiteralValueFromStringWithDateValues() {
        // sYYYY-MM-DDThh:mm:ss.sssTZD
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345Z"), context), is("2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345UTC"), context), is("2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.3-01:00"), context), is("2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("2009-03-22T03:22:45.345+01:00"), context), is("2009-03-22T02:22:45.345Z"));

        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345Z"), context), is("-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345UTC"), context), is("-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.3-01:00"), context), is("-2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("-2009-03-22T03:22:45.345+01:00"), context), is("-2009-03-22T02:22:45.345Z"));

        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345Z"), context), is("2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345UTC"), context), is("2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.3-01:00"), context), is("2009-03-22T04:22:45.300Z"));
        assertThat(parser.parseLiteralValue(tokens("+2009-03-22T03:22:45.345+01:00"), context), is("2009-03-22T02:22:45.345Z"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLiteralValue - quoted
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeIntegerValues() {
        assertThat(parser.parseLiteralValue(tokens("'123'"), context), is("123"));
        assertThat(parser.parseLiteralValue(tokens("'-123'"), context), is("-123"));
        assertThat(parser.parseLiteralValue(tokens("'- 123'"), context), is("- 123"));
        assertThat(parser.parseLiteralValue(tokens("'+123'"), context), is("+123"));
        assertThat(parser.parseLiteralValue(tokens("'+ 123'"), context), is("+ 123"));
        assertThat(parser.parseLiteralValue(tokens("'0'"), context), is("0"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeDecimalValues() {
        assertThat(parser.parseLiteralValue(tokens("'1.23'"), context), is("1.23"));
        assertThat(parser.parseLiteralValue(tokens("'-1.23'"), context), is("-1.23"));
        assertThat(parser.parseLiteralValue(tokens("'+0.123'"), context), is("+0.123"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithPositiveAndNegativeDecimalValuesInScientificNotation() {
        assertThat(parser.parseLiteralValue(tokens("'1.23'"), context), is("1.23"));
        assertThat(parser.parseLiteralValue(tokens("'1.23e10'"), context), is("1.23e10"));
        assertThat(parser.parseLiteralValue(tokens("'- 1.23e10'"), context), is("- 1.23e10"));
        assertThat(parser.parseLiteralValue(tokens("'- 1.23e-10'"), context), is("- 1.23e-10"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithBooleanValues() {
        assertThat(parser.parseLiteralValue(tokens("'true'"), context), is("true"));
        assertThat(parser.parseLiteralValue(tokens("'false'"), context), is("false"));
        assertThat(parser.parseLiteralValue(tokens("'TRUE'"), context), is("TRUE"));
        assertThat(parser.parseLiteralValue(tokens("'FALSE'"), context), is("FALSE"));
    }

    @Test
    public void shouldParseLiteralValueFromQuotedStringWithDateValues() {
        // sYYYY-MM-DDThh:mm:ss.sssTZD
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345Z'"), context), is("2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345UTC'"), context), is("2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.3-01:00'"), context), is("2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'2009-03-22T03:22:45.345+01:00'"), context),
                   is("2009-03-22T03:22:45.345+01:00"));

        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345Z'"), context), is("-2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345UTC'"), context), is("-2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.3-01:00'"), context),
                   is("-2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'-2009-03-22T03:22:45.345+01:00'"), context),
                   is("-2009-03-22T03:22:45.345+01:00"));

        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345Z'"), context), is("+2009-03-22T03:22:45.345Z"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345UTC'"), context), is("+2009-03-22T03:22:45.345UTC"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.3-01:00'"), context),
                   is("+2009-03-22T03:22:45.3-01:00"));
        assertThat(parser.parseLiteralValue(tokens("'+2009-03-22T03:22:45.345+01:00'"), context),
                   is("+2009-03-22T03:22:45.345+01:00"));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LENGTH
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLengthOfPropertyValue() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LENGTH(tableA.property)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(Length.class)));
        Length length = (Length)operand;
        assertThat(length.getPropertyValue().getPropertyName(), is(name("property")));
        assertThat(length.getPropertyValue().getSelectorName(), is(selectorName("tableA")));
        assertThat(length.getSelectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("LENGTH(property)"), context, source);
        assertThat(operand, is(instanceOf(Length.class)));
        length = (Length)operand;
        assertThat(length.getPropertyValue().getPropertyName(), is(name("property")));
        assertThat(length.getPropertyValue().getSelectorName(), is(selectorName("tableA")));
        assertThat(length.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLengthWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LENGTH(tableA.property other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLengthWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LENGTH tableA.property other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LOWER
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLowerOfAnotherDynamicOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOWER(tableA.property)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)operand;
        assertThat(lower.getSelectorName(), is(selectorName("tableA")));
        assertThat(lower.getOperand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)lower.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("LOWER(property)"), context, source);
        assertThat(operand, is(instanceOf(LowerCase.class)));
        lower = (LowerCase)operand;
        assertThat(lower.getSelectorName(), is(selectorName("tableA")));
        assertThat(lower.getOperand(), is(instanceOf(PropertyValue.class)));
        value = (PropertyValue)lower.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingLowerOfUpperCaseOfAnotherOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOWER(UPPER(tableA.property))"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)operand;
        assertThat(lower.getSelectorName(), is(selectorName("tableA")));
        assertThat(lower.getOperand(), is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)lower.getOperand();
        assertThat(upper.getSelectorName(), is(selectorName("tableA")));
        assertThat(upper.getOperand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)upper.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLowerWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LOWER(tableA.property other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLowerWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LOWER tableA.property other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - UPPER
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingUpperOfAnotherDynamicOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("UPPER(tableA.property)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)operand;
        assertThat(upper.getSelectorName(), is(selectorName("tableA")));
        assertThat(upper.getOperand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)upper.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));

        Source source = new NamedSelector(selectorName("tableA"));
        operand = parser.parseDynamicOperand(tokens("UPPER(property)"), context, source);
        assertThat(operand, is(instanceOf(UpperCase.class)));
        upper = (UpperCase)operand;
        assertThat(upper.getSelectorName(), is(selectorName("tableA")));
        assertThat(upper.getOperand(), is(instanceOf(PropertyValue.class)));
        value = (PropertyValue)upper.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingUpperOfLowerCaseOfAnotherOperand() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("UPPER(LOWER(tableA.property))"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(UpperCase.class)));
        UpperCase upper = (UpperCase)operand;
        assertThat(upper.getSelectorName(), is(selectorName("tableA")));
        assertThat(upper.getOperand(), is(instanceOf(LowerCase.class)));
        LowerCase lower = (LowerCase)upper.getOperand();
        assertThat(lower.getSelectorName(), is(selectorName("tableA")));
        assertThat(lower.getOperand(), is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)lower.getOperand();
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingUpperWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("UPPER(tableA.property other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingUpperWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Upper tableA.property other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - NAME
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingNameOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("NAME(tableA)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(NodeName.class)));
        NodeName name = (NodeName)operand;
        assertThat(name.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingNameWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("NAME()"), context, source);
        assertThat(operand, is(instanceOf(NodeName.class)));
        NodeName name = (NodeName)operand;
        assertThat(name.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("NAME()"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("NAME(tableA.property) other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("NAME(tableA other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingNameWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Name  tableA other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - LOCALNAME
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingLocalNameOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOCALNAME(tableA)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(NodeLocalName.class)));
        NodeLocalName name = (NodeLocalName)operand;
        assertThat(name.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingLocalNameWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("LOCALNAME()"), context, source);
        assertThat(operand, is(instanceOf(NodeLocalName.class)));
        NodeLocalName name = (NodeLocalName)operand;
        assertThat(name.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("LOCALNAME()"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("LOCALNAME(tableA.property) other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("LOCALNAME(tableA other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingLocalNameWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("LocalName  tableA other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - SCORE
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringContainingFullTextSearchScoreOfSelector() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("SCORE(tableA)"), context, mock(Source.class));
        assertThat(operand, is(instanceOf(FullTextSearchScore.class)));
        FullTextSearchScore score = (FullTextSearchScore)operand;
        assertThat(score.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringContainingFullTextSearchScoreWithNoSelectorOnlyIfThereIsOneSelectorAsSource() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("SCORE()"), context, source);
        assertThat(operand, is(instanceOf(FullTextSearchScore.class)));
        FullTextSearchScore score = (FullTextSearchScore)operand;
        assertThat(score.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithNoSelectorIfTheSourceIsNotASelector() {
        parser.parseDynamicOperand(tokens("SCORE()"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithWithSelectorNameAndProperty() {
        parser.parseDynamicOperand(tokens("SCORE(tableA.property) other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithoutClosingParenthesis() {
        parser.parseDynamicOperand(tokens("SCORE(tableA other"), context, mock(Source.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringContainingFullTextSearchScoreWithoutOpeningParenthesis() {
        parser.parseDynamicOperand(tokens("Score  tableA other"), context, mock(Source.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseDynamicOperand - PropertyValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseDynamicOperandFromStringWithUnquotedSelectorNameAndUnquotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("tableA.property"), context, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithQuotedSelectorNameAndUnquotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("[dna:tableA].property"), context, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("dna:tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithQuotedSelectorNameAndQuotedPropertyName() {
        DynamicOperand operand = parser.parseDynamicOperand(tokens("[dna:tableA].[dna:property]"), context, mock(Join.class));
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.getPropertyName(), is(name("dna:property")));
        assertThat(value.getSelectorName(), is(selectorName("dna:tableA")));
    }

    @Test
    public void shouldParseDynamicOperandFromStringWithOnlyPropertyNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        DynamicOperand operand = parser.parseDynamicOperand(tokens("property"), context, source);
        assertThat(operand, is(instanceOf(PropertyValue.class)));
        PropertyValue value = (PropertyValue)operand;
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToDynamicOperandValueFromStringWithOnlyPropertyNameIfSourceIsNotSelector() {
        parser.parsePropertyValue(tokens("property"), context, mock(Join.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseDynamicOperandFromStringWithOnlySelectorNameAndPeriod() {
        parser.parsePropertyValue(tokens("tableA. "), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePropertyValue
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePropertyValueFromStringWithUnquotedSelectorNameAndUnquotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("tableA.property"), context, mock(Join.class));
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithQuotedSelectorNameAndUnquotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("[dna:tableA].property"), context, mock(Join.class));
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("dna:tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithQuotedSelectorNameAndQuotedPropertyName() {
        PropertyValue value = parser.parsePropertyValue(tokens("[dna:tableA].[dna:property]"), context, mock(Join.class));
        assertThat(value.getPropertyName(), is(name("dna:property")));
        assertThat(value.getSelectorName(), is(selectorName("dna:tableA")));
    }

    @Test
    public void shouldParsePropertyValueFromStringWithOnlyPropertyNameIfSourceIsSelector() {
        Source source = new NamedSelector(selectorName("tableA"));
        PropertyValue value = parser.parsePropertyValue(tokens("property"), context, source);
        assertThat(value.getPropertyName(), is(name("property")));
        assertThat(value.getSelectorName(), is(selectorName("tableA")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyValueFromStringWithOnlyPropertyNameIfSourceIsNotSelector() {
        parser.parsePropertyValue(tokens("property"), context, mock(Join.class));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParsePropertyValueFromStringWithOnlySelectorNameAndPeriod() {
        parser.parsePropertyValue(tokens("tableA. "), context, mock(Join.class));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseLimit
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseLimitFromFormWithJustOneNumber() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10"));
        assertThat(limit.getRowLimit(), is(10));
        assertThat(limit.getOffset(), is(0));

        limit = parser.parseLimit(tokens("LIMIT 10 NONOFFSET"));
        assertThat(limit.getRowLimit(), is(10));
        assertThat(limit.getOffset(), is(0));
    }

    @Test
    public void shouldParseLimitFromFormWithRowLimitAndOffset() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10 OFFSET 30"));
        assertThat(limit.getRowLimit(), is(10));
        assertThat(limit.getOffset(), is(30));

        limit = parser.parseLimit(tokens("LIMIT 10 OFFSET 30 OTHER"));
        assertThat(limit.getRowLimit(), is(10));
        assertThat(limit.getOffset(), is(30));
    }

    @Test
    public void shouldParseLimitFromFormWithTwoCommaSeparatedNumbers() {
        Limit limit = parser.parseLimit(tokens("LIMIT 10,30"));
        assertThat(limit.getRowLimit(), is(20));
        assertThat(limit.getOffset(), is(10));
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
        NamedSelector selector = parser.parseNamedSelector(tokens("name"));
        assertThat(selector.getName(), is(selectorName("name")));
        assertThat(selector.getAlias(), is(nullValue()));
        assertThat(selector.getAliasOrName(), is(selectorName("name")));
    }

    @Test
    public void shouldParseNamedSelectorFromUnquotedNameWithUnquotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("name AS alias"));
        assertThat(selector.getName(), is(selectorName("name")));
        assertThat(selector.getAlias(), is(selectorName("alias")));
        assertThat(selector.getAliasOrName(), is(selectorName("alias")));
    }

    @Test
    public void shouldParseNamedSelectorFromQuotedNameWithUnquotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("'name' AS alias"));
        assertThat(selector.getName(), is(selectorName("name")));
        assertThat(selector.getAlias(), is(selectorName("alias")));
        assertThat(selector.getAliasOrName(), is(selectorName("alias")));
    }

    @Test
    public void shouldParseNamedSelectorFromQuotedNameWithQuotedAlias() {
        NamedSelector selector = parser.parseNamedSelector(tokens("'name' AS [alias]"));
        assertThat(selector.getName(), is(selectorName("name")));
        assertThat(selector.getAlias(), is(selectorName("alias")));
        assertThat(selector.getAliasOrName(), is(selectorName("alias")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParseNamedSelectorIfNoMoreTokens() {
        parser.parseNamedSelector(tokens("  "));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseSelectorName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseSelectorNameFromUnquotedString() {
        assertThat(parser.parseSelectorName(tokens("name")), is(selectorName("name")));
    }

    @Test
    public void shouldParseSelectorNameFromQuotedString() {
        assertThat(parser.parseSelectorName(tokens("'name'")), is(selectorName("name")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParseSelectorNameIfNoMoreTokens() {
        parser.parseSelectorName(tokens("  "));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNameFromSingleQuotedString() {
        assertThat(parser.parseName(tokens("'jcr:name'"), context), is(name("jcr:name")));
    }

    @Test
    public void shouldParseNameFromDoubleQuotedString() {
        assertThat(parser.parseName(tokens("\"jcr:name\""), context), is(name("jcr:name")));
    }

    @Test
    public void shouldParseNameFromBracketedString() {
        assertThat(parser.parseName(tokens("[jcr:name]"), context), is(name("jcr:name")));
    }

    @Test
    public void shouldParseNameFromUnquotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("name"), context), is(name("name")));
    }

    @Test
    public void shouldParseNameFromSingleQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("'name'"), context), is(name("name")));
    }

    @Test
    public void shouldParseNameFromDoubleQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("\"name\""), context), is(name("name")));
    }

    @Test
    public void shouldParseNameFromBracketedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("[name]"), context), is(name("name")));
    }

    @Test
    public void shouldParseNameFromBracketedAndQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("['name']"), context), is(name("name")));
        assertThat(parser.parseName(tokens("[\"name\"]"), context), is(name("name")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNameIfNoMoreTokens() {
        parser.parseName(tokens("  "), context);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parsePath
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePathFromUnquotedStringConsistingOfSql92Identifiers() {
        String identifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        assertThat(parser.parsePath(tokens(identifier), context), is(path(identifier)));
    }

    @Test
    public void shouldParsePathFromSingleQuotedString() {
        assertThat(parser.parsePath(tokens("'/a/b/c/dna:something/d'"), context), is(path("/a/b/c/dna:something/d")));
    }

    @Test
    public void shouldParsePathFromDoubleQuotedString() {
        assertThat(parser.parsePath(tokens("\"/a/b/c/dna:something/d\""), context), is(path("/a/b/c/dna:something/d")));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailInParsePathIfNoMoreTokens() {
        parser.parsePath(tokens("  "), context);
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
        parser.parseQuery(query, context);
    }

    protected SelectorName selectorName( String name ) {
        return new SelectorName(name);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected TokenStream tokens( String content ) {
        return new TokenStream(content, new SqlQueryParser.SqlTokenizer(false), false).start();
    }

}
