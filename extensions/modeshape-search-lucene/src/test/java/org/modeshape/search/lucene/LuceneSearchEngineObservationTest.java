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
package org.modeshape.search.lucene;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.math.Duration;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.Selector;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.graph.query.process.QueryResultColumns;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.processor.RequestProcessor;
import org.modeshape.graph.search.SearchEngineIndexer;
import org.xml.sax.SAXException;

/**
 * These tests verify that the {@link LuceneSearchEngine} is able to properly update the content based upon
 * {@link LuceneSearchEngine#index(org.modeshape.graph.ExecutionContext, Iterable) observations}.
 */
public class LuceneSearchEngineObservationTest {

    private String sourceName;
    private String workspaceName1;
    private String workspaceName2;
    private ExecutionContext context;
    private TypeSystem typeSystem;
    private InMemoryRepositorySource source;
    private InMemoryRepositorySource unsearchedSource;
    private RepositoryConnectionFactory connectionFactory;
    private Graph content;
    private Graph unsearchedContent;
    private LuceneSearchEngine searchEngine;
    private Schemata schemata;
    private SqlQueryParser sql;
    private Map<String, Object> variables;
    private Stopwatch sw;
    private int depthToRead;

    /** Controls whether the results from each test should be printed to System.out */
    private boolean print = false;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        typeSystem = context.getValueFactories().getTypeSystem();
        workspaceName1 = "cars";
        workspaceName2 = "aircraft";
        depthToRead = 10;
        sw = new Stopwatch();

        sourceName = "source";
        source = new InMemoryRepositorySource();
        source.setName(sourceName);
        content = Graph.create(source, context);
        unsearchedSource = new InMemoryRepositorySource();
        unsearchedSource.setName(sourceName);
        unsearchedContent = Graph.create(unsearchedSource, context);

