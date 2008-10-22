/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.BasicExecutionContext;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepository;

/**
 * @author Serge Pagop
 */
@SuppressWarnings( "unused" )
public class SVNRepositoryConnectionTest {
    private SVNRepositoryConnection connection;
    private ExecutionContext context;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private PropertyFactory propertyFactory;
    private DAVRepository davRepository;
    private String uuidPropertyName;
    private String sourceName;

    @Mock
    private CachePolicy policy;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        pathFactory = context.getValueFactories().getPathFactory();
        propertyFactory = context.getPropertyFactory();
        nameFactory = context.getValueFactories().getNameFactory();

        // Create a Repository instance from the http-protocol, that use a anonymous credential.
        String url = "http://anonsvn.jboss.org/repos/dna/trunk/extensions/dna-connector-svn/src/test/resources";
        String username = "anonymous";
        String password = "anonymous";
        // Set up the appropriate factory for a particular protocol
        davRepository = SVNConnectorTestUtil.createDAVRepositoryURL(url, username, password);
        sourceName = "the source name";
        uuidPropertyName = "dna:uuid";
        connection = new SVNRepositoryConnection(sourceName, policy, uuidPropertyName, davRepository);
    }

    @After
    public void afterEach() {

    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfSourceNameIsNull() {
        sourceName = null;
        connection = new SVNRepositoryConnection(sourceName, policy, uuidPropertyName, davRepository);
    }
    
    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfRepositoryIsNull() {
        davRepository = null;
        connection = new SVNRepositoryConnection(sourceName, policy, uuidPropertyName, davRepository);
    }
    
    

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfUuidPropertyNameIsNull() throws Exception {
        uuidPropertyName = null;
        connection = new SVNRepositoryConnection(sourceName, policy, uuidPropertyName, davRepository);

    }

    @Test
    public void shouldInstantiateWithValidSourceAndDAVRepositoryReferences() throws Exception {
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsSourceName() {
        assertThat(connection.getSourceName(), is("the source name"));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsDefaultCachePolicy() {
        assertThat(connection.getDefaultCachePolicy(), is(sameInstance(policy)));
    }

    @Test
    public void shouldGetTheSVNRepositoryRootFromTheSVNRepositoryWhenPinged() throws Exception {
        CachePolicy policy = mock(CachePolicy.class);
        davRepository = mock(DAVRepository.class);
        connection = new SVNRepositoryConnection("the source name", policy, SVNRepositorySource.DEFAULT_UUID_PROPERTY_NAME,
                                                 davRepository);
        stub(davRepository.getRepositoryRoot(true)).toReturn(null);
        assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
        verify(davRepository).getRepositoryRoot(true);
    }

    @Test
    public void shouldHaveNoOpListenerWhenCreated() {
        assertThat(connection.getListener(), is(sameInstance(SVNRepositoryConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldUseNoOpListenerWhenSettingListenerToNull() {
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(SVNRepositoryConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldSetListenerToNonNullValue() {
        RepositorySourceListener listener = mock(RepositorySourceListener.class);
        connection.setListener(listener);
        assertThat(connection.getListener(), is(sameInstance(listener)));
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(SVNRepositoryConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldGetUuidPropertyNameFromSouceAndShouldNotChangeDuringLifetimeOfConnection() {
        Name name = connection.getUuidPropertyName(context);
        assertThat(name.getLocalName(), is("uuid"));
        assertThat(name.getNamespaceUri(), is(DnaLexicon.Namespace.URI));
        for (int i = 0; i != 10; ++i) {
            Name name2 = connection.getUuidPropertyName(context);
            assertThat(name2, is(sameInstance(name)));
        }
    }

    @Test
    public void shouldGenerateUuid() {
        for (int i = 0; i != 100; ++i) {
            assertThat(connection.generateUuid(), is(notNullValue()));
        }
    }
    
    @Test
    public void should

    /**
     * Create a Repository instance from the file-protocol.
     * 
     * @return {@link FSRepository} the repository
     * @throws Exception - in case of a exceptional error during the access.
     */
    // @SuppressWarnings( "unused" )
    // private FSRepository createFSRepositoryFromFileProtocol() throws Exception {
    // //TODO to be changed
    // // String url = "file:///Users/sp/SVNRepos/test";
    // String url = "";
    // // Set up the appropriate factory for a particular protocol
    // FSRepositoryFactory.setup();
    // // The factory knows how to create a DAVRepository
    // FSRepository fsRepository = (FSRepository)FSRepositoryFactory.create(SVNURL.parseURIDecoded(url));
    // return fsRepository;
    // }
}
