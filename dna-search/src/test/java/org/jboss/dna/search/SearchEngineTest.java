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
import java.io.IOException;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.search.SearchEngine;
import org.jboss.dna.graph.search.SearchProvider;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SearchEngineTest {

    private SearchEngine engine;
    private SearchProvider provider;
    private ExecutionContext context;
    private String sourceName;
    private String workspaceName1;
    private String workspaceName2;
    private InMemoryRepositorySource source;
    private RepositoryConnectionFactory connectionFactory;
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

        // Set up the provider and the search engine ...
        IndexRules rules = DualIndexSearchProvider.DEFAULT_RULES;
        LuceneConfiguration luceneConfig = LuceneConfigurations.inMemory();
        // LuceneConfiguration luceneConfig = LuceneConfigurations.using(new File("target/testIndexes"));
        provider = new DualIndexSearchProvider(luceneConfig, rules);
        engine = new SearchEngine(context, sourceName, connectionFactory, provider);
        loadContent();
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
}
