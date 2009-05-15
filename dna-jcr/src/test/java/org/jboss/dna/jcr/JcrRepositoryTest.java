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
package org.jboss.dna.jcr;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrRepositoryTest {

    private String sourceName;
    private ExecutionContext context;
    private JcrRepository repository;
    private InMemoryRepositorySource source;
    private Map<String, String> descriptors;
    private RepositoryConnectionFactory connectionFactory;
    protected AccessControlContext accessControlContext = AccessController.getContext();
    @Mock
    LoginContext loginContext;
    private Credentials credentials = new Credentials() {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unused" )
        public AccessControlContext getAccessControlContext() {
            return accessControlContext;
        }
    };

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        sourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(sourceName);

        // Set up the execution context ...
        context = new ExecutionContext();

        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return sourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Make sure the path to the namespaces exists ...
        Graph graph = Graph.create(source, context);
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

        // Set up the repository ...
        descriptors = new HashMap<String, String>();
        repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, null);
    }

    @Test
    public void shouldAllowNullDescriptors() {
        new JcrRepository(context, connectionFactory, sourceName, null, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullExecutionContext() throws Exception {
        new JcrRepository(null, connectionFactory, sourceName, descriptors, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullConnectionFactories() throws Exception {
        new JcrRepository(context, null, sourceName, descriptors, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSourceName() throws Exception {
        new JcrRepository(context, connectionFactory, null, descriptors, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoDescriptorKey() {
        repository.getDescriptor(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyDescriptorKey() {
        repository.getDescriptor("");
    }

    @Test
    public void shouldProvideBuiltInDescriptorKeys() {
        testDescriptorKeys(repository);
    }

    @Test
    public void shouldProvideDescriptorValues() {
        testDescriptorValues(repository);
    }

    @Test
    public void shouldProvideBuiltInDescriptorsWhenNotSuppliedDescriptors() {
        Repository repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, null);
        testDescriptorKeys(repository);
        testDescriptorValues(repository);
    }

    @Test
    public void shouldHaveDefaultOptionsWhenNotOverridden() {
        JcrRepository repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, null);
        assertThat(repository.getOptions().get(JcrRepository.Option.PROJECT_NODE_TYPES),
                   is(JcrRepository.DefaultOption.PROJECT_NODE_TYPES));
    }

    @Test
    public void shouldProvideUserSuppliedDescriptors() {
        Map<String, String> descriptors = new HashMap<String, String>();
        descriptors.put("property", "value");
        Repository repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, null);
        testDescriptorKeys(repository);
        testDescriptorValues(repository);
        assertThat(repository.getDescriptor("property"), is("value"));
    }

    @Test
    public void shouldAllowLoginWithNoCredentials() throws Exception {
        Session session = repository.login();
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login((Credentials)null);
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login(null, JcrI18n.defaultWorkspaceName.text());
        assertThat(session, notNullValue());
    }

    @Test
    public void shouldAllowLoginWithProperCredentials() throws Exception {
        repository.login(credentials);
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public LoginContext getLoginContext() throws LoginException {
                return loginContext;
            }
        });
    }

    @Test
    public void shouldAllowLoginWithNoWorkspaceName() throws Exception {
        Session session = repository.login((String)null);
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login(credentials, null);
        assertThat(session, notNullValue());
        session.logout();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsDoNotProvideJaasMethod() throws Exception {
        repository.login(Mockito.mock(Credentials.class));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNoAccessControlContext() throws Exception {
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public AccessControlContext getAccessControlContext() {
                return null;
            }
        });
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNoLoginContext() throws Exception {
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public LoginContext getLoginContext() {
                return null;
            }
        });
    }

    private void testDescriptorKeys( Repository repository ) {
        String[] keys = repository.getDescriptorKeys();
        assertThat(keys, notNullValue());
        assertThat(keys.length >= 15, is(true));
        assertThat(keys, hasItemInArray(Repository.LEVEL_1_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.LEVEL_2_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_LOCKING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_OBSERVATION_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_QUERY_SQL_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_TRANSACTIONS_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_VERSIONING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_DOC_ORDER));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_POS_INDEX));
        assertThat(keys, hasItemInArray(Repository.REP_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_URL_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VERSION_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_VERSION_DESC));
    }

    private void testDescriptorValues( Repository repository ) {
        assertThat(repository.getDescriptor(Repository.LEVEL_1_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.LEVEL_2_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_LOCKING_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_TRANSACTIONS_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_DOC_ORDER), is("true"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_POS_INDEX), is("true"));
        assertThat(repository.getDescriptor(Repository.REP_NAME_DESC), is(JcrI18n.REP_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_DESC), is(JcrI18n.REP_VENDOR_DESC.text()));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_URL_DESC), is("http://www.jboss.org/dna"));
        assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC), is("0.4"));
        assertThat(repository.getDescriptor(Repository.SPEC_NAME_DESC), is(JcrI18n.SPEC_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.SPEC_VERSION_DESC), is("1.0"));
    }
}
