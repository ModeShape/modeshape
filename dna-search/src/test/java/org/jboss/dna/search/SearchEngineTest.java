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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
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
    private DirectoryConfiguration directoryFactory;
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

        // Load some content ...
        content.useWorkspace(workspaceName1);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");
        content.useWorkspace(workspaceName2);
        content.importXmlFrom(getClass().getClassLoader().getResourceAsStream("aircraft.xml")).into("/");

        // Set up the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return source.getConnection();
            }
        };

        // Now set up the search engine ...
        directoryFactory = DirectoryConfigurations.inMemory();
        engine = new SearchEngine(context, sourceName, connectionFactory, directoryFactory);
    }

    protected Path path( String string ) {
        return context.getValueFactories().getPathFactory().create(string);
    }

    @Test
    public void shouldHaveLoadedTestContentIntoRepositorySource() {
        content.useWorkspace(workspaceName1);
        assertThat(content.getNodeAt("/Cars/Hybrid/Toyota Prius").getProperty("msrp").getFirstValue(), is((Object)"$21,500"));
        content.useWorkspace(workspaceName2);
        assertThat(content.getNodeAt("/Aircraft/Commercial/Boeing 787").getProperty("range").getFirstValue(),
                   is((Object)"3050nm"));
    }

    @Test
    public void shouldHaveExecutionContext() {
        assertThat(engine.getContext(), is(sameInstance(context)));
    }

    @Test
    public void shouldHaveSourceName() {
        assertThat(engine.getSourceName(), is(sourceName));
    }

    @Test
    public void shouldFindExistingWorkspaces() {
        assertThat(engine.getWorkspaceEngine(workspaceName1), is(notNullValue()));
        assertThat(engine.getWorkspaceEngine(workspaceName2), is(notNullValue()));
    }

    @Test( expected = InvalidWorkspaceException.class )
    public void shouldNotFindNonExistingWorkspaces() {
        engine.getWorkspaceEngine("Non-existant workspace");
    }

    @Test
    public void shouldIndexAllContentInRepositorySource() {
        engine.indexContent(3);
    }

    @Test
    public void shouldIndexAllContentInWorkspace() {
        engine.indexContent(workspaceName1, 3);
        engine.indexContent(workspaceName2, 5);
    }

    @Test
    public void shouldIndexAllContentInWorkspaceBelowPath() {
        engine.indexContent(workspaceName1, path("/Cars/Hybrid"), 3);
        engine.indexContent(workspaceName2, path("/Aircraft/Commercial"), 5);
    }

    @Test
    public void shouldReIndexAllContentInWorkspaceBelowPath() {
        for (int i = 0; i != 0; i++) {
            engine.indexContent(workspaceName1, path("/Cars/Hybrid"), 3);
            engine.indexContent(workspaceName2, path("/Aircraft/Commercial"), 5);
        }
    }
}
