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
package org.modeshape.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.cache.BasicCachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;

/**
 * @author Serge Pagop
 */
public class SvnRepositorySourceTest {

    private SvnRepositorySource source;
    private RepositoryConnection connection;
    private String validName;
    private static String url;
    private String username;
    private String password;
    private final ExecutionContext context = new ExecutionContext();

    @BeforeClass
    public static void beforeAny() throws Exception {
        url = SvnConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");

    }

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.source = new SvnRepositorySource();
        // Set the mandatory properties ...
        this.source.setName("Test Repository");
        this.source.setUsername("sp");
        this.source.setPassword("");
        this.source.setRepositoryRootUrl(url);
        this.source.initialize(new RepositoryContext() {

            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            @SuppressWarnings( "synthetic-access" )
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return new RepositoryConnectionFactory() {

                    public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                        return null;
                    }

                };
            }

        });
    }

    @After
    public void afterEach() throws Exception {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldNotSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(false));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(false));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new SvnRepositorySource();
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("name you like");
        assertThat(source.getName(), is("name you like"));
        source.setName("name you do not like");
        assertThat(source.getName(), is("name you do not like"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("something that can change the world");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(SvnRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSVNUrl() {
        source.setRepositoryRootUrl(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptySVNUrl() {
        source.setRepositoryRootUrl("");
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

    @Test( expected = RepositorySourceException.class )
    public void shouldFailToCreateConnectionIfSourceHasNoName() {
        source.setName(null);
        source.getConnection();
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldFailToCreateConnectionIfSourceHasNoUsername() {
        source.setUsername(null);
        source.getConnection();
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldFailToCreateConnectionIfSourceHasNoPassword() {
        source.setPassword(null);
        source.getConnection();
    }

    @Test
    public void shouldCreateConnection() throws Exception {
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReference() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference(validName, url, username, password, 100);
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReferenceWithNullProperties() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference("some source", "url1", null, null, 100);
        convertToAndFromJndiReference(null, "url2", null, null, 100);
    }

    private void convertToAndFromJndiReference( String sourceName,
                                                String url,
                                                String username,
                                                String password,
                                                int retryLimit ) throws Exception {
        source.setRetryLimit(retryLimit);
        source.setName(sourceName);
        source.setRepositoryRootUrl(url);
        source.setUsername(username);
        source.setPassword(password);

        Reference ref = source.getReference();

        assertThat(ref.getClassName(), is(SvnRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(SvnRepositorySource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat((String)refAttributes.remove(SvnRepositorySource.SOURCE_NAME), is(source.getName()));
        assertThat((String)refAttributes.remove(SvnRepositorySource.SVN_REPOSITORY_ROOT_URL), is(source.getRepositoryRootUrl()));
        assertThat((String)refAttributes.remove(SvnRepositorySource.SVN_USERNAME), is(source.getUsername()));
        assertThat((String)refAttributes.remove(SvnRepositorySource.SVN_PASSWORD), is(source.getPassword()));
        assertThat((String)refAttributes.remove(SvnRepositorySource.ROOT_NODE_UUID),
                   is(source.getRootNodeUuidObject().toString()));
        assertThat((String)refAttributes.remove(SvnRepositorySource.RETRY_LIMIT), is(Integer.toString(source.getRetryLimit())));
        assertThat((String)refAttributes.remove(SvnRepositorySource.ALLOW_CREATING_WORKSPACES),
                   is(Boolean.toString(source.isCreatingWorkspacesAllowed())));
        assertThat((String)refAttributes.remove(SvnRepositorySource.DEFAULT_WORKSPACE), is(source.getDefaultWorkspaceName()));
        refAttributes.remove(SvnRepositorySource.PREDEFINED_WORKSPACE_NAMES);
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new SvnRepositorySource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        SvnRepositorySource recoveredSource = (SvnRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getRepositoryRootUrl(), is(source.getRepositoryRootUrl()));
        assertThat(recoveredSource.getUsername(), is(source.getUsername()));
        assertThat(recoveredSource.getPassword(), is(source.getPassword()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }

    @Test
    public void shouldAllowMultipleConnectionsToBeOpenAtTheSameTime() throws Exception {
        List<RepositoryConnection> connections = new ArrayList<RepositoryConnection>();
        try {
            for (int i = 0; i != 10; ++i) {
                RepositoryConnection conn = source.getConnection();
                assertThat(conn, is(notNullValue()));
                connections.add(conn);
            }
        } finally {
            // Close all open connections ...
            for (RepositoryConnection conn : connections) {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }
}