        // Create the workspaces ...
        content.createWorkspace().named(workspaceName1);
        content.createWorkspace().named(workspaceName2);
        unsearchedContent.createWorkspace().named(workspaceName1);
        unsearchedContent.createWorkspace().named(workspaceName2);

        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String name ) throws RepositorySourceException {
                assertThat(sourceName, is(name));
                return source.getConnection();
            }
        };

        // Set up the provider and the search engine ...
        IndexRules.Builder rulesBuilder = IndexRules.createBuilder(LuceneSearchEngine.DEFAULT_RULES);
        rulesBuilder.defaultTo(Field.Store.YES, Field.Index.NOT_ANALYZED, false, true);
        rulesBuilder.stringField(name("model"), Field.Store.YES, Field.Index.ANALYZED, false, true);
        rulesBuilder.integerField(name("year"), Field.Store.YES, Field.Index.NOT_ANALYZED, 1990, 2020);
        rulesBuilder.floatField(name("userRating"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0.0f, 10.0f);
        rulesBuilder.integerField(name("mpgCity"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0, 50);
        rulesBuilder.integerField(name("mpgHighway"), Field.Store.YES, Field.Index.NOT_ANALYZED, 0, 50);
        // rulesBuilder.analyzeAndStoreAndFullText(name("maker"));
        IndexRules rules = rulesBuilder.build();
        LuceneConfiguration luceneConfig = LuceneConfigurations.inMemory();
        // LuceneConfiguration luceneConfig = LuceneConfigurations.using(new File("target/testIndexes"));
        Analyzer analyzer = null;
        searchEngine = new LuceneSearchEngine(sourceName, connectionFactory, false, depthToRead, luceneConfig, rules, analyzer);

        // Initialize the source so that the search engine observes the events ...
        @SuppressWarnings( "synthetic-access" )
        RepositoryContext repositoryContext = new RepositoryContext() {
            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            public ExecutionContext getExecutionContext() {
                return context;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return connectionFactory;
            }

            public Observer getObserver() {
                return new Observer() {
                    public void notify( Changes changes ) {
                        // -----------------------------------------------------------
                        // NOTE THAT THE SEARCH ENGINE IS UPDATED IN-THREAD !!!!!!!!!!
                        // -----------------------------------------------------------
                        // This means the indexing should be done before the graph operations return
                        searchEngine.index(context, changes.getChangeRequests());
                    }
                };
            }
        };
        source.initialize(repositoryContext);

        // Create the schemata for the workspaces ...
        schemata = ImmutableSchemata.createBuilder(typeSystem)
                                    .addTable("__ALLNODES__", "maker", "model", "year", "msrp", "mpgHighway", "mpgCity")
                                    .makeSearchable("__ALLNODES__", "maker")
                                    .build();

        // And create the SQL parser ...
        sql = new SqlQueryParser();

        variables = new HashMap<String, Object>();
    }

    @After
    public void afterEach() {
        searchEngine = null;
        content = null;
        context = null;
        source = null;
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected void loadContent( Graph graph ) {
        try {
            // Load the content ...
            graph.useWorkspace(workspaceName1);
            graph.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");
            graph.useWorkspace(workspaceName2);
            graph.importXmlFrom(getClass().getClassLoader().getResourceAsStream("aircraft.xml")).into("/");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    protected void assertRowCount( QueryResults results,
                                   int rowCount ) {
        assertThat(results.getProblems().isEmpty(), is(true));
        if (print) {
            System.out.println(results);
        }
        assertThat(results.getTuples().size(), is(rowCount));
    }

    protected QueryResults search( String workspaceName,
                                   String searchExpression,
                                   int maxResults,
                                   int offset ) {
        RequestProcessor processor = searchEngine.createProcessor(context, null, true);
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
            return new org.modeshape.graph.query.process.QueryResults(request.getResultColumns(), request.getStatistics(),
                                                                      request.getTuples());
        } finally {
            processor.close();
        }
    }

    protected QueryResults query( String workspaceName,
                                  String sql ) {
        QueryCommand command = this.sql.parseQuery(sql, typeSystem);
        assertThat(command, is(instanceOf(Query.class)));
        Query query = (Query)command;
        Source source = query.source();
        assertThat(source, is(instanceOf(Selector.class)));
        SelectorName tableName = ((Selector)source).name();
        Constraint constraint = query.constraint();
        List<String> types = new ArrayList<String>();
        for (int i = 0; i != query.columns().size(); ++i) {
            types.add(PropertyType.STRING.getName());
        }
        Columns resultColumns = new QueryResultColumns(query.columns(), types,
                                                       QueryResultColumns.includeFullTextScores(constraint));
        List<Constraint> andedConstraints = getAndedConstraint(constraint, new ArrayList<Constraint>());
        Limit limit = query.limits();
        RequestProcessor processor = searchEngine.createProcessor(context, null, true);
        try {
            AccessQueryRequest request = new AccessQueryRequest(workspaceName, tableName, resultColumns, andedConstraints, limit,
                                                                schemata, variables);
            processor.process(request);
            if (request.hasError()) {
                request.getError().printStackTrace(System.out);
                fail(request.getError().getMessage());
            }
            return new org.modeshape.graph.query.process.QueryResults(request.resultColumns(), request.getStatistics(),
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
                getAndedConstraint(and.left(), andedConstraints);
                getAndedConstraint(and.right(), andedConstraints);
            } else {
                andedConstraints.add(constraint);
            }
        }
        return andedConstraints;
    }

    @Test
    public void shouldInitializeWithoutAddingContentToSource() {
    }

    @Test
    public void shouldEstimateTimeToIndexContent() {
        // Prime the reading of the files ...
        InMemoryRepositorySource prime = new InMemoryRepositorySource();
        prime.setName(sourceName);
        Graph primeGraph = Graph.create(prime, context);
        primeGraph.createWorkspace().named(workspaceName1);
        primeGraph.createWorkspace().named(workspaceName2);

        // Prime the search engine ...
        sw.reset();
        sw.start();
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory, depthToRead);
        indexer.indexAllWorkspaces();
        indexer.close();
        sw.stop();
        Duration zeroth = sw.getTotalDuration();
        System.out.println("Time to prime search engine:                 " + zeroth);

        // First load the content into the unsearched source ...
        sw.reset();
        sw.start();
        loadContent(unsearchedContent);
        sw.stop();
        Duration first = sw.getTotalDuration();

        // Now load the same content into the searchable source ...
        sw.reset();
        sw.start();
        loadContent(content);
        sw.stop();
        Duration second = sw.getTotalDuration();

        // And measure the time required to re-index ...
        sw.reset();
        sw.start();
        indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory, 10);
        indexer.indexAllWorkspaces();
        indexer.close();
        sw.stop();
        Duration third = sw.getTotalDuration();

        int percentOfLoading = (int)(((second.floatValue() / first.floatValue())) * 100.0f);
        System.out.println("Time to load content without indexing:       " + first);
        System.out.println("Time to load content and updating indexes:   " + second + "  (" + percentOfLoading
                           + "% of loading w/o indexing)");
        Duration loadingDiff = second.subtract(first);
        System.out.println("Time to update indexes during loading:       " + loadingDiff);
        int percentChange = (int)((((third.floatValue() - loadingDiff.floatValue()) / loadingDiff.floatValue())) * 100.0f);
        if (percentChange >= 0) {
            System.out.println("Time to re-index all content:                " + third + "  (" + percentChange
                               + "% more than indexing time during loading)");
        } else {
            System.out.println("Time to re-index all content:                " + third + "  (" + percentChange
                               + "% less than indexing time during loading)");
        }

        // Make sure we're finding the results ...
        // print = true;
        QueryResults results = search(workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 2);
        Location location1 = (Location)(results.getTuples().get(0)[0]);
        Location location2 = (Location)(results.getTuples().get(1)[0]);
        assertThat(location1.getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(location2.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));

    }

    @Test
    public void shouldUpdateIndexesWhenPropertiesAreSetOnRootInSource() {
        content.set("year").on("/").to("2009").and();
    }

    @Test
    public void shouldUpdateIndexesWhenMultipleNodesAreAdded() {
        content.batch()
               .create("/TheEnzo")
               .with("year", 2009)
               .and("model", "Enzo")
               .and()
               .create("/TheEsto")
               .with("year", 2009)
               .and("model", "Esto")
               .and()
               .execute();
    }

    @Test
    public void shouldUpdateIndexesWhenDeletingNodesInSource() {
        loadContent(content);

        // Make sure we're finding the results ...
        QueryResults results = search(workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 2);
        Location location1 = (Location)(results.getTuples().get(0)[0]);
        Location location2 = (Location)(results.getTuples().get(1)[0]);
        assertThat(location1.getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(location2.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));

        String query = "SELECT model, maker FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[%]/Hy%/Toyota%' OR PATH() LIKE '/Cars[1]/Utility[1]/%'";
        results = query(workspaceName1, query);
        assertRowCount(results, 6);

        content.useWorkspace(workspaceName1);
        content.delete("/Cars/Hybrid/Toyota Prius");

        // Make sure we don't find the 'Prius' anymore, but we still should find the 'Highlander' ...
        results = search(workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 1);
        location1 = (Location)(results.getTuples().get(0)[0]);
        assertThat(location1.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));

        query = "SELECT model, maker FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[%]/Hy%/Toyota%' OR PATH() LIKE '/Cars[1]/Utility[1]/%'";
        results = query(workspaceName1, query);
        assertRowCount(results, 5);
    }

    @Test
    public void shouldUpdateIndexesWhenUpdatingPropertiesInSource() {
        loadContent(content);

        // Make sure we're finding the results ...
        QueryResults results = search(workspaceName1, "Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertRowCount(results, 2);
        Location location1 = (Location)(results.getTuples().get(0)[0]);
        Location location2 = (Location)(results.getTuples().get(1)[0]);
        assertThat(location1.getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(location2.getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));

        String query = "SELECT model, maker, year FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[1]/Utility[1]/Ford F-150[1]' AND year = 2008";
        results = query(workspaceName1, query);
        assertRowCount(results, 1);

        content.useWorkspace(workspaceName1);
        content.set("year").on("/Cars/Utility/Ford F-150").to(2011).and();

        // Make sure we DO find the F-150 with the updated year ...
        query = "SELECT model, maker, year FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[1]/Utility[1]/Ford F-150[1]' AND year = 2011";
        results = query(workspaceName1, query);
        assertRowCount(results, 1);

        // Make sure we do NOT find the F-150 anymore with the old year ...
        query = "SELECT model, maker, year FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[1]/Utility[1]/Ford F-150[1]' AND year = 2008";
        results = query(workspaceName1, query);
        assertRowCount(results, 0);

        // We should find this since it still matches the criteria ...
        query = "SELECT model, maker, year FROM __ALLNODES__ WHERE PATH() LIKE '/Cars[1]/Utility[1]/Ford F-150[1]' AND year >= 2010";
        results = query(workspaceName1, query);
        assertRowCount(results, 1);

        // Try some queries that should NOT work ...
        query = "SELECT model, maker, year FROM __ALLNODES__ WHERE year <= 1899";
        results = query(workspaceName1, query);
        assertRowCount(results, 0);

    }
}
