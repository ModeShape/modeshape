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
package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.FixFor;
import org.modeshape.common.text.TokenStream;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DescendantNodeJoinCondition;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.jcr.query.qom.JcrLiteral;

/**
 * 
 */
public class JcrSql2QueryParserTest {

    /**
     * Note that this ValueFactory is a simple mock that just returns null for each of its <code>createX()</code> methods, since
     * the {@link Value} objects are just placed into the {@link JcrLiteral} objects, and we never call
     * {@link JcrLiteral#getLiteralValue()} in these tests.
     */
    @Mock
    private ValueFactory valueFactory;
    private JcrTypeSystem typeSystem;
    private JcrSql2QueryParser parser;
    private Query query;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        typeSystem = new MockJcrTypeSystem(valueFactory);
        parser = new JcrSql2QueryParser();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseQuery
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNominalQueries() {
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived1' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived1' AND PATH() LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived2' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived%' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived2' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived1' AND PATH() LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived1' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived%' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNameArchived%' AND PATH() LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testRestPost/assets[%]/%' and  ( [drools:format]='drl' OR [drools:format]='xls' )  AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and [drools:archive] = 'true' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:packagesnapshot_area/testPackageSnapshot/PROD 2.0/assets[%]/%' and [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and [drools:format]='xyz' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and [drools:format]='xyz' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and  ( [drools:format]='xyz' OR [drools:format]='ABC' )  AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and [drools:format]='testSearchSharedAssetByFormat' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' and [drools:archive] = 'true' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not [drools:format]='drl' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( [drools:format]='drl' OR [drools:format]='wang' )  AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( [drools:format]='drl' OR [drools:format]='xls' )  AND [drools:archive] = 'false' ORDER BY [drools:title]");

        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNamex1' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNamex2' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNamex%' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNamex2' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE [drools:title] LIKE 'findRulesByNamex%' AND PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:subject] LIKE 'testQueryXXX42' AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND [drools:subject] LIKE 'testQueryXXX42' AND [drools:source] LIKE 'database'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false' AND [jcr:created] > '1974-07-10T00:00:00.000-05:00' AND [jcr:created] < '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false' AND [jcr:created] > '1974-07-10T00:00:00.000-05:00'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false' AND [jcr:created] < '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false' AND [jcr:created] > '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT [drools:title], [drools:description], [drools:archive] FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/%' AND ([drools:subject] LIKE 'testQueryXXX42' OR [drools:subject] LIKE 'wankle') AND ([drools:source] LIKE 'database' OR [drools:source] LIKE 'wankle') AND [drools:archive] = 'false' AND [jcr:created] < '1974-07-10T00:00:00.000-05:00'");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and [drools:format]='xyz' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("SELECT * FROM [drools:assetNodeType] WHERE PATH() LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and [drools:format]='xyz' AND [drools:archive] = 'false' ORDER BY [drools:title]");
        parse("select * from [mgnl:content] where PATH() like '/modules/%/templates'");
    }

    @Test
    public void shouldParseQueriesUsedInJcrTckTests() {
        parse("SELECT * FROM [nt:unstructured] JOIN [mix:referenceable] ON ISSAMENODE([nt:unstructured],[mix:referenceable]) WHERE PATH([nt:unstructured]) LIKE '/testroot/%'");
        parse("SELECT * FROM [nt:unstructured] JOIN [nt:base] ON ISSAMENODE([nt:base], [nt:unstructured]) WHERE PATH([nt:unstructured]) LIKE '/testroot/%'");
        parse("SELECT * FROM [nt:base] JOIN [mix:referenceable] ON ISSAMENODE([nt:base], [mix:referenceable]) WHERE PATH([nt:base]) LIKE '/testroot/%'");
        parse("SELECT * FROM [nt:unstructured] JOIN [mix:referenceable] ON ISSAMENODE([nt:unstructured], [mix:referenceable]) WHERE PATH([nt:unstructured]) LIKE '/testroot/%'");
        parse("SELECT prop1 FROM [nt:unstructured] WHERE prop2 IN ('two') AND prop1 = 'existence' AND PATH() LIKE '/testroot/%'");
    }

    @Test
    public void shouldParseSelectStarFromSingleSourceWithWhereContainingPathLikeConstraint() {
        query = parse("SELECT * FROM [mgnl:content] WHERE PATH() LIKE '/modules/%/templates'");
        assertThat(query.source(), is(instanceOf(NamedSelector.class)));
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        NamedSelector selector = (NamedSelector)query.source();
        assertThat(selector.name(), is(selectorName("mgnl:content")));
        assertThat(selector.aliasOrName(), is(selectorName("mgnl:content")));
        assertThat(selector.alias(), is(nullValue()));
        // WHERE ...
        Comparison comparison = isComparison(query.constraint());
        assertThat(comparison.operand1(), is((DynamicOperand)nodePath(selectorName("mgnl:content"))));
        assertThat(comparison.operand2(), is((StaticOperand)literal("/modules/%/templates")));
    }

    @Test
    public void shouldParseSelectStarFromSingleSourceWithWhereContainingTwoPathLikeConstraints() {
        query = parse("SELECT * FROM [mgnl:content] WHERE PATH() LIKE '/modules/%/templates' or PATH() like '/modules/%/other'");
        assertThat(query.source(), is(instanceOf(NamedSelector.class)));
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        NamedSelector selector = (NamedSelector)query.source();
        assertThat(selector.name(), is(selectorName("mgnl:content")));
        assertThat(selector.aliasOrName(), is(selectorName("mgnl:content")));
        assertThat(selector.alias(), is(nullValue()));
        // WHERE ...
        Or and = isOr(query.constraint());
        Comparison comparison1 = isComparison(and.left());
        assertThat(comparison1.operand1(), is((DynamicOperand)nodePath(selectorName("mgnl:content"))));
        assertThat(comparison1.operand2(), is((StaticOperand)literal("/modules/%/templates")));
        Comparison comparison2 = isComparison(and.right());
        assertThat(comparison2.operand1(), is((DynamicOperand)nodePath(selectorName("mgnl:content"))));
        assertThat(comparison2.operand2(), is((StaticOperand)literal("/modules/%/other")));
    }

    @Test
    public void shouldParseSelectStarFromTwoJoinedSourcesWithWhereContainingJoinCriteria() {
        query = parse("SELECT * FROM [mgnl:content] JOIN [acme:stuff] ON ISSAMENODE([mgnl:content],[acme:stuff])");
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        Join join = isJoin(query.source());
        assertThat(join.left(), is((Source)namedSelector(selectorName("mgnl:content"))));
        assertThat(join.right(), is((Source)namedSelector(selectorName("acme:stuff"))));
        assertThat(join.type(), is(JoinType.INNER));
        SameNodeJoinCondition joinCondition = isSameNodeJoinCondition(join.joinCondition());
        assertThat(joinCondition.selector1Name(), is(selectorName("mgnl:content")));
        assertThat(joinCondition.selector2Name(), is(selectorName("acme:stuff")));
        assertThat(joinCondition.selector2Path(), is(nullValue()));
        // WHERE ...
        assertThat(query.constraint(), is(nullValue()));
    }

    @Test
    public void shouldParseSelectStarFromThreeJoinedSourcesWithWhereContainingJoinCriteria() {
        query = parse("SELECT * FROM [mgnl:content] JOIN [acme:stuff] ON ISSAMENODE([mgnl:content],[acme:stuff]) JOIN [foo:bar] ON ISSAMENODE([mgnl:content],[foo:bar])");
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        Join join = isJoin(query.source());
        Join join2 = isJoin(join.left());
        assertThat(join2.left(), is((Source)namedSelector(selectorName("mgnl:content"))));
        assertThat(join2.right(), is((Source)namedSelector(selectorName("acme:stuff"))));
        assertThat(join2.type(), is(JoinType.INNER));
        SameNodeJoinCondition joinCondition2 = isSameNodeJoinCondition(join2.joinCondition());
        assertThat(joinCondition2.selector1Name(), is(selectorName("mgnl:content")));
        assertThat(joinCondition2.selector2Name(), is(selectorName("acme:stuff")));
        assertThat(joinCondition2.selector2Path(), is(nullValue()));

        assertThat(join.right(), is((Source)namedSelector(selectorName("foo:bar"))));
        assertThat(join.type(), is(JoinType.INNER));
        SameNodeJoinCondition joinCondition = isSameNodeJoinCondition(join.joinCondition());
        assertThat(joinCondition.selector1Name(), is(selectorName("mgnl:content")));
        assertThat(joinCondition.selector2Name(), is(selectorName("foo:bar")));
        assertThat(joinCondition.selector2Path(), is(nullValue()));

        // WHERE ...
        assertThat(query.constraint(), is(nullValue()));
    }

    @Test
    public void shouldParseSelectStarFromEquijoinAndAdditionalCriteria() {
        query = parse("SELECT * FROM [modetest:queryable] JOIN [mix:referenceable] ON ISSAMENODE([modetest:queryable],[mix:referenceable]) WHERE PATH([modetest:queryable]) LIKE '/testroot/someQueryableNodeD/%'");
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        Join join = isJoin(query.source());
        assertThat(join.left(), is((Source)namedSelector(selectorName("modetest:queryable"))));
        assertThat(join.right(), is((Source)namedSelector(selectorName("mix:referenceable"))));
        assertThat(join.type(), is(JoinType.INNER));
        SameNodeJoinCondition joinCondition = isSameNodeJoinCondition(join.joinCondition());
        assertThat(joinCondition.selector1Name(), is(selectorName("modetest:queryable")));
        assertThat(joinCondition.selector2Name(), is(selectorName("mix:referenceable")));
        assertThat(joinCondition.selector2Path(), is(nullValue()));
        // WHERE ...
        Comparison comparison = isComparison(query.constraint());
        assertThat(comparison.operand1(), is((DynamicOperand)nodePath(selectorName("modetest:queryable"))));
        assertThat(comparison.operand2(), is((StaticOperand)literal("/testroot/someQueryableNodeD/%")));
    }

    @Test
    public void shouldParseSelectWithOrderByClause() {
        query = parse("SELECT [car:model] FROM [car:Car] WHERE [car:model] IS NOT NULL ORDER BY [car:model] ASC");
        // SELECT car:model ...
        assertThat(query.columns().size(), is(1));
        assertThat(query.columns().get(0).selectorName(), is(selectorName("car:Car")));
        assertThat(query.columns().get(0).columnName(), is("car:model"));
        assertThat(query.columns().get(0).propertyName(), is("car:model"));
        // FROM ...
        NamedSelector selector = (NamedSelector)query.source();
        assertThat(selector.name(), is(selectorName("car:Car")));
        assertThat(selector.aliasOrName(), is(selectorName("car:Car")));
        assertThat(selector.alias(), is(nullValue()));
        // WHERE ...
        PropertyExistence constraint = isPropertyExistence(query.constraint());
        assertThat(constraint.propertyName(), is("car:model"));
        assertThat(constraint.selectorName(), is(selectorName("car:Car")));
        // ORDER BY ...
        assertThat(query.orderings().size(), is(1));
        Ordering ordering = query.orderings().get(0);
        assertThat(ordering.order(), is(Order.ASCENDING));
        assertThat(ordering.operand(), is((DynamicOperand)propertyValue(selectorName("car:Car"), "car:model")));
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     */
    @Test
    public void shouldParseSelectWithChildAxisCriteria() {
        query = parse("SELECT * FROM [nt:base] WHERE PATH() LIKE '/a/b/%' AND NOT PATH() LIKE '/a/b/%/%'");
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        NamedSelector selector = (NamedSelector)query.source();
        assertThat(selector.name(), is(selectorName("nt:base")));
        assertThat(selector.aliasOrName(), is(selectorName("nt:base")));
        assertThat(selector.alias(), is(nullValue()));
        // WHERE ...
        And and = isAnd(query.constraint());
        Comparison comparison1 = isComparison(and.left());
        assertThat(comparison1.operand1(), is((DynamicOperand)nodePath(selectorName("nt:base"))));
        assertThat(comparison1.operand2(), is((StaticOperand)literal("/a/b/%")));
        Not not = isNot(and.right());
        Comparison comparison2a = isComparison(not.constraint());
        assertThat(comparison2a.operand1(), is((DynamicOperand)nodePath(selectorName("nt:base"))));
        assertThat(comparison2a.operand2(), is((StaticOperand)literal("/a/b/%/%")));
    }

    @Test
    public void shouldParseDescendantNodeJoinWithNoCriteria() {
        query = parse("select * from [lom:Metadata] as lom join [lom:LangString] as lang on isdescendantnode(lang,lom)");
        // SELECT * ...
        assertThat(query.columns().isEmpty(), is(true));
        // FROM ...
        Join join = isJoin(query.source());
        assertThat(join.left(), is((Source)namedSelector(selectorName("lom:Metadata"), selectorName("lom"))));
        assertThat(join.right(), is((Source)namedSelector(selectorName("lom:LangString"), selectorName("lang"))));
        assertThat(join.type(), is(JoinType.INNER));
        DescendantNodeJoinCondition joinCondition = isDescendantNodeJoinCondition(join.joinCondition());
        assertThat(joinCondition.ancestorSelectorName(), is(selectorName("lom")));
        assertThat(joinCondition.descendantSelectorName(), is(selectorName("lang")));
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelect() {
        query = parse("select [jcr:primaryType], [jcr:path] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeNameInSelect() {
        query = parse("select [jcr:primaryType], [jcr:name] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeLocalNameInSelect() {
        query = parse("select [jcr:primaryType], [jcr:localName] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeDepthInSelect() {
        query = parse("select [jcr:primaryType], [jcr:depth] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeScoreInSelect() {
        query = parse("select [jcr:primaryType], [jcr:score] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedPathInSelect() {
        query = parse("select [nt:base].[jcr:primaryType], [nt:base].[jcr:path] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeNameInSelect() {
        query = parse("select [nt:base].[jcr:primaryType], [nt:base].[jcr:name] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeLocalNameInSelect() {
        query = parse("select [nt:base].[jcr:primaryType], [nt:base].[jcr:localName] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeDepthInSelect() {
        query = parse("select [nt:base].[jcr:primaryType], [nt:base].[jcr:depth] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeScoreInSelect() {
        query = parse("select [nt:base].[jcr:primaryType], [nt:base].[jcr:score] FROM [nt:base]");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInCriteria() {
        query = parse("select [jcr:primaryType] FROM [nt:base] WHERE [jcr:path] = '/some/path'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeNameInCriteria() {
        query = parse("select [jcr:primaryType] FROM [nt:base] WHERE [jcr:path] = 'mode:nodeName'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeLocalNameInCriteria() {
        query = parse("select [jcr:primaryType] FROM [nt:base] WHERE [jcr:localName] = 'nodeName'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeDepthInCriteria() {
        query = parse("select [jcr:primaryType] FROM [nt:base] WHERE [jcr:depth] = 2");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedNodeScoreInCriteria() {
        query = parse("select [jcr:primaryType] FROM [nt:base] WHERE [jcr:score] <= 2.0");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedPathInCriteria() {
        query = parse("select [nt:base].[jcr:primaryType] FROM [nt:base] WHERE [nt:base].[jcr:path] = '/some/path'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeNameInCriteria() {
        query = parse("select [nt:base].[jcr:primaryType] FROM [nt:base] WHERE [nt:base].[jcr:name] = 'mode:nodeName'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeLocalNameInCriteria() {
        query = parse("select [nt:base].[jcr:primaryType] FROM [nt:base] WHERE [nt:base].[jcr:localName] = 'nodeName'");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeDepthInCriteria() {
        query = parse("select [nt:base].[jcr:primaryType] FROM [nt:base] WHERE [nt:base].[jcr:depth] = 3");
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithQualifiedNodeScoreInCriteria() {
        query = parse("select [nt:base].[jcr:primaryType] FROM [nt:base] WHERE [nt:base].[jcr:score] <= 1.3");
    }

    protected Join isJoin( Source source ) {
        assertThat(source, is(instanceOf(Join.class)));
        return (Join)source;
    }

    protected PropertyExistence isPropertyExistence( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(PropertyExistence.class)));
        return (PropertyExistence)constraint;
    }

    protected Not isNot( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(Not.class)));
        return (Not)constraint;
    }

    protected Comparison isComparison( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(Comparison.class)));
        return (Comparison)constraint;
    }

    protected SameNodeJoinCondition isSameNodeJoinCondition( JoinCondition condition ) {
        assertThat(condition, is(instanceOf(SameNodeJoinCondition.class)));
        return (SameNodeJoinCondition)condition;
    }

    protected DescendantNodeJoinCondition isDescendantNodeJoinCondition( JoinCondition condition ) {
        assertThat(condition, is(instanceOf(DescendantNodeJoinCondition.class)));
        return (DescendantNodeJoinCondition)condition;
    }

    protected And isAnd( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(And.class)));
        return (And)constraint;
    }

    protected Or isOr( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(Or.class)));
        return (Or)constraint;
    }

    protected NodePath nodePath( SelectorName name ) {
        return new NodePath(name);
    }

    protected PropertyValue propertyValue( SelectorName selectorName,
                                           String propertyName ) {
        return new PropertyValue(selectorName, propertyName);
    }

    protected Literal literal( Object value ) {
        return new Literal(value);
    }

    protected NamedSelector namedSelector( SelectorName selectorName ) {
        return new NamedSelector(selectorName);
    }

    protected NamedSelector namedSelector( SelectorName selectorName,
                                           SelectorName alias ) {
        return new NamedSelector(selectorName, alias);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // removeBracketsAndQuotes
    // ----------------------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------
    protected Query parse( String query ) {
        QueryCommand command = parseCommand(query);
        assertThat(command, is(instanceOf(Query.class)));
        return (Query)command;
    }

    protected QueryCommand parseCommand( String query ) {
        return parser.parseQuery(query, typeSystem);
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

    protected TokenStream tokens( String content ) {
        return new TokenStream(content, new SqlQueryParser.SqlTokenizer(false), false).start();
    }

    protected static class MockJcrTypeSystem implements JcrTypeSystem {
        protected final ValueFactory valueFactory;
        protected final TypeSystem delegate;
        protected final ExecutionContext executionContext;

        protected MockJcrTypeSystem( ValueFactory valueFactory ) {
            this.executionContext = new ExecutionContext();
            this.valueFactory = valueFactory;
            this.delegate = this.executionContext.getValueFactories().getTypeSystem();
        }

        @Override
        public Set<String> getTypeNames() {
            return delegate.getTypeNames();
        }

        @Override
        public TypeFactory<?> getTypeFactory( Object prototype ) {
            return delegate.getTypeFactory(prototype);
        }

        @Override
        public TypeFactory<?> getTypeFactory( String typeName ) {
            return delegate.getTypeFactory(typeName);
        }

        @Override
        public TypeFactory<String> getStringFactory() {
            return delegate.getStringFactory();
        }

        @Override
        public TypeFactory<?> getReferenceFactory() {
            return delegate.getReferenceFactory();
        }

        @Override
        public TypeFactory<?> getPathFactory() {
            return delegate.getPathFactory();
        }

        @Override
        public TypeFactory<Long> getLongFactory() {
            return delegate.getLongFactory();
        }

        @Override
        public TypeFactory<Double> getDoubleFactory() {
            return delegate.getDoubleFactory();
        }

        @Override
        public String getDefaultType() {
            return delegate.getDefaultType();
        }

        @Override
        public Comparator<Object> getDefaultComparator() {
            return delegate.getDefaultComparator();
        }

        @Override
        public TypeFactory<BigDecimal> getDecimalFactory() {
            return delegate.getDecimalFactory();
        }

        @Override
        public TypeFactory<?> getDateTimeFactory() {
            return delegate.getDateTimeFactory();
        }

        @Override
        public String getCompatibleType( String type1,
                                         String type2 ) {
            return delegate.getCompatibleType(type1, type2);
        }

        @Override
        public TypeFactory<Boolean> getBooleanFactory() {
            return delegate.getBooleanFactory();
        }

        @Override
        public TypeFactory<?> getBinaryFactory() {
            return delegate.getBinaryFactory();
        }

        @Override
        public String asString( Object value ) {
            return delegate.asString(value);
        }

        @Override
        public ValueFactory getValueFactory() {
            return valueFactory;
        }
    }
}
