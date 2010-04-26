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
package org.modeshape.jcr.sql;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.TokenStream;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.SqlQueryParser;

/**
 * 
 */
public class JcrSqlQueryParserTest {

    private TypeSystem typeSystem;
    private JcrSqlQueryParser parser;
    private Query query;

    @Before
    public void beforeEach() {
        typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        parser = new JcrSqlQueryParser();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseQuery
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNominalQueries() {
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%'");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestPost/assets[%]/%' and  ( drools:format='drl' OR drools:format='xls' )  AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:archive = 'true' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:packagesnapshot_area/testPackageSnapshot/PROD 2.0/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and  ( drools:format='xyz' OR drools:format='ABC' )  AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='testSearchSharedAssetByFormat' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' and drools:archive = 'true' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( drools:format='drl' OR drools:format='wang' )  AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( drools:format='drl' OR drools:format='xls' )  AND drools:archive = 'false' ORDER BY drools:title");

        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:subject LIKE 'testQueryXXX42' AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:subject LIKE 'testQueryXXX42' AND drools:source LIKE 'database'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00' AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '3074-07-10T00:00:00.000-05:00'");
        parse("SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created < TIMESTAMP '1974-07-10T00:00:00.000-05:00'");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title");
        parse("SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title");
        parse("select * from mgnl:content where jcr:path like '/modules/%/templates'");
    }

    @Test
    public void shouldParseSelectStarFromSingleSourceWherePathLikeValue() {
        query = parse("SELECT * FROM mgnl:content WHERE jcr:path LIKE '/modules/%/templates'");
        assertThat(query.getSource(), is(instanceOf(NamedSelector.class)));
        // SELECT * ...
        assertThat(query.getColumns().isEmpty(), is(true));
        // FROM ...
        NamedSelector selector = (NamedSelector)query.getSource();
        assertThat(selector.getName(), is(selectorName("mgnl:content")));
        assertThat(selector.getAliasOrName(), is(selectorName("mgnl:content")));
        assertThat(selector.getAlias(), is(nullValue()));
        // WHERE ...
        Comparison comparison = isComparison(query.getConstraint());
        assertThat(comparison.getOperand1(), is((DynamicOperand)nodePath(selectorName("mgnl:content"))));
        assertThat(comparison.getOperand2(), is((StaticOperand)literal("/modules/%/templates")));
    }

    protected Comparison isComparison( Constraint constraint ) {
        assertThat(constraint, is(instanceOf(Comparison.class)));
        return (Comparison)constraint;
    }

    protected NodePath nodePath( SelectorName name ) {
        return new NodePath(name);
    }

    protected Literal literal( Object value ) {
        return new Literal(value);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // parseName
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParseNonPrefixedNameFromUnquotedString() {
        assertThat(parser.parseName(tokens("name"), typeSystem), is("name"));
    }

    @Test
    public void shouldParsePrefixedNameFromUnquotedString() {
        assertThat(parser.parseName(tokens("jcr:name"), typeSystem), is("jcr:name"));
    }

    @Test
    public void shouldParseNameFromSingleQuotedString() {
        assertThat(parser.parseName(tokens("'jcr:name'"), typeSystem), is("jcr:name"));
    }

    @Test
    public void shouldParseNameFromUnquotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("name"), typeSystem), is("name"));
    }

    @Test
    public void shouldParseNameFromSingleQuotedStringWithoutPrefix() {
        assertThat(parser.parseName(tokens("'name'"), typeSystem), is("name"));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseNameIfNoMoreTokens() {
        parser.parseName(tokens("  "), typeSystem);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // removeBracketsAndQuotes
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldRemoveBracketsAndQuotes() {
        assertThat(parser.removeBracketsAndQuotes("string"), is("string"));
        assertThat(parser.removeBracketsAndQuotes("'string'"), is("string"));
        assertThat(parser.removeBracketsAndQuotes("word one and two"), is("word one and two"));
        assertThat(parser.removeBracketsAndQuotes("'word one and two'"), is("word one and two"));
    }

    @Test
    public void shouldFailToRemoveDoubleQuotesAroundOneWord() {
        assertThat(parser.removeBracketsAndQuotes("\"string\""), is("\"string\""));
        assertThat(parser.removeBracketsAndQuotes("\"string\""), is("\"string\""));
        assertThat(parser.removeBracketsAndQuotes("\"word one and two\""), is("\"word one and two\""));
        assertThat(parser.removeBracketsAndQuotes("[word one and two]"), is("[word one and two]"));
    }

    /*
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNameArchived%' AND jcr:path LIKE '/drools:repository/drools:package_area/%'
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestPost/assets[%]/%' and  ( drools:format='drl' OR drools:format='xls' )  AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:archive = 'true' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testRestDelete/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testPackageSnapshot/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:packagesnapshot_area/testPackageSnapshot/PROD 2.0/assets[%]/%' and drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/searchByFormat/assets[%]/%' and  ( drools:format='xyz' OR drools:format='ABC' )  AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='testSearchSharedAssetByFormat' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' and drools:archive = 'true' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/org.drools.archivedtest/assets[%]/%' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not drools:format='drl' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( drools:format='drl' OR drools:format='wang' )  AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/testExcludeAssetTypes/assets[%]/%' and not ( drools:format='drl' OR drools:format='xls' )  AND drools:archive = 'false' ORDER BY drools:title

    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex1' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex2' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE drools:title LIKE 'findRulesByNamex%' AND jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:subject LIKE 'testQueryXXX42' AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND drools:subject LIKE 'testQueryXXX42' AND drools:source LIKE 'database'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00' AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created > TIMESTAMP '3074-07-10T00:00:00.000-05:00'
    SELECT drools:title, drools:description, drools:archive FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/%' AND (drools:subject LIKE 'testQueryXXX42' OR drools:subject LIKE 'wankle') AND (drools:source LIKE 'database' OR drools:source LIKE 'wankle') AND drools:archive = 'false' AND jcr:created < TIMESTAMP '1974-07-10T00:00:00.000-05:00'
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title
    SELECT * FROM drools:assetNodeType WHERE jcr:path LIKE '/drools:repository/drools:package_area/globalArea/assets[%]/%' and drools:format='xyz' AND drools:archive = 'false' ORDER BY drools:title
    select * from mgnl:content where jcr:path like '/modules/%/templates'
    */

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

}
