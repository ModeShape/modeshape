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
package org.modeshape.graph.search;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.xml.sax.SAXException;

public class SearchableRepositorySourceTest {

    private ExecutionContext context;
    private SearchableRepositorySource searchable;
    private RepositorySource wrapped;
    private SearchEngineProcessor searchProcessor;
    private LinkedList<Request> searchRequests;
    private ExecutorService executor;
    private TypeSystem typeSystem;
    private Schemata schemata;
    private SqlQueryParser sql;
    @Mock
    private SearchEngine searchEngine;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        this.context = new ExecutionContext();
        executor = Executors.newSingleThreadExecutor();
        typeSystem = context.getValueFactories().getTypeSystem();
        schemata = ImmutableSchemata.createBuilder(typeSystem).addTable("t1", "c1", "c2", "c3").build();
        sql = new SqlQueryParser();

        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("source1");
        this.wrapped = source;

        // Create the request processor that will be called for the search engine
        searchRequests = new LinkedList<Request>();
        searchProcessor = new MockSearchEngineProcessor("source1", context, searchRequests);

        // Stub the search engine methods ...
        when(searchEngine.createProcessor(context, null, true)).thenReturn(searchProcessor);
        when(searchEngine.createProcessor(context, null, false)).thenReturn(searchProcessor);
    }

    @After
    public void afterEach() {
        try {
            this.executor.shutdown();
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.interrupted();
            fail(e.getLocalizedMessage());
        }
    }

    protected SearchableRepositorySource newSynchronousSearchable() {
        return new SearchableRepositorySource(wrapped, searchEngine, null, false, false);
    }

    protected SearchableRepositorySource newAsynchronousSearchable() {
        return new SearchableRepositorySource(wrapped, searchEngine, executor, true, true);
    }

    @Test
    public void shouldReturnNameOfWrappedSource() {
        wrapped = mock(RepositorySource.class);
        when(wrapped.getName()).thenReturn("name");
        searchable = newSynchronousSearchable();
        assertThat(searchable.getName(), is(wrapped.getName()));
        verify(wrapped, times(2)).getName();
    }

    @Test
    public void shouldReturnRetryLimitOfWrappedSource() {
        wrapped = mock(RepositorySource.class);
        when(wrapped.getRetryLimit()).thenReturn(3);
        searchable = newSynchronousSearchable();
        assertThat(searchable.getRetryLimit(), is(wrapped.getRetryLimit()));
        verify(wrapped, times(2)).getRetryLimit();
    }

    @Test
    public void shouldCloseByClosingWrappedSource() {
        wrapped = mock(RepositorySource.class);
        searchable = newSynchronousSearchable();
        searchable.close();
        verify(wrapped, times(1)).close();
    }

    @Test
    public void shouldReturnCapabilitiesThatMatchSourceCapabilitiesExceptWithSearchableAndQueryable() {
        searchable = newSynchronousSearchable();
        RepositorySourceCapabilities sourceCapabilities = wrapped.getCapabilities();
        RepositorySourceCapabilities searchableCapabilities = searchable.getCapabilities();
        assertThat(searchableCapabilities.supportsCreatingWorkspaces(), is(sourceCapabilities.supportsCreatingWorkspaces()));
        assertThat(searchableCapabilities.supportsEvents(), is(sourceCapabilities.supportsEvents()));
        assertThat(searchableCapabilities.supportsLocks(), is(sourceCapabilities.supportsLocks()));
        assertThat(searchableCapabilities.supportsReferences(), is(sourceCapabilities.supportsReferences()));
        assertThat(searchableCapabilities.supportsSameNameSiblings(), is(sourceCapabilities.supportsSameNameSiblings()));
        assertThat(searchableCapabilities.supportsUpdates(), is(sourceCapabilities.supportsUpdates()));
        assertThat(searchableCapabilities.supportsQueries(), is(true));
        assertThat(searchableCapabilities.supportsSearches(), is(true));
    }

    @Test
    public void shouldReturnSynchronousConnectionIfSpecifiedInConstructor() {
        searchable = new SearchableRepositorySource(wrapped, searchEngine, executor, false, false);
        RepositoryConnection connection = searchable.getConnection();
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(SearchableRepositorySource.SynchronousConnection.class)));
    }

    @Test
    public void shouldReturnSynchronousConnectionIfExecutorNotProvidedInConstructor() {
        searchable = new SearchableRepositorySource(wrapped, searchEngine, null, false, false);
        RepositoryConnection connection = searchable.getConnection();
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(SearchableRepositorySource.SynchronousConnection.class)));

        searchable = new SearchableRepositorySource(wrapped, searchEngine, null, true, true);
        connection = searchable.getConnection();
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(SearchableRepositorySource.SynchronousConnection.class)));
    }

    @Test
    public void shouldReturnParallelConnectionIfExecutorProvidedInConstructor() {
        searchable = new SearchableRepositorySource(wrapped, searchEngine, executor, true, false);
        RepositoryConnection connection = searchable.getConnection();
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(SearchableRepositorySource.ParallelConnection.class)));

        searchable = new SearchableRepositorySource(wrapped, searchEngine, executor, true, true);
        connection = searchable.getConnection();
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(SearchableRepositorySource.ParallelConnection.class)));
    }

    @SuppressWarnings( "synthetic-access" )
    @Test
    public void shouldInitializeByInitializingWrappedSourceWithCustomObserverAndConnectionFactory() {
        final RepositoryConnectionFactory mockConnectionFactory = mock(RepositoryConnectionFactory.class);
        RepositoryContext repoContext = new RepositoryContext() {
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Subgraph getConfiguration( int depth ) {
                return null; // there is no configuration
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return mockConnectionFactory;
            }
        };
        final AtomicInteger called = new AtomicInteger(0);
        wrapped = new InMemoryRepositorySource() {
            private static final long serialVersionUID = 1L;

            @Override
            public void initialize( RepositoryContext context ) throws RepositorySourceException {
                called.incrementAndGet();
                assertThat(context, is(notNullValue()));
                assertThat(context.getExecutionContext(), is(sameInstance(SearchableRepositorySourceTest.this.context)));
                assertThat(context.getObserver(), is(notNullValue()));
                assertThat(context.getRepositoryConnectionFactory(), is(notNullValue()));
                assertThat(context.getRepositoryConnectionFactory(), is(not(sameInstance(mockConnectionFactory))));
            }
        };
        searchable = newSynchronousSearchable();
        searchable.initialize(repoContext);
        assertThat(called.get(), is(1));
    }

    protected void assertThatSourceIsNotSearchable( RepositorySource source ) {
        try {
            Graph graph = Graph.create(wrapped, context);
            graph.search("term", 100, 0);
            fail("Wrapped repository source should not support searching");
        } catch (InvalidRequestException e) {
            // expected ...
        }
        assertThat(source.getCapabilities().supportsSearches(), is(false));
    }

    protected void loadContentInto( RepositorySource source,
                                    String resourceOnClasspath ) throws SAXException, IOException, URISyntaxException {
        Graph graph = Graph.create(source, context);
        graph.importXmlFrom(getClass().getClassLoader().getResource(resourceOnClasspath).toURI()).into("/");

        // Verify the content is there ...
        Node boeing777 = graph.getNodeAt("/Aircraft/Commercial/Boeing 777");
        assertThat(boeing777, is(notNullValue()));
    }

    @Test
    public void shouldProcessSearchRequest() throws Exception {
        assertThatSourceIsNotSearchable(wrapped);
        loadContentInto(wrapped, "aircraft.xml");
    }

    @Test
    public void shouldSendSearchRequestToSearchEngine() throws Exception {
        assertThatSourceIsNotSearchable(wrapped);
        loadContentInto(wrapped, "aircraft.xml");
        searchable = newSynchronousSearchable();
        assertThat(searchable.getCapabilities().supportsSearches(), is(true));
        QueryResults results = Graph.create(searchable, context).search("Boeing", 100, 1);
        assertThat(results, is(notNullValue()));
    }

    @Test
    public void shouldSendQueryRequestToSearchEngine() throws Exception {
        assertThatSourceIsNotSearchable(wrapped);
        loadContentInto(wrapped, "aircraft.xml");
        searchable = newSynchronousSearchable();
        assertThat(searchable.getCapabilities().supportsQueries(), is(true));
        QueryResults results = Graph.create(searchable, context)
                                    .query(sql.parseQuery("SELECT * FROM t1", typeSystem), schemata)
                                    .execute();
        assertThat(results, is(notNullValue()));
    }

    @Test
    public void shouldSendAllRequestsToSynchronousWrappedSourceWhenRequestsAreNotSearchOrQueryRequests() throws Exception {
        assertThatSourceIsNotSearchable(wrapped);
        loadContentInto(wrapped, "aircraft.xml");
        searchable = newSynchronousSearchable();
        Graph graph = Graph.create(searchable, context);
        Node boeing777 = graph.getNodeAt("/Aircraft/Commercial/Boeing 777");
        assertThat(boeing777, is(notNullValue()));

        // Now do a batch ...
        graph.batch().read("/Aircraft/Commercial/Boeing 777").and().read("/Aircraft/Commercial/Boeing 787").execute();
    }

    @Test
    public void shouldSendAllRequestsToAsynchronousWrappedSourceWhenRequestsAreNotSearchOrQueryRequests() throws Exception {
        assertThatSourceIsNotSearchable(wrapped);
        loadContentInto(wrapped, "aircraft.xml");
        searchable = newAsynchronousSearchable();
        Graph graph = Graph.create(searchable, context);
        Node boeing777 = graph.getNodeAt("/Aircraft/Commercial/Boeing 777");
        assertThat(boeing777, is(notNullValue()));

        // Now do a batch ...
        graph.batch().read("/Aircraft/Commercial/Boeing 777").and().read("/Aircraft/Commercial/Boeing 787").execute();
    }
}
