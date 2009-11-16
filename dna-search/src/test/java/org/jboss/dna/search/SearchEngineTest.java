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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.Query;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.search.IndexLayout;
import org.jboss.dna.search.IndexSession;
import org.jboss.dna.search.SearchEngine;
import org.junit.Before;
import org.junit.Test;

public class SearchEngineTest {

    private SearchEngine engine;
    private ExecutionContext context;
    private String sourceName;
    private String workspaceName1;
    private String workspaceName2;
    private InMemoryRepositorySource source;
    private RepositoryConnectionFactory connectionFactory;
    private IndexLayout layout;
    private IndexSession sessionWs1;
    private IndexSession sessionWs2;
    private IndexSession sessionDefault;
    private Graph content;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
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

        // Set up the index layout ...
        layout = mock(IndexLayout.class);
        sessionWs1 = mockSession(layout, workspaceName1);
        sessionWs2 = mockSession(layout, workspaceName2);
        sessionDefault = mockSession(layout, "");

        // Now set up the search engine ...
        engine = new SearchEngine(context, sourceName, connectionFactory, layout);
    }

    protected IndexSession mockSession( IndexLayout mockLayout,
                                        String workspaceName ) {
        IndexSession session = mock(IndexSession.class);
        stub(layout.createSession(context, sourceName, workspaceName, false, false)).toReturn(session);
        stub(layout.createSession(context, sourceName, workspaceName, false, true)).toReturn(session);
        stub(layout.createSession(context, sourceName, workspaceName, true, false)).toReturn(session);
        stub(layout.createSession(context, sourceName, workspaceName, true, true)).toReturn(session);
        stub(session.getWorkspaceName()).toReturn(workspaceName);
        stub(session.getSourceName()).toReturn(sourceName);
        return session;
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected void loadContent() throws Exception {
        // Load some content ...
        content.useWorkspace(workspaceName1);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");
        content.useWorkspace(workspaceName2);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("aircraft.xml")).into("/");
    }

    @Test
    public void shouldReturnSearchWorkspaceForExistingWorkspaceInSource() {
        SearchEngine.Workspace workspace = engine.getWorkspace(workspaceName1);
        assertThat(workspace, is(notNullValue()));
        assertThat(workspace.modifiedNodesSinceLastOptimize.get(), is(0));
        assertThat(workspace.getWorkspaceName(), is(workspaceName1));
    }

    @Test( expected = InvalidWorkspaceException.class )
    public void shouldFailToReturnSearchWorkspaceForNonExistantWorkspaceInSource() {
        engine.getWorkspace(workspaceName1 + "foobar");
    }

    @Test
    public void shouldDoNothingDuringRemoveWorkspaceIfWorkspaceHasNotBeenLoaded() throws Exception {
        engine.removeWorkspace(workspaceName1);
        verifyZeroInteractions(layout);
    }

    @Test
    public void shouldForwardRemoveWorkspaceToIndexLayout() throws Exception {
        engine.getWorkspace(workspaceName1);
        engine.removeWorkspace(workspaceName1);
        verify(layout).destroyIndexes(context, sourceName, workspaceName1);
        verifyNoMoreInteractions(layout);
    }

    @Test
    public void shouldForwardRemoveWorkspaceToIndexLayoutForEachWorkspaceThatWasLoaded() throws Exception {
        engine.getWorkspace(workspaceName1);
        engine.removeWorkspaces();
        verify(layout).destroyIndexes(context, sourceName, workspaceName1);
        verifyZeroInteractions(layout);
    }

    @Test
    public void shouldForwardRemoveWorkspaceToIndexLayoutForAllWorkspacesThatWereLoaded() throws Exception {
        engine.getWorkspace(workspaceName1);
        engine.getWorkspace(workspaceName2);
        engine.removeWorkspaces();
        verify(layout).destroyIndexes(context, sourceName, workspaceName1);
        verify(layout).destroyIndexes(context, sourceName, workspaceName2);
        verifyNoMoreInteractions(layout);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailIfNullWorkspaceNamePassedToRemoveWorkspace() throws Exception {
        engine.removeWorkspace(null);
    }

    @Test
    public void shouldForwardOptimizeOfWorkspaceToIndexSession() throws Exception {
        engine.optimize(workspaceName1);
        verify(sessionWs1).optimize();
        verify(sessionWs1).commit();
        verifyNoMoreInteractions(sessionWs1);
    }

    @Test
    public void shouldForwardOptimizeOfAllWorkspacesToEachIndexSession() throws Exception {
        engine.optimize(); // will find all three workspaces
        verify(sessionWs1).optimize();
        verify(sessionWs1).commit();
        verifyNoMoreInteractions(sessionWs1);
        verify(sessionWs2).optimize();
        verify(sessionWs2).commit();
        verifyNoMoreInteractions(sessionWs2);
        verify(sessionDefault).optimize();
        verify(sessionDefault).commit();
        verifyNoMoreInteractions(sessionDefault);
    }

    @Test
    public void shouldForwardIndexOfWorkspaceToIndexSession() throws Exception {
        loadContent();
        engine.index(workspaceName1, 3);
        verify(sessionWs1, times(18)).index((Node)anyObject());
        verify(sessionWs1).commit();
    }

    @Test
    public void shouldForwardIndexOfSubgraphInWorkspaceToIndexSession() throws Exception {
        loadContent();
        engine.index(workspaceName1, path("/Cars"), 3);
        verify(sessionWs1).deleteBelow(path("/Cars"));
        verify(sessionWs1, times(17)).index((Node)anyObject());
        verify(sessionWs1).commit();
    }

    @Test
    public void shouldForwardIndexEntireWorkspaceToIndexSession() throws Exception {
        loadContent();
        engine.index(workspaceName1, path("/"), 3);
        verify(sessionWs1, times(18)).index((Node)anyObject());
        verify(sessionWs1).commit();
    }

    @Test
    public void shouldForwardIndexOfAllWorkspacesToEachIndexSession() throws Exception {
        loadContent();
        engine.index(3); // will find all three workspaces
        verify(sessionWs1, times(18)).index((Node)anyObject());
        verify(sessionWs1).commit();
        verify(sessionWs2, times(24)).index((Node)anyObject());
        verify(sessionWs2).commit();
        verify(sessionDefault, times(1)).index((Node)anyObject());
        verify(sessionDefault).commit();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldForwardSearchToIndexSession() throws Exception {
        String query = "term1 term2";
        engine.fullTextSearch(context, workspaceName1, query, 3, 0);
        verify(sessionWs1).search(eq(context), eq(query), eq(3), eq(0), (List<Location>)anyObject());
        verify(sessionWs1).commit();
    }

    @Test
    public void shouldForwardQueryToIndexSession() throws Exception {
        Query query = mock(Query.class);
        Schemata schemata = mock(Schemata.class);
        engine.query(context, workspaceName1, query, schemata);
        verify(sessionWs1).query(eq(new QueryContext(context, schemata)), eq(query));
        verify(sessionWs1).commit();
    }

    // These tests expect there to be some real IndexLayout ...

    // @Test
    // public void shouldIndexAllContentInRepositorySource() throws Exception {
    // loadContent();
    // engine.index(3);
    // }
    //
    // @Test
    // public void shouldIndexAllContentInWorkspace() throws Exception {
    // loadContent();
    // engine.index(workspaceName1, 3);
    // engine.index(workspaceName2, 5);
    // }
    //
    // @Test
    // public void shouldIndexAllContentInWorkspaceBelowPath() throws Exception {
    // loadContent();
    // engine.index(workspaceName1, path("/Cars/Hybrid"), 3);
    // engine.index(workspaceName2, path("/Aircraft/Commercial"), 5);
    // }
    //
    // @Test
    // public void shouldReIndexAllContentInWorkspaceBelowPath() throws Exception {
    // loadContent();
    // for (int i = 0; i != 0; i++) {
    // engine.index(workspaceName1, path("/Cars/Hybrid"), 3);
    // engine.index(workspaceName2, path("/Aircraft/Commercial"), 5);
    // }
    // }
    //
    // @Test
    // public void shouldHaveLoadedTestContentIntoRepositorySource() {
    // assertThat(content.getNodeAt("/Cars/Hybrid/Toyota Prius").getProperty("msrp").getFirstValue(), is((Object)"$21,500"));
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfOne() {
    // engine.index(workspaceName1, path("/"), 1);
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTwo() {
    // engine.index(workspaceName1, path("/"), 2);
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfThree() {
    // engine.index(workspaceName1, path("/"), 3);
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfFour() {
    // engine.index(workspaceName1, path("/"), 4);
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTen() {
    // engine.index(workspaceName1, path("/"), 10);
    // }
    //
    // @Test
    // public void shouldIndexRepositoryContentStartingAtNonRootNode() {
    // engine.index(workspaceName1, path("/Cars"), 10);
    // }
    //
    // @Test
    // public void shouldReIndexRepositoryContentStartingAtNonRootNode() {
    // for (int i = 0; i != 3; ++i) {
    // engine.index(workspaceName1, path("/Cars"), 10);
    // }
    // }
    //
    // @Test
    // public void shouldFindNodesByFullTextSearch() {
    // engine.index(workspaceName1, path("/"), 100);
    // List<Location> results = engine.fullTextSearch(context, workspaceName1, "Toyota Prius", 10, 0);
    // assertThat(results, is(notNullValue()));
    // assertThat(results.size(), is(2));
    // assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
    // assertThat(results.get(1).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    // }
    //
    // @Test
    // public void shouldFindNodesByFullTextSearchWithOffset() {
    // engine.index(workspaceName1, path("/"), 100);
    // List<Location> results = engine.fullTextSearch(context, workspaceName1, "toyota prius", 1, 0);
    // assertThat(results, is(notNullValue()));
    // assertThat(results.size(), is(1));
    // assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
    //
    // results = engine.fullTextSearch(context, workspaceName1, "+Toyota", 1, 1);
    // assertThat(results, is(notNullValue()));
    // assertThat(results.size(), is(1));
    // assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    // }
}
