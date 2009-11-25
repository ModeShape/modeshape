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
package org.jboss.dna.search;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Field;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.parse.SqlQueryParser;
import org.jboss.dna.graph.query.validate.ImmutableSchemata;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.search.SearchEngine;
import org.jboss.dna.graph.search.SearchProvider;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SearchEngineTest {

    private SearchEngine engine;
    private SearchProvider provider;
    private ExecutionContext context;
    private TypeSystem typeSystem;
    private String sourceName;
    private String workspaceName1;
    private String workspaceName2;
    private InMemoryRepositorySource source;
    private RepositoryConnectionFactory connectionFactory;
    private Graph content;
    private Schemata schemata;
    private SqlQueryParser sql;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        typeSystem = context.getValueFactories().getTypeSystem();
        sourceName = "sourceA";
        workspaceName1 = "workspace1";
        workspaceName2 = "workspace2";

        // Set up the source and graph instance ...
        source = new InMemoryRepositorySource();
        source.setName(sourceName);
        content = Graph.create(source, context);

        // Create the workspaces ...
        content.createWorkspace().named(workspaceName1);
        content.createWorkspace().named(workspaceName2);

        // Set up the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return source.getConnection();
            }
        };

        // Set up the provider and the search engine ...
        IndexRules.Builder rulesBuilder = IndexRules.createBuilder(DualIndexSearchProvider.DEFAULT_RULES);
        rulesBuilder.defaultTo(Field.Store.YES, Field.Index.NOT_ANALYZED);
        rulesBuilder.stringField(name("model"), Field.Store.YES, Field.Index.ANALYZED);
        rulesBuilder.integerField(name("year"), Field.Store.YES, Field.Index.NOT_ANALYZED);
        rulesBuilder.floatField(name("userRating"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0.0f, 10.0f);
        rulesBuilder.integerField(name("mpgCity"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0, 50);
        rulesBuilder.integerField(name("mpgHighway"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0, 50);
        // rulesBuilder.analyzeAndStoreAndFullText(name("maker"));
        IndexRules rules = rulesBuilder.build();
        LuceneConfiguration luceneConfig = LuceneConfigurations.inMemory();
        // LuceneConfiguration luceneConfig = LuceneConfigurations.using(new File("target/testIndexes"));
        provider = new DualIndexSearchProvider(luceneConfig, rules);
        engine = new SearchEngine(context, sourceName, connectionFactory, provider);
        loadContent();

        // Create the schemata for the workspaces ...
        schemata = ImmutableSchemata.createBuilder(typeSystem)
                                    .addTable("__ALLNODES__", "maker", "model", "year", "msrp", "mpgHighway", "mpgCity")
                                    .makeSearchable("__ALLNODES__", "maker")
                                    .build();

        // And create the SQL parser ...
        sql = new SqlQueryParser();
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected void loadContent() throws IOException, SAXException {
        // Load the content ...
        content.useWorkspace(workspaceName1);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");
        content.useWorkspace(workspaceName2);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("aircraft.xml")).into("/");
    }

    @Test
    public void shouldIndexAllContentInRepositorySource() throws Exception {
        engine.index(3);
    }

    @Test
    public void shouldIndexAllContentInWorkspace() throws Exception {
        engine.index(workspaceName1, 3);
        engine.index(workspaceName2, 5);
    }

    @Test
    public void shouldIndexAllContentInWorkspaceBelowPath() throws Exception {
        engine.index(workspaceName1, path("/Cars/Hybrid"), 3);
        engine.index(workspaceName2, path("/Aircraft/Commercial"), 5);
    }

    @Test
    public void shouldReIndexAllContentInWorkspaceBelowPath() throws Exception {
        for (int i = 0; i != 0; i++) {
            engine.index(workspaceName1, path("/Cars/Hybrid"), 3);
            engine.index(workspaceName2, path("/Aircraft/Commercial"), 5);
        }
    }

    @Test
    public void shouldHaveLoadedTestContentIntoRepositorySource() {
        content.useWorkspace(workspaceName1);
        assertThat(content.getNodeAt("/Cars/Hybrid/Toyota Prius").getProperty("msrp").getFirstValue(), is((Object)"$21,500"));
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfOne() {
        engine.index(workspaceName1, path("/"), 1);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTwo() {
        engine.index(workspaceName1, path("/"), 2);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfThree() {
        engine.index(workspaceName1, path("/"), 3);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfFour() {
        engine.index(workspaceName1, path("/"), 4);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTen() {
        engine.index(workspaceName1, path("/"), 10);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtNonRootNode() {
        engine.index(workspaceName1, path("/Cars"), 10);
    }

    @Test
    public void shouldReIndexRepositoryContentStartingAtNonRootNode() {
        engine.index(workspaceName1, path("/Cars"), 10);
        engine.index(workspaceName1, path("/Cars"), 10);
        engine.index(workspaceName1, path("/Cars"), 10);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Full-text search
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldFindNodesByFullTextSearch() {
        engine.index(workspaceName1, path("/"), 100);
        List<Location> results = engine.fullTextSearch(context, workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(2));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(results.get(1).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }

    @Test
    public void shouldFindNodesByFullTextSearchWithOffset() {
        engine.index(workspaceName1, path("/"), 100);
        List<Location> results = engine.fullTextSearch(context, workspaceName1, "toyota prius", 1, 0);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));

        results = engine.fullTextSearch(context, workspaceName1, "+Toyota", 1, 1);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }

    @Test
    public void shouldFindNodesBySimpleXpathQuery() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(18));
        System.out.println(results);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Query
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldFindNodesBySimpleQuery() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(18));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithEqualityComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE maker = 'Toyota'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithGreaterThanComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker, mpgHighway, mpgCity FROM __ALLNODES__ WHERE mpgHighway > 20",
                                            typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(6));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLowercaseEqualityComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE LOWER(maker) = 'toyota'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithUppercaseEqualityComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE UPPER(maker) = 'TOYOTA'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLikeComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE maker LIKE 'Toyo%'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLikeComparisonCriteriaWithLeadingWildcard() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE maker LIKE '%yota'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLowercaseLikeComparisonCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE LOWER(maker) LIKE 'toyo%'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(2));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithFullTextSearchCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE CONTAINS(maker,'martin')", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(1));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithDepthCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE DEPTH() > 2", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(12));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLocalNameCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE LOCALNAME() LIKE 'Toyota%' OR LOCALNAME() LIKE 'Land %'",
                                            typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(4));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithNameCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE NAME() LIKE 'Toyota%[1]' OR NAME() LIKE 'Land %'",
                                            typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(4));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithNameCriteriaThatMatchesNoNodes() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE NAME() LIKE 'Toyota%[2]'", typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(0));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithPathCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[%]/Hy%/Toyota%' OR PATH() LIKE '/Cars[1]/Utility[1]/%'",
                                            typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(6));
        System.out.println(results);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithDescendantCriteria() {
        engine.index(workspaceName1, path("/"), 100);
        QueryCommand query = sql.parseQuery("SELECT model, maker FROM __ALLNODES__ WHERE ISDESCENDANTNODE('/Cars/Hybrid')",
                                            typeSystem);
        QueryResults results = engine.query(context, workspaceName1, query, schemata);
        assertNoErrors(results);
        assertThat(results, is(notNullValue()));
        assertThat(results.getRowCount(), is(3));
        System.out.println(results);
    }

    protected void assertNoErrors( QueryResults results ) {
        if (results.getProblems().hasErrors()) {
            fail("Found errors: " + results.getProblems());
        }
        assertThat(results.getProblems().hasErrors(), is(false));
    }

}
