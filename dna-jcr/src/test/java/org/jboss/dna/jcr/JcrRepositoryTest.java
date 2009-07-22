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
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.MockSecurityContext;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.JaasSecurityContext.UserPasswordCallbackHandler;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 */
public class JcrRepositoryTest {

    private String sourceName;
    private ExecutionContext context;
    private JcrRepository repository;
    private InMemoryRepositorySource source;
    private Map<String, String> descriptors;
    private RepositoryConnectionFactory connectionFactory;
    private Credentials credentials;
    private Graph sourceGraph;
    private Graph systemGraph;
    private JcrSession session;

    @BeforeClass
    public static void beforeClass() {
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        sourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(sourceName);

        // Set up the execution context ...
        context = new ExecutionContext();
        credentials = new SimpleCredentials("superuser", "superuser".toCharArray());

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

        // Set up the repository ...
        descriptors = new HashMap<String, String>();
        repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, null);

        // Set up the graph that goes directly to the source ...
        sourceGraph = Graph.create(source, context);

        // Set up the graph that goes directly to the system source ...
        systemGraph = repository.createSystemGraph();
    }

    @After
    public void afterEach() {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
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

    @Test( expected = javax.jcr.LoginException.class )
    public void shouldNotAllowLoginWithNoCredentials() throws Exception {
        // This would work iff this code was executing in a privileged block, but it's not
        repository.login();
    }

    @Test
    public void shouldAllowLoginWithNoCredentialsInPrivilegedBlock() throws Exception {
        LoginContext login = new LoginContext("dna-jcr", new UserPasswordCallbackHandler("superuser", "superuser".toCharArray()));
        login.login();

        Subject subject = login.getSubject();

        Session session = (Session)Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {

            @SuppressWarnings( "synthetic-access" )
            public Session run() throws Exception {
                return repository.login();
            }

        }, AccessController.getContext());

        assertThat(session, is(notNullValue()));
        assertThat(session.getUserID(), is("superuser"));
        login.logout();
    }

    @Test
    public void shouldAllowLoginWithProperCredentials() throws Exception {
        repository.login(credentials);
        repository.login(new SecurityContextCredentials(
                                                        new MockSecurityContext(
                                                                                null,
                                                                                Collections.singleton(JcrSession.DNA_ADMIN_PERMISSION))));
    }

    @Test
    public void shouldAllowLoginWithNoWorkspaceName() throws Exception {
        Session session = repository.login(credentials, null);
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login(new SecurityContextCredentials(
                                                                  new MockSecurityContext(
                                                                                          null,
                                                                                          Collections.singleton(JcrSession.DNA_ADMIN_PERMISSION))),
                                   (String)null);
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

    @Test
    public void shouldHaveRootNode() throws Exception {
        session = createSession();
        javax.jcr.Node root = session.getRootNode();
        String uuid = root.getUUID();

        // Get the root via the direct graph ...
        Node dnaRoot = sourceGraph.getNodeAt("/");
        UUID dnaRootUuid = dnaRoot.getLocation().getUuid();

        // They should have the same UUID ...
        assertThat(uuid, is(dnaRootUuid.toString()));

        // Get the children of the root node ...
        javax.jcr.NodeIterator iter = root.getNodes();
        javax.jcr.Node system = iter.nextNode();
        assertThat(system.getName(), is("jcr:system"));

        // Add a child node ...
        javax.jcr.Node childA = root.addNode("childA", "nt:unstructured");
        assertThat(childA, is(notNullValue()));
        iter = root.getNodes();
        javax.jcr.Node system2 = iter.nextNode();
        javax.jcr.Node childA2 = iter.nextNode();
        assertThat(system2.getName(), is("jcr:system"));
        assertThat(childA2.getName(), is("childA"));
    }

    @Test
    public void shouldHaveSystemBranch() throws Exception {
        session = createSession();
        javax.jcr.Node root = session.getRootNode();
        AbstractJcrNode system = (AbstractJcrNode)root.getNode("jcr:system");
        UUID uuid = system.location.getUuid();

        for (int i = 0; i != 3; ++i) {
            // Get the same node via the direct graph ...
            Node dnaSystem = systemGraph.getNodeAt("/jcr:system");
            UUID dnaSystemUuid = dnaSystem.getLocation().getUuid();

            // They should have the same UUID ...
            assertThat(uuid, is(dnaSystemUuid));
        }
    }

    @Test
    public void shouldHaveRegisteredThoseNamespacesNeedeByDna() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("dna"), is("http://www.jboss.org/dna/1.0"));
        assertThat(session.getNamespaceURI("dnaint"), is("http://www.jboss.org/dna/internal/1.0"));
    }

    @Test
    public void shouldHaveRegisteredThoseNamespacesDefinedByTheJcrSpecification() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("dna"), is("http://www.jboss.org/dna/1.0"));
        assertThat(session.getNamespaceURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(session.getNamespaceURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(session.getNamespaceURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(session.getNamespaceURI(""), is(""));
    }

    @Test
    public void shouldHaveRegisteredThoseNamespacesDefinedByTheJcrApiJavaDoc() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("sv"), is("http://www.jcp.org/jcr/sv/1.0"));
        assertThat(session.getNamespaceURI("xmlns"), is("http://www.w3.org/2000/xmlns/"));
    }

    @Test
    public void shouldParseSourceNameOptionWithOnlySourceName() {
        assertSourceWorkspacePair("source name", "source name", null);
        assertSourceWorkspacePair(" \t source name \n ", "source name", null);
        assertSourceWorkspacePair(" \t source \\@ name \n ", "source @ name", null);
    }

    @Test
    public void shouldParseSourceNameOptionWithWorkspaceNameAndSourceName() {
        assertSourceWorkspacePair("workspace@source", "source", "workspace");
        assertSourceWorkspacePair(" \t workspace\t@ \t source \t \n", "source", "workspace");
        assertSourceWorkspacePair(" \t workspace\\@ name \t@ \t source\\@name \t \n", "source@name", "workspace@ name");
        assertSourceWorkspacePair("@ \t source \\@ name \n ", "source @ name", "");
        assertSourceWorkspacePair("   @ \t source \\@ name \n ", "source @ name", "");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToParseSourceNameOptionThatHasZeroLengthSource() {
        new JcrRepository.SourceWorkspacePair("workspace@");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToParseSourceNameOptionThatHasBlankSource() {
        new JcrRepository.SourceWorkspacePair("workspace@  ");
    }

    @Test
    public void shouldParseSourceNameOptionThatHasBlankSourceAndWorkspace() {
        assertSourceWorkspacePair("@", "", "");
        assertSourceWorkspacePair(" @ ", "", "");
    }

    protected void assertSourceWorkspacePair( String value,
                                              String expectedSourceName,
                                              String expectedWorkspaceName ) {
        JcrRepository.SourceWorkspacePair pair = new JcrRepository.SourceWorkspacePair(value);
        assertThat(pair, is(notNullValue()));
        assertThat(pair.getSourceName(), is(expectedSourceName));
        assertThat(pair.getWorkspaceName(), is(expectedWorkspaceName));
    }

    protected JcrSession createSession() throws Exception {
        LoginContext login = new LoginContext("dna-jcr", new UserPasswordCallbackHandler("superuser", "superuser".toCharArray()));
        login.login();

        Subject subject = login.getSubject();
        JcrSession session = (JcrSession)Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {

            @SuppressWarnings( "synthetic-access" )
            public Session run() throws Exception {
                return repository.login();
            }

        }, AccessController.getContext());
        return session;
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
