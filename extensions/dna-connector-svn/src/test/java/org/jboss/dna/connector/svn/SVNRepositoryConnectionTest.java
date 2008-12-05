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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.text.UrlEncoder;
import org.jboss.dna.graph.BasicExecutionContext;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.Path.Segment;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepository;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.io.SVNRepository;

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
    private Graph graph;

    @Mock
    private CachePolicy policy;

    @Mock
    private ReadAllChildrenRequest request;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
        pathFactory = context.getValueFactories().getPathFactory();
        propertyFactory = context.getPropertyFactory();
        nameFactory = context.getValueFactories().getNameFactory();

        // Create a Repository instance from the http-protocol, that use a anonymous credential.
        String url = "http://anonsvn.jboss.org/repos/dna/trunk/extensions/dna-connector-svn/src/test/resources";
        String username = "anonymous";
        String password = "anonymous";
        // Set up the appropriate factory for a particular protocol
        davRepository = SVNConnectorTestUtil.createRepository(url, username, password);
        sourceName = "the source name";
        connection = new SVNRepositoryConnection(sourceName, policy, Boolean.FALSE, davRepository);
        // And create the graph ...
        graph = Graph.create(connection, context);
    }

    @After
    public void afterEach() {
        try {
            if (connection != null) connection.close();
        } finally {

        }
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfSourceNameIsNull() {
        sourceName = null;
        connection = new SVNRepositoryConnection(sourceName, policy, Boolean.FALSE, davRepository);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfRepositoryIsNull() {
        davRepository = null;
        connection = new SVNRepositoryConnection(sourceName, policy, Boolean.FALSE, davRepository);
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

    // @Test
    // public void shouldGetTheSVNRepositoryRootFromTheSVNRepositoryWhenPinged() throws Exception {
    // CachePolicy policy = mock(CachePolicy.class);
    // davRepository = mock(DAVRepository.class);
    // connection = new SVNRepositoryConnection("the source name", policy, SVNRepositorySource.DEFAULT_UUID_PROPERTY_NAME,
    // davRepository);
    // stub(davRepository.getRepositoryRoot(true)).toReturn(null);
    // assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
    // verify(davRepository).getRepositoryRoot(true);
    // }

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

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToGetChildrenFromAWrongRequestedPath() {
        List<Location> l = graph.getChildren().of(pathFactory.create("wrongRequestedPath"));
    }

    @Test
    public void shouldReturnTheContentNodePathOfTheFile() {
        List<Location> locations00 = graph.getChildren().of(pathFactory.create("/trunk/extensions/dna-connector-svn/src/test/resources/nodeA/itemA1.txt"));
        assertThat(locations00.isEmpty(), is(false));
        assertThat(containsPaths(locations00).contains("/trunk/extensions/dna-connector-svn/src/test/resources/nodeA/itemA1.txt/jcr:content"),
                   is(true));

    }

    @Test
    public void shouldListLocationForChildrenOfAParentPath() {

        // read children from the root node.
        List<Location> l = graph.getChildren().of(pathFactory.create("/"));
        assertThat(containsPaths(l).contains("/trunk"), is(true));
        assertThat(containsPaths(l).contains("/branches"), is(true));
        assertThat(containsPaths(l).contains("/tags"), is(true));

        List<Location> locations02 = graph.getChildren().of(pathFactory.create("/trunk/extensions/dna-connector-svn/src/test/resources/nodeA"));
        assertThat(locations02.size() > 0, is(true));
        assertThat(containsPaths(locations02).contains("/trunk/extensions/dna-connector-svn/src/test/resources/nodeA/itemA1.txt/jcr:content"),
                   is(true));
        assertThat(containsPaths(locations02).contains("/trunk/extensions/dna-connector-svn/src/test/resources/nodeA/itemA2.txt/jcr:content"),
                   is(true));

        List<Location> locations03 = graph.getChildren().of(pathFactory.create("/trunk/extensions/dna-connector-svn/src/test/resources/nodeB"));
        assertThat(locations03.size() > 0, is(true));
        assertThat(containsPaths(locations03).contains("/trunk/extensions/dna-connector-svn/src/test/resources/nodeB/JBossORG-EULA.txt/jcr:content"),
                   is(true));
        assertThat(containsPaths(locations03).contains("/trunk/extensions/dna-connector-svn/src/test/resources/nodeB/nodeB1"),
                   is(true));
    }

    protected Collection<String> containsPaths( Collection<Location> locations ) {
        List<String> paths = new ArrayList<String>();
        for (Location location : locations) {
            paths.add(location.getPath().getString(context.getNamespaceRegistry(), new UrlEncoder()));
        }
        return paths;
    }
}
