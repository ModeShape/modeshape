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
package org.modeshape.graph.query;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.Visitors;

/**
 * 
 */
public class QueryBuilderTest {

    private QueryBuilder builder;
    private QueryCommand query;

    @Before
    public void beforeEach() {
        builder = new QueryBuilder(new ExecutionContext().getValueFactories().getTypeSystem());
    }

    protected void assertThatSql( QueryCommand query,
                                  Matcher<String> expected ) {
        assertThat(Visitors.readable(query), expected);
    }

    @Test
    public void shouldBuildSelectStarFromAllNodes() {
        query = builder.selectStar().fromAllNodes().query();
        assertThatSql(query, is("SELECT * FROM __ALLNODES__"));
    }

    @Test
    public void shouldBuildSelectStarFromAllNodesAs() {
        query = builder.selectStar().fromAllNodesAs("nodes").query();
        assertThatSql(query, is("SELECT * FROM __ALLNODES__ AS nodes"));
    }

    @Test
    public void shouldBuildSelectStarWithoutOtherClausesAsFromAllNodes() {
        query = builder.selectStar().query();
        assertThatSql(query, is("SELECT * FROM __ALLNODES__"));
    }

    @Test
    public void shouldBuildSelectColumnsFromAllNodes() {
        query = builder.select("col1", "col2").fromAllNodes().query();
        assertThatSql(query, is("SELECT __ALLNODES__.col1,__ALLNODES__.col2 FROM __ALLNODES__"));
    }

    @Test
    public void shouldBuildSelectColumnsFromAllNodesAs() {
        query = builder.select("col1", "col2").fromAllNodesAs("nodes").query();
        assertThatSql(query, is("SELECT nodes.col1,nodes.col2 FROM __ALLNODES__ AS nodes"));
    }

    @Test
    public void shouldBuildSelectColumnsUsingAliasFromAllNodesAs() {
        query = builder.select("col1", "nodes.col2").fromAllNodesAs("nodes").query();
        assertThatSql(query, is("SELECT nodes.col1,nodes.col2 FROM __ALLNODES__ AS nodes"));
    }

    @Test
    public void shouldBuildSelectStarFromOneTable() {
        query = builder.selectStar().from("table").query();
        assertThatSql(query, is("SELECT * FROM table"));
    }

    @Test
    public void shouldBuildSelectStarFromOneTableAs() {
        query = builder.selectStar().from("table AS nodes").query();
        assertThatSql(query, is("SELECT * FROM table AS nodes"));
    }

    @Test
    public void shouldBuildSelectColumnsFromOneTable() {
        query = builder.select("col1", "col2").from("table").query();
        assertThatSql(query, is("SELECT table.col1,table.col2 FROM table"));
    }

    @Test
    public void shouldBuildSelectColumnsFromOneTableAs() {
        query = builder.select("col1", "col2").from("table AS nodes").query();
        assertThatSql(query, is("SELECT nodes.col1,nodes.col2 FROM table AS nodes"));
    }

    @Test
    public void shouldBuildSelectColumnsUsingAliasFromOneTableAs() {
        query = builder.select("col1", "nodes.col2").from("table AS  nodes").query();
        assertThatSql(query, is("SELECT nodes.col1,nodes.col2 FROM table AS nodes"));
    }

