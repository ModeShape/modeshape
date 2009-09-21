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
import static org.mockito.Mockito.mock;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceSearchEngineTest {

    private WorkspaceSearchEngine engine;
    private ExecutionContext context;
    private String sourceName;
    private String workspaceName;
    private InMemoryRepositorySource source;
    private RepositoryConnectionFactory connectionFactory;
    private DirectoryConfiguration directoryFactory;
    private IndexingStrategy indexingStrategy;
    private Graph content;
    private Schemata schemata;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        sourceName = "sourceA";
        workspaceName = "workspace1";

        // Set up the source and graph instance ...
        source = new InMemoryRepositorySource();
        source.setName(sourceName);
        source.setDefaultWorkspaceName(workspaceName);
        content = Graph.create(source, context);

        // Load some content ...
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");

        // Set up the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return source.getConnection();
            }
        };

        // Set up the schemata for the queries ...
        schemata = mock(Schemata.class);

        // Set up the indexing strategy ...
        IndexingRules rules = IndexingRules.createBuilder(StoreLittleIndexingStrategy.DEFAULT_RULES)
                                           .defaultTo(IndexingRules.INDEX | IndexingRules.ANALYZE | IndexingRules.FULL_TEXT)
                                           .build();
        indexingStrategy = new StoreLittleIndexingStrategy(schemata, rules);

        // Now set up the search engine ...
        directoryFactory = DirectoryConfigurations.inMemory();
        engine = new WorkspaceSearchEngine(context, directoryFactory, indexingStrategy, sourceName, workspaceName,
                                           connectionFactory);
    }

    protected Path path( String string ) {
        return context.getValueFactories().getPathFactory().create(string);
    }

    protected void assertSearchResults( String fullTextSearch,
                                        Path... expectedPaths ) {
        int numExpected = expectedPaths.length;
        List<Location> results = engine.fullTextSearch(fullTextSearch, numExpected, 0);
        int numFound = results.size();
        assertThat("Different number of results were found", numExpected, is(numFound));
        Path[] actualPaths = new Path[numFound];
        int i = 0;
        for (Location actual : results) {
            actualPaths[i++] = actual.getPath();
        }
        assertThat(expectedPaths, is(actualPaths));
    }

    @Test
    public void shouldHaveLoadedTestContentIntoRepositorySource() {
        assertThat(content.getNodeAt("/Cars/Hybrid/Toyota Prius").getProperty("msrp").getFirstValue(), is((Object)"$21,500"));
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfOne() {
        engine.indexContent(path("/"), 1);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTwo() {
        engine.indexContent(path("/"), 2);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfThree() {
        engine.indexContent(path("/"), 3);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfFour() {
        engine.indexContent(path("/"), 4);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtRootAndUsingDepthOfTen() {
        engine.indexContent(path("/"), 10);
    }

    @Test
    public void shouldIndexRepositoryContentStartingAtNonRootNode() {
        engine.indexContent(path("/Cars"), 10);
    }

    @Test
    public void shouldReIndexRepositoryContentStartingAtNonRootNode() {
        for (int i = 0; i != 3; ++i) {
            engine.indexContent(path("/Cars"), 10);
        }
    }

    @Test
    public void shouldFindNodesByFullTextSearch() {
        engine.indexContent(path("/"), 100);
        List<Location> results = engine.fullTextSearch("Toyota Prius", 10, 0);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(2));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));
        assertThat(results.get(1).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }

    @Test
    public void shouldFindNodesByFullTextSearchWithOffset() {
        engine.indexContent(path("/"), 100);
        List<Location> results = engine.fullTextSearch("toyota prius", 1, 0);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Prius")));

        results = engine.fullTextSearch("+Toyota", 1, 1);
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getPath(), is(path("/Cars/Hybrid/Toyota Highlander")));
    }
}
