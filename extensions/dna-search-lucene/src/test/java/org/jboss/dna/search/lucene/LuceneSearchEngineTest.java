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
package org.jboss.dna.search.lucene;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
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
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.Limit;
import org.jboss.dna.graph.query.model.Query;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Selector;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Source;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.parse.SqlQueryParser;
import org.jboss.dna.graph.query.process.QueryResultColumns;
import org.jboss.dna.graph.query.validate.ImmutableSchemata;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.FullTextSearchRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.jboss.dna.graph.search.SearchEngine;
import org.jboss.dna.graph.search.SearchEngineIndexer;
import org.jboss.dna.graph.search.SearchEngineProcessor;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class LuceneSearchEngineTest {

    private SearchEngine engine;
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
    private Map<String, Object> variables;

    /** Controls whether the results from each test should be printed to System.out */
    private boolean print = false;

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
        IndexRules.Builder rulesBuilder = IndexRules.createBuilder(LuceneSearchEngine.DEFAULT_RULES);
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
        Analyzer analyzer = null;
        engine = new LuceneSearchEngine(sourceName, connectionFactory, true, luceneConfig, rules, analyzer);
        loadContent();

        // Load the workspaces into the engine ...
        SearchEngineProcessor processor = engine.createProcessor(context, null, false);
        try {
            for (String workspaceName : content.getWorkspaces()) {
                processor.process(new VerifyWorkspaceRequest(workspaceName));
            }
        } finally {
            processor.close();
        }

        // Create the schemata for the workspaces ...
        schemata = ImmutableSchemata.createBuilder(typeSystem)
                                    .addTable("__ALLNODES__", "maker", "model", "year", "msrp", "mpgHighway", "mpgCity")
                                    .makeSearchable("__ALLNODES__", "maker")
                                    .build();

        // And create the SQL parser ...
        sql = new SqlQueryParser();

        variables = new HashMap<String, Object>();
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

    protected QueryResults search( String workspaceName,
                                   String searchExpression,
                                   int maxResults,
                                   int offset ) {
        RequestProcessor processor = engine.createProcessor(context, null, true);
        try {
            FullTextSearchRequest request = new FullTextSearchRequest(searchExpression, workspaceName, maxResults, offset);
            processor.process(request);
            if (request.hasError()) {
                fail(request.getError().getMessage());
                return null;
            }
            assertThat(request.getResultColumns().getColumnCount(), is(0));
            assertThat(request.getResultColumns().getLocationCount(), is(1));
            assertThat(request.getResultColumns().hasFullTextSearchScores(), is(true));
            // Convert the results to a List<Location>
            List<Object[]> tuples = request.getTuples();
            List<Location> results = new ArrayList<Location>(tuples.size());
            for (Object[] tuple : tuples) {
                results.add((Location)tuple[0]);
                Float score = (Float)tuple[1];
                assertThat(score, is(notNullValue()));
            }
            return new org.jboss.dna.graph.query.process.QueryResults(request.getResultColumns(), request.getStatistics(),
                                                                      request.getTuples());
        } finally {
            processor.close();
        }
    }

    protected List<Constraint> getAndedConstraint( Constraint constraint,
                                                   List<Constraint> andedConstraints ) {
        if (constraint != null) {
            if (constraint instanceof And) {
                And and = (And)constraint;
                getAndedConstraint(and.getLeft(), andedConstraints);
                getAndedConstraint(and.getRight(), andedConstraints);
            } else {
                andedConstraints.add(constraint);
            }
        }
        return andedConstraints;
    }

    protected QueryResults query( String workspaceName,
                                  String sql ) {
        QueryCommand command = this.sql.parseQuery(sql, typeSystem);
        assertThat(command, is(instanceOf(Query.class)));
        Query query = (Query)command;
        Source source = query.getSource();
        assertThat(source, is(instanceOf(Selector.class)));
        SelectorName tableName = ((Selector)source).getName();
        Constraint constraint = query.getConstraint();
        Columns resultColumns = new QueryResultColumns(query.getColumns(), QueryResultColumns.includeFullTextScores(constraint));
        List<Constraint> andedConstraints = getAndedConstraint(constraint, new ArrayList<Constraint>());
        Limit limit = query.getLimits();
        RequestProcessor processor = engine.createProcessor(context, null, true);
        try {
            AccessQueryRequest request = new AccessQueryRequest(workspaceName, tableName, resultColumns, andedConstraints, limit,
                                                                schemata, variables);
            processor.process(request);
            if (request.hasError()) {
                fail(request.getError().getMessage());
            }
            return new org.jboss.dna.graph.query.process.QueryResults(request.resultColumns(), request.getStatistics(),
                                                                      request.getTuples());
        } finally {
            processor.close();
        }
    }

    @Test
    public void shouldIndexAllContentInRepositorySource() throws Exception {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.indexAllWorkspaces().close();
    }

    @Test
    public void shouldIndexAllContentInWorkspace() throws Exception {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1);
        indexer.index(workspaceName2);
        indexer.close();
    }

    @Test
    public void shouldIndexAllContentInWorkspaceBelowPath() throws Exception {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/Cars/Hybrid"), 3);
        indexer.index(workspaceName2, path("/Aircraft/Commercial"), 5);
        indexer.close();
    }

    @Test
    public void shouldReIndexAllContentInWorkspaceBelowPath() throws Exception {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        for (int i = 0; i != 0; i++) {
            indexer.index(workspaceName1, path("/Cars/Hybrid"), 3);
            indexer.index(workspaceName2, path("/Aircraft/Commercial"), 5);
        }
        indexer.close();
    }

    @Test
    public void shouldHaveLoadedTestContentIntoRepositorySource() {
        content.useWorkspace(workspaceName1);
        assertThat(content.getNodeAt("/Cars/Hybrid/Toyota Prius").getProperty("msrp").getFirstValue(), is((Object)"$21,500"));
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfOne() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/"), 1);
        indexer.close();
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTwo() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/"), 2);
        indexer.close();
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfThree() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/"), 3);
        indexer.close();
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfFour() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/"), 4);
        indexer.close();
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTen() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/"), 10);
        indexer.close();
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtNonRootNode() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/Cars"), 10);
        indexer.close();
    }

    @Test
    public void shouldReIndexRepositoryContentStartingAtNonRootNode() {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        indexer.index(workspaceName1, path("/Cars"), 10);
        indexer.index(workspaceName1, path("/Cars"), 10);
        indexer.index(workspaceName1, path("/Cars"), 10);
        indexer.close();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Full-text search
    // ----------------------------------------------------------------------------------------------------------------

    protected void indexWorkspace( String workspaceName ) {
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, engine, connectionFactory);
        try {
            indexer.index(workspaceName, path("/"));
        } finally {
            indexer.close();
        }
    }

    @Test
    public void shouldFindNodesByFullTextSearch() {
        indexWorkspace(workspaceName1);
        QueryResults results = search(workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 2);
        Location first = (Location)(results.getTuples().get(0)[0]);
        Location second = (Location)(results.getTuples().get(1)[0]);
        assertThat(first.getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(second.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }

    @Test
    public void shouldFindNodesByFullTextSearchWithOffset() {
        indexWorkspace(workspaceName1);
        QueryResults results = search(workspaceName1, "toyota prius", 1, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 1);
        Location first = (Location)(results.getTuples().get(0)[0]);
        assertThat(first.getPath(), is(path("/Cars/Hybrid/Toyota Prius")));

        results = search(workspaceName1, "+Toyota", 1, 1);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 1);
        first = (Location)(results.getTuples().get(0)[0]);
        assertThat(first.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Query
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldFindNodesBySimpleQuery() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 18);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithEqualityComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE maker = 'Toyota'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithGreaterThanComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker, mpgHighway, mpgCity FROM __ALLNODES__ WHERE mpgHighway > 20";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 6);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLowercaseEqualityComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE LOWER(maker) = 'toyota'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithUppercaseEqualityComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE UPPER(maker) = 'TOYOTA'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLikeComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE maker LIKE 'Toyo%'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLikeComparisonCriteriaWithLeadingWildcard() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE maker LIKE '%yota'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLowercaseLikeComparisonCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE LOWER(maker) LIKE 'toyo%'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 2);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithFullTextSearchCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE CONTAINS(maker,'martin')";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 1);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithDepthCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE DEPTH() > 2";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 12);
    }

    @Test
    public void shouldFindNodesBySimpleQueryWithLocalNameCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE LOCALNAME() LIKE 'Toyota%' OR LOCALNAME() LIKE 'Land %'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 4);

    }

    @Test
    public void shouldFindNodesBySimpleQueryWithNameCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE NAME() LIKE 'Toyota%[1]' OR NAME() LIKE 'Land %'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 4);

    }

    @Test
    public void shouldFindNodesBySimpleQueryWithNameCriteriaThatMatchesNoNodes() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE NAME() LIKE 'Toyota%[2]'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 0);

    }

    @Test
    public void shouldFindNodesBySimpleQueryWithPathCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[%]/Hy%/Toyota%' OR PATH() LIKE '/Cars[1]/Utility[1]/%'";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 6);

    }

    @Test
    public void shouldFindNodesBySimpleQueryWithDescendantCriteria() {
        indexWorkspace(workspaceName1);
        String query = "SELECT model, maker FROM __ALLNODES__ WHERE ISDESCENDANTNODE('/Cars/Hybrid')";
        QueryResults results = query(workspaceName1, query);
        assertRowCount(results, 3);

    }

    protected void assertRowCount( QueryResults results,
                                   int rowCount ) {
        assertThat(results.getProblems().isEmpty(), is(true));
        assertThat(results.getTuples().size(), is(rowCount));
        if (print) {
            System.out.println(results);
        }
    }

}