    @Test
    public void shouldBuildUnionFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .union()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes UNION SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildUnionAllFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .unionAll()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes UNION ALL SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildUnionAllFromThreeSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .union()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .unionAll()
                       .select("col5", "col6")
                       .from("table3")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes UNION SELECT edges.col3,edges.col4 FROM table2 AS edges UNION ALL SELECT table3.col5,table3.col6 FROM table3"));
    }

    @Test
    public void shouldBuildIntersectFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .intersect()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes INTERSECT SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildIntersectAllFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .intersectAll()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes INTERSECT ALL SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildExceptFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .intersect()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes INTERSECT SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildExceptAllFromTwoSimpleSelects() {
        query = builder.select("col1", "nodes.col2")
                       .from("table1 AS  nodes")
                       .intersectAll()
                       .select("col3", "edges.col4")
                       .from("table2 AS  edges")
                       .query();
        assertThatSql(query,
                      is("SELECT nodes.col1,nodes.col2 FROM table1 AS nodes INTERSECT ALL SELECT edges.col3,edges.col4 FROM table2 AS edges"));
    }

    @Test
    public void shouldBuildEquiJoin() {
        query = builder.select("t1.c1", "t2.c2").from("table1 AS  t1").join("table2 as t2").on(" t1.c0= t2. c0").query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 INNER JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildInnerEquiJoin() {
        query = builder.select("t1.c1", "t2.c2").from("table1 AS  t1").innerJoin("table2 as t2").on(" t1.c0= t2. c0").query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 INNER JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildLeftOuterEquiJoin() {
        query = builder.select("t1.c1", "t2.c2").from("table1 AS  t1").leftOuterJoin("table2 as t2").on(" t1.c0= t2. c0").query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 LEFT OUTER JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildRightOuterEquiJoin() {
        query = builder.select("t1.c1", "t2.c2")
                       .from("table1 AS  t1")
                       .rightOuterJoin("table2 as t2")
                       .on(" t1.c0= t2. c0")
                       .query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 RIGHT OUTER JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildFullOuterEquiJoin() {
        query = builder.select("t1.c1", "t2.c2").from("table1 AS  t1").fullOuterJoin("table2 as t2").on(" t1.c0= t2. c0").query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 FULL OUTER JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildCrossEquiJoin() {
        query = builder.select("t1.c1", "t2.c2").from("table1 AS  t1").crossJoin("table2 as t2").on(" t1.c0= t2. c0").query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 CROSS JOIN table2 as t2 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldBuildMultiJoinUsingEquiJoinCriteria() {
        query = builder.select("t1.c1", "t2.c2")
                       .from("table1 AS  t1")
                       .join("table2 as t2")
                       .on(" t1.c0= t2. c0")
                       .join("table3 as t3")
                       .on(" t1.c0= t3. c0")
                       .query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 " + //
                                "INNER JOIN table2 as t2 ON t1.c0 = t2.c0 " + //
                                "INNER JOIN table3 as t3 ON t1.c0 = t3.c0"));
    }

    @Test
    public void shouldBuildMultiJoinAndCrossUsingEquiJoinCriteria() {
        query = builder.select("t1.c1", "t2.c2")
                       .from("table1 AS  t1")
                       .join("table2 as t2")
                       .on(" t1.c0= t2. c0")
                       .crossJoin("table3 as t3")
                       .on(" t1.c0= t3. c0")
                       .query();
        assertThatSql(query, is("SELECT t1.c1,t2.c2 FROM table1 AS t1 " + //
                                "INNER JOIN table2 as t2 " + //
                                "CROSS JOIN table3 as t3 ON t1.c0 = t3.c0 ON t1.c0 = t2.c0"));
    }

    @Test
    public void shouldAddNoConstraintsIfConstraintBuilderIsNotUsedButIsEnded() {
        query = builder.selectStar().from("table AS nodes").where().end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes"));
    }

    @Test
    public void shouldBuildQueryWithBetweenRange() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .propertyValue("nodes", "col1")
                       .isBetween()
                       .literal("lower")
                       .and()
                       .literal(true)
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE nodes.col1 BETWEEN 'lower' AND true"));

        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .propertyValue("nodes", "col1")
                       .isBetween()
                       .literal("lower")
                       .and()
                       .literal("upper")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE nodes.col1 BETWEEN 'lower' AND 'upper'"));
    }

    @Test
    public void shouldBuildQueryWithBetweenRangeWithCast() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .propertyValue("nodes", "col1")
                       .isBetween()
                       .cast("true")
                       .asBoolean()
                       .and()
                       .cast("false")
                       .asBoolean()
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE nodes.col1 BETWEEN true AND false"));
    }

    @Test
    public void shouldBuildQueryWithOneHasPropertyConstraint() {
        query = builder.selectStar().from("table AS nodes").where().hasProperty("nodes", "col1").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE nodes.col1 IS NOT NULL"));
    }

    @Test
    public void shouldBuildQueryWithChildConstraint() {
        query = builder.selectStar().from("table AS nodes").where().isChild("nodes", "/parent/path").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE ISCHILDNODE(nodes,/parent/path)"));
    }

    @Test
    public void shouldBuildQueryWithDescendantConstraint() {
        query = builder.selectStar().from("table AS nodes").where().isBelowPath("nodes", "/parent/path").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE ISDESCENDANTNODE(nodes,/parent/path)"));
    }

    @Test
    public void shouldBuildQueryWithSameNodeConstraint() {
        query = builder.selectStar().from("table AS nodes").where().isSameNode("nodes", "/other/path").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE ISSAMENODE(nodes,/other/path)"));
    }

    @Test
    public void shouldBuildQueryWithFullTextSearchConstraint() {
        query = builder.selectStar().from("table AS nodes").where().search("nodes", "expression").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE CONTAINS(nodes,'expression')"));
    }

    @Test
    public void shouldBuildQueryWithPropertyFullTextSearchConstraint() {
        query = builder.selectStar().from("table AS nodes").where().search("nodes", "property", "expression").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE CONTAINS(nodes.property,'expression')"));
    }

    @Test
    public void shouldBuildQueryWithTwoHasPropertyConstraint() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .hasProperty("nodes", "col2")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes WHERE (nodes.col1 IS NOT NULL AND nodes.col2 IS NOT NULL)"));
    }

    @Test
    public void shouldBuildQueryWithThreeHasPropertyConstraint() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .hasProperty("nodes", "col2")
                       .and()
                       .hasProperty("nodes", "col3")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE ((nodes.col1 IS NOT NULL " + //
                                "AND nodes.col2 IS NOT NULL) " + //
                                "AND nodes.col3 IS NOT NULL)"));
    }

    @Test
    public void shouldBuildQueryWithCorrectPrecedenceWithAndAndOr() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .or()
                       .hasProperty("nodes", "col2")
                       .and()
                       .hasProperty("nodes", "col3")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE (nodes.col1 IS NOT NULL " + //
                                "OR (nodes.col2 IS NOT NULL " + //
                                "AND nodes.col3 IS NOT NULL))"));
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .hasProperty("nodes", "col2")
                       .or()
                       .hasProperty("nodes", "col3")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE ((nodes.col1 IS NOT NULL " + //
                                "AND nodes.col2 IS NOT NULL) " + //
                                "OR nodes.col3 IS NOT NULL)"));
    }

    @Test
    public void shouldBuildQueryWithMixtureOfLogicalWithExplicitParenthesesWithHasPropertyConstraint() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .openParen()
                       .hasProperty("nodes", "col2")
                       .and()
                       .hasProperty("nodes", "col3")
                       .closeParen()
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE (nodes.col1 IS NOT NULL " + //
                                "AND (nodes.col2 IS NOT NULL " + //
                                "AND nodes.col3 IS NOT NULL))"));
    }

    @Test
    public void shouldBuildQueryWithCorrectPrecedenceWithExplicitParentheses() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .openParen()
                       .hasProperty("nodes", "col1")
                       .or()
                       .hasProperty("nodes", "col2")
                       .closeParen()
                       .and()
                       .hasProperty("nodes", "col3")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE ((nodes.col1 IS NOT NULL " + //
                                "OR nodes.col2 IS NOT NULL) " + //
                                "AND nodes.col3 IS NOT NULL)"));
    }

    @Test
    public void shouldBuildQueryWithCorrectPrecedenceWithExplicitParenthesesWithAndFirst() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .openParen()
                       .hasProperty("nodes", "col2")
                       .or()
                       .hasProperty("nodes", "col3")
                       .closeParen()
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE (nodes.col1 IS NOT NULL " + //
                                "AND (nodes.col2 IS NOT NULL " + //
                                "OR nodes.col3 IS NOT NULL))"));
    }

    @Test
    public void shouldBuildQueryWithMixureOfLogicalWithMultipleExplicitParenthesesWithHasPropertyConstraint() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .hasProperty("nodes", "col1")
                       .and()
                       .openParen()
                       .openParen()
                       .hasProperty("nodes", "col2")
                       .and()
                       .hasProperty("nodes", "col3")
                       .closeParen()
                       .and()
                       .search("nodes", "expression")
                       .closeParen()
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE (nodes.col1 IS NOT NULL " + //
                                "AND ((nodes.col2 IS NOT NULL " + //
                                "AND nodes.col3 IS NOT NULL) " + //
                                "AND CONTAINS(nodes,'expression')))"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingPlus() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .depth("nodes")
                       .plus()
                       .depth("nodes")
                       .plus()
                       .depth("nodes")
                       .isEqualTo(3)
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE ((DEPTH(nodes) + DEPTH(nodes)) + DEPTH(nodes)) = 3"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingPlusAndMinus() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .depth("nodes")
                       .minus()
                       .depth("nodes")
                       .plus()
                       .fullTextSearchScore("nodes")
                       .isEqualTo(3)
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE (DEPTH(nodes) - (DEPTH(nodes) + SCORE(nodes))) = 3"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) = $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthNotEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isNotEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) != 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthNotEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isNotEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) != $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLessThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isLessThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) < 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLessThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isLessThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) < $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLessThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isLessThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) <= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isLessThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) <= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthGreaterThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isGreaterThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) > 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthGreaterThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isGreaterThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) > $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthGreaterThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isGreaterThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) >= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthGreaterThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isGreaterThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) >= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLike() {
        query = builder.selectStar().from("table AS nodes").where().length("nodes", "property").isLike("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) LIKE 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLengthLikeVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .length("nodes", "property")
                       .isLikeVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LENGTH(nodes.property) LIKE $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeDepthEqualToLiteral() {
        query = builder.selectStar().from("table AS nodes").where().depth("nodes").isEqualTo(3).end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE DEPTH(nodes) = 3"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeDepthLessThanOrEqualToLongLiteral() {
        query = builder.selectStar().from("table AS nodes").where().depth("nodes").isLessThanOrEqualTo(3).end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE DEPTH(nodes) <= 3"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeDepthLessThanOrEqualToStringLiteral() {
        query = builder.selectStar().from("table AS nodes").where().depth("nodes").isLessThanOrEqualTo(3).end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE DEPTH(nodes) <= 3"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeDepthLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .depth("nodes")
                       .isLessThanOrEqualToVariable("value")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE DEPTH(nodes) <= $value"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameEqualTo() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isEqualTo("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameEqualToVariable() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isEqualToVariable("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) = $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameNotEqualTo() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isNotEqualTo("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) != 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameNotEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isNotEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) != $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLessThan() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isLessThan("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) < 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLessThanVariable() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isLessThanVariable("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) < $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLessThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isLessThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) <= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isLessThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) <= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameGreaterThan() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isGreaterThan("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) > 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameGreaterThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isGreaterThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) > $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameGreaterThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) >= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameGreaterThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) >= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLike() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isLike("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) LIKE 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeNameLikeVariable() {
        query = builder.selectStar().from("table AS nodes").where().nodeName("nodes").isLikeVariable("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) LIKE $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameEqualTo() {
        query = builder.selectStar().from("table AS nodes").where().nodeLocalName("nodes").isEqualTo("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) = $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameNotEqualTo() {
        query = builder.selectStar().from("table AS nodes").where().nodeLocalName("nodes").isNotEqualTo("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) != 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameNotEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isNotEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) != $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLessThan() {
        query = builder.selectStar().from("table AS nodes").where().nodeLocalName("nodes").isLessThan("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) < 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLessThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isLessThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) < $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLessThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isLessThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) <= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isLessThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) <= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameGreaterThan() {
        query = builder.selectStar().from("table AS nodes").where().nodeLocalName("nodes").isGreaterThan("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) > 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameGreaterThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isGreaterThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) > $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameGreaterThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isGreaterThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) >= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameGreaterThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isGreaterThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) >= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLike() {
        query = builder.selectStar().from("table AS nodes").where().nodeLocalName("nodes").isLike("literal").end().query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) LIKE 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingNodeLocalNameLikeVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeLocalName("nodes")
                       .isLikeVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOCALNAME(nodes) LIKE $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) = $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameNotEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isNotEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) != 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameNotEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isNotEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) != $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLessThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLessThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) < 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLessThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLessThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) < $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLessThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLessThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) <= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLessThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) <= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameGreaterThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isGreaterThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) > 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameGreaterThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) > $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameGreaterThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) >= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameGreaterThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) >= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLike() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLike("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) LIKE 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingUppercaseOfNodeNameLikeVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isLikeVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE UPPER(NAME(nodes)) LIKE $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) = $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameNotEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isNotEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) != 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameNotEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isNotEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) != $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLessThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLessThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) < 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLessThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLessThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) < $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLessThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLessThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) <= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLessThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLessThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) <= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameGreaterThan() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isGreaterThan("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) > 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameGreaterThanVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) > $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameGreaterThanOrEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) >= 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameGreaterThanOrEqualToVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isGreaterThanOrEqualToVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) >= $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLike() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLike("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) LIKE 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfNodeNameLikeVariable() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .nodeName("nodes")
                       .isLikeVariable("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(NAME(nodes)) LIKE $literal"));
    }

    @Test
    public void shouldBuildQueryWithCriteriaUsingLowercaseOfUppercaseOfNodeNameEqualTo() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .lowerCaseOf()
                       .upperCaseOf()
                       .nodeName("nodes")
                       .isEqualTo("literal")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE LOWER(UPPER(NAME(nodes))) = 'literal'"));
    }

    @Test
    public void shouldBuildQueryWithSetCriteria() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .where()
                       .nodeName("nodes")
                       .isIn("value1", "value2", "value3")
                       .end()
                       .query();
        assertThatSql(query, is("SELECT * FROM table AS nodes " + //
                                "WHERE NAME(nodes) IN ('value1','value2','value3')"));
    }

    @Test
    public void shouldBuildQueryWithOneOrderByClause() {
        query = builder.selectStar()
                       .from("table AS nodes")
                       .orderBy()
                       .ascending()
                       .fullTextSearchScore("nodes")
                       .then()
                       .descending()
                       .length("nodes", "column")
                       .end()
                       .query();
    }
}
