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
package org.jboss.dna.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.security.auth.callback.CallbackHandler;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * This JUnit test constructs a federated repository of multiple repository sources, and is basically a lightweight integration
 * test that uses all of the actual components of a {@link FederatedRepository} but using mock or fake components for other
 * systems.
 * 
 * @author Randall Hauch
 */
public class FederatedRepositorySourceIntegrationTest {

    private FederatedRepositorySource source;
    private String sourceName;
    private String username;
    private String credentials;
    private String executionContextFactoryJndiName;
    private String repositoryConnectionFactoryJndiName;
    private String configurationSourceName;
    private String securityDomain;
    private Graph config;
    private InMemoryRepositorySource configSource;
    private Graph cache;
    private InMemoryRepositorySource cacheSource;
    private Graph repository1;
    private InMemoryRepositorySource source1;
    private Graph repository2;
    private InMemoryRepositorySource source2;
    private ExecutionContext context;
    private RepositorySource[] sources;
    @Mock
    private Context jndiContext;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    @Mock
    private ExecutionContext executionContextFactory;
    @Mock
    private RepositoryContext repositoryContext;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Set up the environment (ExecutionContext, JNDI, security, etc.)
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        executionContextFactoryJndiName = "context factory jndi name";
        repositoryConnectionFactoryJndiName = "repository connection factory jndi name";
        configurationSourceName = "configuration source name";
        securityDomain = "security domain";
        stub(jndiContext.lookup(executionContextFactoryJndiName)).toReturn(executionContextFactory);
        stub(jndiContext.lookup(repositoryConnectionFactoryJndiName)).toReturn(connectionFactory);
        stub(executionContextFactory.with(eq(securityDomain), anyCallbackHandler())).toReturn(context);
        stub(repositoryContext.getExecutionContext()).toReturn(executionContextFactory);
        stub(repositoryContext.getRepositoryConnectionFactory()).toReturn(connectionFactory);

        // Set up the federated repository source ...
        source = new FederatedRepositorySource("Test Repository");
        sourceName = "federated source";
        username = "valid username";
        credentials = "valid password";
        source.setName(sourceName);
        source.setUsername(username);
        source.setPassword(credentials);
        source.setConfigurationSourceName(configurationSourceName);
        source.setConfigurationWorkspaceName("configSpace");
        source.setConfigurationSourcePath("/repos/RepoX");
        source.setSecurityDomain(securityDomain);
        source.initialize(repositoryContext);

        // Set up the configuration repository with its content ...
        configSource = new InMemoryRepositorySource();
        configSource.setName(configurationSourceName);
        config = Graph.create(configSource, context);

        // Set up the cache repository ...
        cacheSource = new InMemoryRepositorySource();
        cacheSource.setName("cache source");
        cache = Graph.create(cacheSource, context);

        // Set up the first source repository ...
        source1 = new InMemoryRepositorySource();
        source1.setName("source 1");
        repository1 = Graph.create(source1, context);

        // Set up the second source repository ...
        source2 = new InMemoryRepositorySource();
        source2.setName("source 2");
        repository2 = Graph.create(source2, context);

