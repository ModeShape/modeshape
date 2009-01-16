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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
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
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.SimpleRepository;
import org.jboss.dna.graph.connector.SimpleRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.junit.Before;
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
    private SimpleRepository config;
    private SimpleRepositorySource configSource;
    private SimpleRepository cache;
    private SimpleRepositorySource cacheSource;
    private SimpleRepository repository1;
    private SimpleRepositorySource source1;
    private SimpleRepository repository2;
    private SimpleRepositorySource source2;
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
        source.setConfigurationSourcePath("/repos/RepoX");
        source.setSecurityDomain(securityDomain);
        source.initialize(repositoryContext);

        // Set up the configuration repository with its content ...
        config = SimpleRepository.get("Configuration Repository");
        configSource = new SimpleRepositorySource();
        configSource.setRepositoryName(config.getRepositoryName());
        configSource.setName(configurationSourceName);

        // Set up the cache repository ...
        cache = SimpleRepository.get("Cache");
        cacheSource = new SimpleRepositorySource();
        cacheSource.setRepositoryName(cache.getRepositoryName());
        cacheSource.setName("cache source");

        // Set up the first source repository ...
        repository1 = SimpleRepository.get("source 1");
        source1 = new SimpleRepositorySource();
        source1.setRepositoryName(repository1.getRepositoryName());
        source1.setName("source 1");

        // Set up the second source repository ...
        repository2 = SimpleRepository.get("source 2");
        source2 = new SimpleRepositorySource();
        source2.setRepositoryName(repository2.getRepositoryName());
        source2.setName("source 2");

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

    protected void set( SimpleRepository repository,
                        String path,
                        String propertyName,
                        Object... values ) {
        repository.setProperty(context, path, propertyName, values);
    }

    protected void assertNonExistant( RepositorySource source,
                                      String path ) throws Exception {
        RepositoryConnection connection = null;
        Path pathObj = context.getValueFactories().getPathFactory().create(path);
        try {
            connection = source.getConnection();
            ReadNodeRequest request = new ReadNodeRequest(new Location(pathObj));
            connection.execute(context, request);
            assertThat(request.hasError(), is(true));
            assertThat(request.getError(), instanceOf(PathNotFoundException.class));
        } finally {
            if (connection != null) connection.close();
        }
    }

    protected void assertChildren( RepositorySource source,
                                   String path,
                                   String... children ) throws Exception {
        RepositoryConnection connection = null;
        Path pathObj = context.getValueFactories().getPathFactory().create(path);
        try {
            connection = source.getConnection();
            ReadNodeRequest request = new ReadNodeRequest(new Location(pathObj));
            connection.execute(context, request);
            assertThat(request.hasError(), is(false));
            if (children == null || children.length == 0) {
                assertThat(request.getChildren().isEmpty(), is(true));
            } else {
                // We can't use Location.equals(...), since we're only given the expected paths and the actual
                // locations may have more than just paths. So, create a list of actual locations that just have paths ...
                List<Location> actualChildren = request.getChildren();
                List<Location> actualPathOnlyChildren = new ArrayList<Location>(actualChildren.size());
                for (Location actualChild : actualChildren) {
                    actualPathOnlyChildren.add(new Location(actualChild.getPath()));
                }
                // Now create the array of expected locations (that each contain only a path) ...
                Location[] expectedChildren = new Location[children.length];
                int i = 0;
                for (String child : children) {
                    Path.Segment segment = context.getValueFactories().getPathFactory().createSegment(child);
                    Path childPath = context.getValueFactories().getPathFactory().create(pathObj, segment);
                    expectedChildren[i++] = new Location(childPath);
                }
                assertThat(actualPathOnlyChildren, hasItems(expectedChildren));
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    protected void assertProperty( RepositorySource source,
                                   String path,
                                   String propertyName,
                                   Object... values ) throws Exception {
        RepositoryConnection connection = null;
        Path pathObj = context.getValueFactories().getPathFactory().create(path);
        try {
            connection = source.getConnection();
            ReadNodeRequest request = new ReadNodeRequest(new Location(pathObj));
            connection.execute(context, request);
            assertThat(request.hasError(), is(false));
            Name name = context.getValueFactories().getNameFactory().create(propertyName);
            Property property = request.getPropertiesByName().get(name);
            assertThat(property.getValuesAsArray(), is(values));
        } finally {
            if (connection != null) connection.close();
        }
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

    @Test
    public void shouldProvideReadAccessToContentFederatedFromOneSourceThatMatchesTheContentFromTheSource() throws Exception {
        // Set up the configuration to use a single sources.
        set(config, "/repos/RepoX/dna:federation/", "dna:timeToCache", 100000);
        set(config, "/repos/RepoX/dna:federation/dna:cache/cache source/", "dna:projectionRules", "/ => /");
        set(config, "/repos/RepoX/dna:federation/dna:projections/source 1/", "dna:projectionRules", "/ => /s1");

        // Set up the content in the first source ...
        set(repository1, "/s1/a/a/a", "desc", "description for /a/a/a");
        set(repository1, "/s1/a/a/b", "desc", "description for /a/a/b");
        set(repository1, "/s1/a/a/c", "desc", "description for /a/a/c");
        set(repository1, "/s1/a/b/a", "desc", "description for /a/b/a");
        set(repository1, "/s1/a/b/b", "desc", "description for /a/b/b");
        set(repository1, "/s1/a/b/c", "desc", "description for /a/b/c");
        set(repository1, "/s1/b/a", "desc", "description for /b/a");
        set(repository1, "/s1/b/b", "desc", "description for /b/b");
        set(repository1, "/s1/b/c", "desc", "description for /b/c");

        // Create a connection to the federated repository. Doing so will read the config information.
        RepositoryConnection fedConnection = source.getConnection();
        assertThat(fedConnection, is(notNullValue()));
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

    @Test
    public void shouldProvideReadAccessToContentFederatedFromMultipleSources() throws Exception {
        // Set up the configuration to use multiple sources.
        set(config, "/repos/RepoX/dna:federation/", "dna:timeToCache", 100000);
        set(config, "/repos/RepoX/dna:federation/dna:cache/cache source/", "dna:projectionRules", "/ => /");
        set(config, "/repos/RepoX/dna:federation/dna:projections/source 1/", "dna:projectionRules", "/ => /s1");
        set(config, "/repos/RepoX/dna:federation/dna:projections/source 2/", "dna:projectionRules", "/ => /s2");

        // Set up the content in the first source ...
        set(repository1, "/s1/a/a/a", "desc", "description for /a/a/a");
        set(repository1, "/s1/a/a/b", "desc", "description for /a/a/b");
        set(repository1, "/s1/a/a/c", "desc", "description for /a/a/c");
        set(repository1, "/s1/a/b/a", "desc", "description for /a/b/a");
        set(repository1, "/s1/a/b/b", "desc", "description for /a/b/b");
        set(repository1, "/s1/a/b/c", "desc", "description for /a/b/c");
        set(repository1, "/s1/b/a", "desc", "description for /b/a");
        set(repository1, "/s1/b/b", "desc", "description for /b/b");
        set(repository1, "/s1/b/c", "desc", "description for /b/c");

        // Set up the content in the first source ...
        set(repository2, "/s2/x/x/x", "desc", "description for /x/x/x");
        set(repository2, "/s2/x/x/y", "desc", "description for /x/x/y");
        set(repository2, "/s2/x/x/z", "desc", "description for /x/x/z");
        set(repository2, "/s2/x/y/x", "desc", "description for /x/y/x");
        set(repository2, "/s2/x/y/y", "desc", "description for /x/y/y");
        set(repository2, "/s2/x/y/z", "desc", "description for /x/y/z");
        set(repository2, "/s2/y/x", "desc", "description for /y/x");
        set(repository2, "/s2/y/y", "desc", "description for /y/y");
        set(repository2, "/s2/y/z", "desc", "description for /y/z");

        // Create a connection to the federated repository. Doing so will read the config information.
        RepositoryConnection fedConnection = source.getConnection();
        assertThat(fedConnection, is(notNullValue()));
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
