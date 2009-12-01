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
package org.jboss.dna.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.security.auth.callback.CallbackHandler;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.observe.Observer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * 
 */
public class FederatedRepositorySourceTest {

    private FederatedRepositorySource source;
    private String sourceName;
    private String repositoryName;
    private String configurationSourceName;
    private InMemoryRepositorySource configRepositorySource;
    private RepositoryConnection configRepositoryConnection;
    private ExecutionContext context;
    @Mock
    private RepositoryConnection connection;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryContext repositoryContext;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        configurationSourceName = "configuration";
        repositoryName = "Test Repository";
        configRepositorySource = new InMemoryRepositorySource();
        configRepositorySource.setName("Configuration Repository");

        repositoryContext = new RepositoryContext() {
            @SuppressWarnings( "synthetic-access" )
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return connectionFactory;
            }

            @SuppressWarnings( "synthetic-access" )
            public Subgraph getConfiguration( int depth ) {
                Graph result = Graph.create(configRepositorySource, context);
                result.useWorkspace("configSpace");
                return result.getSubgraphOfDepth(depth).at("/a/b/Test Repository");
            }
        };
        configRepositorySource.initialize(repositoryContext);

        source = new FederatedRepositorySource();
        source.setName(repositoryName);
        sourceName = "federated source";
        source.setName(sourceName);
        source.initialize(repositoryContext);

        Graph configRepository = Graph.create(configRepositorySource, context);
        configRepository.createWorkspace().named("configSpace");
        Graph.Batch batch = configRepository.batch();
        batch.create("/a").and();
        batch.create("/a/b").and();
        batch.create("/a/b/Test Repository").with(DnaLexicon.DEFAULT_WORKSPACE_NAME, "fedSpace").and();
        batch.create("/a/b/Test Repository/dna:workspaces").and();
        batch.create("/a/b/Test Repository/dna:workspaces/fedSpace").and();
        batch.create("/a/b/Test Repository/dna:workspaces/fedSpace/dna:cache")
             .with(DnaLexicon.PROJECTION_RULES, "/ => /")
             .with(DnaLexicon.SOURCE_NAME, "cache source")
             .with(DnaLexicon.WORKSPACE_NAME, "cacheSpace")
             .with(DnaLexicon.TIME_TO_EXPIRE, 100000)
             .and();
        batch.create("/a/b/Test Repository/dna:workspaces/fedSpace/dna:projections").and();
        batch.create("/a/b/Test Repository/dna:workspaces/fedSpace/dna:projections/projection1")
             .with(DnaLexicon.PROJECTION_RULES, "/ => /s1")
             .with(DnaLexicon.SOURCE_NAME, "source 1")
             .with(DnaLexicon.WORKSPACE_NAME, "s1 workspace")
             .and();
        batch.create("/a/b/Test Repository/dna:workspaces/fedSpace/dna:projections/projection2")
             .with(DnaLexicon.PROJECTION_RULES, "/ => /s2")
             .with(DnaLexicon.SOURCE_NAME, "source 2")
             .with(DnaLexicon.WORKSPACE_NAME, "s2 worskspace")
             .and();
        batch.execute();

        configRepositoryConnection = configRepositorySource.getConnection();
        stub(connectionFactory.createConnection(configurationSourceName)).toReturn(configRepositoryConnection);
    }

    protected static CallbackHandler anyCallbackHandler() {
        return argThat(new ArgumentMatcher<CallbackHandler>() {
            @Override
            public boolean matches( Object callback ) {
                return callback != null;
            }
        });
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(true));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    @Test
    public void shouldCreateConnectionsByAuthenticateUsingFederationRepository() throws Exception {
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new FederatedRepositorySource();
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("Something");
        assertThat(source.getName(), is("Something"));
        source.setName("another name");
        assertThat(source.getName(), is("another name"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("some name");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(FederatedRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldSetRetryLimitToZeroWhenSetWithNonPositiveValue() {
        source.setRetryLimit(0);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-1);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-100);
        assertThat(source.getRetryLimit(), is(0));
    }

    @Test
    public void shouldAllowRetryLimitToBeSet() {
        for (int i = 0; i != 100; ++i) {
            source.setRetryLimit(i);
            assertThat(source.getRetryLimit(), is(i));
        }
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReference() throws Exception {
        int retryLimit = 100;
        source.setRetryLimit(retryLimit);
        source.setName("Some source");

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(FederatedRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(FederatedRepositorySource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat((String)refAttributes.remove(FederatedRepositorySource.SOURCE_NAME), is(source.getName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.RETRY_LIMIT), is(Integer.toString(retryLimit)));
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new FederatedRepositorySource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        FederatedRepositorySource recoveredSource = (FederatedRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }
}