        // Set up the connection factory to return connections to the sources ...
        sources = new RepositorySource[] {spy(configSource), spy(cacheSource), spy(source1), spy(source2)};
        for (final RepositorySource source : sources) {
            stub(connectionFactory.createConnection(source.getName())).toAnswer(new Answer<RepositoryConnection>() {
                public RepositoryConnection answer( InvocationOnMock invocation ) throws Throwable {
                    return source.getConnection();
                }
            });
        }
    }

    protected static CallbackHandler anyCallbackHandler() {
        return argThat(new ArgumentMatcher<CallbackHandler>() {
            @Override
            public boolean matches( Object callback ) {
                return callback != null;
            }
        });
    }

    protected void assertNonExistant( Graph source,
                                      String path ) throws Exception {
        try {
            source.getNodeAt(path);
            fail("failed to find node in " + source + " in workspace " + source.getCurrentWorkspaceName() + " at " + path);
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected void assertChildren( Graph source,
                                   String path,
                                   String... children ) throws Exception {
        List<Location> childLocations = source.getChildren().of(path);
        if (children == null || children.length == 0) {
            assertThat(childLocations.isEmpty(), is(true));
        } else {
            // We can't use Location.equals(...), since we're only given the expected paths and the actual
            // locations may have more than just paths. So, create a list of actual locations that just have paths ...
            List<Location> actualPathOnlyChildren = new ArrayList<Location>(childLocations.size());
            for (Location actualChild : childLocations) {
                actualPathOnlyChildren.add(new Location(actualChild.getPath()));
            }
            // Now create the array of expected locations (that each contain only a path) ...
            Location[] expectedChildren = new Location[children.length];
            Path parentPath = context.getValueFactories().getPathFactory().create(path);
            int i = 0;
            for (String child : children) {
                Path.Segment segment = context.getValueFactories().getPathFactory().createSegment(child);
                Path childPath = context.getValueFactories().getPathFactory().create(parentPath, segment);
                expectedChildren[i++] = new Location(childPath);
            }
            assertThat(actualPathOnlyChildren, hasItems(expectedChildren));
        }
    }

    protected void assertProperty( Graph source,
                                   String path,
                                   String propertyName,
                                   Object... values ) throws Exception {
        Property property = source.getProperty(propertyName).on(path);
        assertThat(property.getValuesAsArray(), is(values));
    }

    @Test
    public void shouldReturnCorrectConnectionForEachRepositorySource() throws Exception {
        for (final RepositorySource source : sources) {
            // Get a connection from the factory ...
            RepositoryConnection connection = connectionFactory.createConnection(source.getName());
            assertThat(connection, is(notNullValue()));
            // And check that the connection is for the correct source ...
            assertThat(connection.getSourceName(), is(source.getName()));
            // And close the connection (no need to do this in a finally, since it's a SimpleRepository)
            connection.close();
        }
        // Now, assert that each source created one connection ...
        for (final RepositorySource source : sources) {
            verify(source, times(1)).getConnection();
        }
    }

    @Ignore
    @Test
    public void shouldProvideReadAccessToContentFederatedFromOneSourceThatMatchesTheContentFromTheSource() throws Exception {
        // Set up the configuration to use a single source.
        config.createWorkspace().named("configSpace");
        Graph.Batch batch = config.batch();
        batch.create("/repos").and();
        batch.create("/repos/RepoX").with(FederatedLexicon.DEFAULT_WORKSPACE_NAME, "fedSpace").and();
        batch.create("/repos/RepoX/dna:workspaces").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:cache")
             .with(FederatedLexicon.PROJECTION_RULES, "/ => /")
             .with(FederatedLexicon.SOURCE_NAME, "cache source")
             .with(FederatedLexicon.WORKSPACE_NAME, "cacheSpace")
             .with(FederatedLexicon.TIME_TO_EXPIRE, 100000)
             .and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:projections").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:projections/projection1")
             .with(FederatedLexicon.PROJECTION_RULES, "/ => /s1")
             .with(FederatedLexicon.SOURCE_NAME, "source 1")
             .with(FederatedLexicon.WORKSPACE_NAME, "s1 workspace")
             .and();
        batch.execute();

        // Set up the cache ...
        cache.createWorkspace().named("cacheSpace");

        // Set up the content in the first source ...
        repository1.createWorkspace().named("s1 workspace");
        batch = repository1.batch();
        batch.create("/s1").and();
        batch.create("/s1/a").and();
        batch.create("/s1/a/a").and();
        batch.create("/s1/a/a/a").with("desc", "description for /a/a/a").and();
        batch.create("/s1/a/a/b").with("desc", "description for /a/a/b").and();
        batch.create("/s1/a/a/c").with("desc", "description for /a/a/c").and();
        batch.create("/s1/a/b").and();
        batch.create("/s1/a/b/a").with("desc", "description for /a/b/a").and();
        batch.create("/s1/a/b/b").with("desc", "description for /a/b/b").and();
        batch.create("/s1/a/b/c").with("desc", "description for /a/b/c").and();
        batch.create("/s1/b").and();
        batch.create("/s1/b/a").with("desc", "description for /b/a").and();
        batch.create("/s1/b/b").with("desc", "description for /b/b").and();
        batch.create("/s1/b/c").with("desc", "description for /b/c").and();
        batch.execute();

        // Create a connection to the federated repository. Doing so will read the config information.
        Graph source = Graph.create(this.source, context);
        source.useWorkspace("fedSpace");
        assertChildren(source, "/", "a", "b");
        assertChildren(source, "/a/", "a", "b");
        assertChildren(source, "/a/a", "a", "b", "c");
        assertChildren(source, "/a/a/a");
        assertChildren(source, "/a/a/b");
        assertChildren(source, "/a/a/c");
        assertChildren(source, "/a/b", "a", "b", "c");
        assertChildren(source, "/a/b/a");
        assertChildren(source, "/a/b/b");
        assertChildren(source, "/a/b/c");
        assertChildren(source, "/b", "a", "b", "c");
        assertChildren(source, "/b/a");
        assertChildren(source, "/b/b");
        assertChildren(source, "/b/c");
    }

    @Ignore
    @Test
    public void shouldProvideReadAccessToContentFederatedFromMultipleSources() throws Exception {
        // Set up the configuration to use multiple sources.
        config.createWorkspace().named("configSpace");
        Graph.Batch batch = config.batch();
        batch.create("/repos").and();
        batch.create("/repos/RepoX").and();
        batch.create("/repos/RepoX/dna:workspaces").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:cache")
             .with(FederatedLexicon.PROJECTION_RULES, "/ => /")
             .with(FederatedLexicon.SOURCE_NAME, "cache source")
             .with(FederatedLexicon.WORKSPACE_NAME, "cacheSpace")
             .with(FederatedLexicon.TIME_TO_EXPIRE, 100000)
             .and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:projections").and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:projections/projection1")
             .with(FederatedLexicon.PROJECTION_RULES, "/ => /s1")
             .with(FederatedLexicon.SOURCE_NAME, "source 1")
             .with(FederatedLexicon.WORKSPACE_NAME, "s1 workspace")
             .and();
        batch.create("/repos/RepoX/dna:workspaces/fedSpace/dna:projections/projection2")
             .with(FederatedLexicon.PROJECTION_RULES, "/ => /s2")
             .with(FederatedLexicon.SOURCE_NAME, "source 2")
             .with(FederatedLexicon.WORKSPACE_NAME, "s2 worskspace")
             .and();
        batch.execute();

        // Set up the cache ...
        cache.createWorkspace().named("cacheSpace");

        // Set up the content in the first source ...
        repository1.createWorkspace().named("s1Space");
        batch = repository1.batch();
        batch.create("/s1").and();
        batch.create("/s1/a").and();
        batch.create("/s1/a/a").and();
        batch.create("/s1/a/a/a").with("desc", "description for /a/a/a").and();
        batch.create("/s1/a/a/b").with("desc", "description for /a/a/b").and();
        batch.create("/s1/a/a/c").with("desc", "description for /a/a/c").and();
        batch.create("/s1/a/b").and();
        batch.create("/s1/a/b/a").with("desc", "description for /a/b/a").and();
        batch.create("/s1/a/b/b").with("desc", "description for /a/b/b").and();
        batch.create("/s1/a/b/c").with("desc", "description for /a/b/c").and();
        batch.create("/s1/b").and();
        batch.create("/s1/b/a").with("desc", "description for /b/a").and();
        batch.create("/s1/b/b").with("desc", "description for /b/b").and();
        batch.create("/s1/b/c").with("desc", "description for /b/c").and();
        batch.execute();

        // Set up the content in the second source ...
        repository2.createWorkspace().named("s2 workspace");
        batch = repository2.batch();
        batch.create("/s2").and();
        batch.create("/s2/x").and();
        batch.create("/s2/x/x").and();
        batch.create("/s2/x/x/x").with("desc", "description for /x/x/x").and();
        batch.create("/s2/x/x/y").with("desc", "description for /x/x/y").and();
        batch.create("/s2/x/x/z").with("desc", "description for /x/x/z").and();
        batch.create("/s2/x").and();
        batch.create("/s2/x/y").and();
        batch.create("/s2/x/y/x").with("desc", "description for /x/y/x").and();
        batch.create("/s2/x/y/y").with("desc", "description for /x/y/y").and();
        batch.create("/s2/x/y/z").with("desc", "description for /x/y/z").and();
        batch.create("/s2/y").and();
        batch.create("/s2/y/x").with("desc", "description for /y/x").and();
        batch.create("/s2/y/y").with("desc", "description for /y/y").and();
        batch.create("/s2/y/z").with("desc", "description for /y/z").and();
        batch.execute();

        // Create a connection to the federated repository. Doing so will read the config information.
        Graph source = Graph.create(this.source, context);
        source.useWorkspace("fedSpace");
        assertChildren(source, "/", "a", "b", "x", "y");
        assertChildren(source, "/a/", "a", "b");
        assertChildren(source, "/a/a", "a", "b", "c");
        assertChildren(source, "/a/a/a");
        assertChildren(source, "/a/a/b");
        assertChildren(source, "/a/a/c");
        assertChildren(source, "/a/b", "a", "b", "c");
        assertChildren(source, "/a/b/a");
        assertChildren(source, "/a/b/b");
        assertChildren(source, "/a/b/c");
        assertChildren(source, "/b", "a", "b", "c");
        assertChildren(source, "/b/a");
        assertChildren(source, "/b/b");
        assertChildren(source, "/b/c");
        assertChildren(source, "/x/", "x", "y");
        assertChildren(source, "/x/x", "x", "y", "z");
        assertChildren(source, "/x/x/x");
        assertChildren(source, "/x/x/y");
        assertChildren(source, "/x/x/z");
        assertChildren(source, "/x/y", "x", "y", "z");
        assertChildren(source, "/x/y/x");
        assertChildren(source, "/x/y/y");
        assertChildren(source, "/x/y/z");
        assertChildren(source, "/y", "x", "y", "z");
        assertChildren(source, "/y/x");
        assertChildren(source, "/y/y");
        assertChildren(source, "/y/z");
    }

}
