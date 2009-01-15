/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.jbosscache;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceListener;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class JBossCacheConnectionTest {

    private JBossCacheConnection connection;
    private CacheFactory<Name, Object> cacheFactory;
    private Cache<Name, Object> cache;
    private ExecutionContext context;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private PropertyFactory propertyFactory;
    private Graph graph;
    @Mock
    private JBossCacheSource source;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        pathFactory = context.getValueFactories().getPathFactory();
        propertyFactory = context.getPropertyFactory();
        nameFactory = context.getValueFactories().getNameFactory();
        cacheFactory = new DefaultCacheFactory<Name, Object>();
        cache = cacheFactory.createCache();
        connection = new JBossCacheConnection(source, cache);
        String sourceName = "the source name";
        stub(source.getUuidPropertyName()).toReturn(DnaLexicon.UUID.getString(context.getNamespaceRegistry()));
        stub(source.getName()).toReturn(sourceName);
        stub(connectionFactory.createConnection(sourceName)).toReturn(connection);
        graph = Graph.create(sourceName, connectionFactory, context);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfCacheReferenceIsNull() {
        cache = null;
        connection = new JBossCacheConnection(source, cache);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfSourceReferenceIsNull() {
        source = null;
        connection = new JBossCacheConnection(source, cache);
    }

    @Test
    public void shouldInstantiateWithValidSourceAndCacheReferences() {
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsSourceName() {
        stub(source.getName()).toReturn("the source name");
        assertThat(connection.getSourceName(), is("the source name"));
        verify(source).getName();
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsDefaultCachePolicy() {
        CachePolicy policy = mock(CachePolicy.class);
        stub(source.getDefaultCachePolicy()).toReturn(policy);
        assertThat(connection.getDefaultCachePolicy(), is(sameInstance(policy)));
        verify(source).getDefaultCachePolicy();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldGetTheRootFromTheCacheWhenPinged() {
        cache = mock(Cache.class);
        connection = new JBossCacheConnection(source, cache);
        stub(cache.getRoot()).toReturn(null);
        assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
        verify(cache).getRoot();
    }

    @Test
    public void shouldHaveNoOpListenerWhenCreated() {
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldUseNoOpListenerWhenSettingListenerToNull() {
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldSetListenerToNonNullValue() {
        RepositorySourceListener listener = mock(RepositorySourceListener.class);
        connection.setListener(listener);
        assertThat(connection.getListener(), is(sameInstance(listener)));
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldGenerateUuid() {
        for (int i = 0; i != 100; ++i) {
            assertThat(connection.generateUuid(), is(notNullValue()));
        }
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromPath() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<?> fqn = connection.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(4));
        assertThat(fqn.isRoot(), is(false));
        for (int i = 0; i != path.size(); ++i) {
            assertThat((Path.Segment)fqn.get(i), is(path.getSegment(i)));
        }
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromRootPath() {
        Path path = pathFactory.createRootPath();
        Fqn<?> fqn = connection.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(0));
        assertThat(fqn.isRoot(), is(true));
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToCreateFullyQualifiedNodeFromNullPath() {
        connection.getFullyQualifiedName((Path)null);
    }

    @Test
    public void shouldCreateFullyQualifiedNodeFromPathSegment() {
        Path.Segment segment = pathFactory.createSegment("a");
        Fqn<?> fqn = connection.getFullyQualifiedName(segment);
        assertThat(fqn.size(), is(1));
        assertThat(fqn.isRoot(), is(false));
        assertThat((Path.Segment)fqn.get(0), is(segment));
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToCreateFullyQualifiedNodeFromNullPathSegment() {
        connection.getFullyQualifiedName((Path.Segment)null);
    }

    @Test
    public void shouldCreatePathFromFullyQualifiedNode() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<?> fqn = connection.getFullyQualifiedName(path);
        assertThat(connection.getPath(pathFactory, fqn), is(path));
    }

    @Test
    public void shouldCreateRootPathFromRootFullyQualifiedNode() {
        Path path = pathFactory.createRootPath();
        Fqn<?> fqn = connection.getFullyQualifiedName(path);
        assertThat(connection.getPath(pathFactory, fqn), is(path));
    }

    @Test
    public void shouldGetNodeIfItExistsInCache() {
        // Set up the cache with data ...
        Name uuidProperty = DnaLexicon.UUID;
        Path[] paths = {pathFactory.create("/a"), pathFactory.create("/a/b"), pathFactory.create("/a/b/c")};
        Path nonExistantPath = pathFactory.create("/a/d");
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        cache.put(Fqn.fromList(paths[0].getSegmentsList()), uuidProperty, uuids[0]);
        cache.put(Fqn.fromList(paths[1].getSegmentsList()), uuidProperty, uuids[1]);
        cache.put(Fqn.fromList(paths[2].getSegmentsList()), uuidProperty, uuids[2]);
        Node<Name, Object> nodeA = cache.getNode(Fqn.fromList(paths[0].getSegmentsList()));
        Node<Name, Object> nodeB = cache.getNode(Fqn.fromList(paths[1].getSegmentsList()));
        Node<Name, Object> nodeC = cache.getNode(Fqn.fromList(paths[2].getSegmentsList()));
        Node<Name, Object> nodeD = cache.getNode(Fqn.fromList(nonExistantPath.getSegmentsList()));
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeD, is(nullValue()));
        // Test the getNode(...) method for existing nodes ...
        assertThat(connection.getNode(context, paths[0]), is(sameInstance(nodeA)));
        assertThat(connection.getNode(context, paths[1]), is(sameInstance(nodeB)));
        assertThat(connection.getNode(context, paths[2]), is(sameInstance(nodeC)));
    }

    @Test
    public void shouldThrowExceptionWithLowestExistingNodeFromGetNodeIfTheNodeDoesNotExist() {
        // Set up the cache with data ...
        Name uuidProperty = DnaLexicon.UUID;
        Path[] paths = {pathFactory.create("/a"), pathFactory.create("/a/b"), pathFactory.create("/a/b/c")};
        Path nonExistantPath = pathFactory.create("/a/d");
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        cache.put(Fqn.fromList(paths[0].getSegmentsList()), uuidProperty, uuids[0]);
        cache.put(Fqn.fromList(paths[1].getSegmentsList()), uuidProperty, uuids[1]);
        cache.put(Fqn.fromList(paths[2].getSegmentsList()), uuidProperty, uuids[2]);
        Node<Name, Object> nodeA = cache.getNode(Fqn.fromList(paths[0].getSegmentsList()));
        Node<Name, Object> nodeB = cache.getNode(Fqn.fromList(paths[1].getSegmentsList()));
        Node<Name, Object> nodeC = cache.getNode(Fqn.fromList(paths[2].getSegmentsList()));
        Node<Name, Object> nodeD = cache.getNode(Fqn.fromList(nonExistantPath.getSegmentsList()));
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeD, is(nullValue()));
        try {
            connection.getNode(context, nonExistantPath);
            fail();
        } catch (PathNotFoundException e) {
            assertThat(e.getLowestAncestorThatDoesExist(), is(paths[0]));
        }
    }

    @Test
    public void shouldCopyNode() {
        // Set up the cache with data ...
        Name uuidProperty = DnaLexicon.UUID;
        Path[] paths = {pathFactory.create("/a"), pathFactory.create("/a/b"), pathFactory.create("/a/b/c"),
            pathFactory.create("/a/d")};
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        cache.put(Fqn.fromList(paths[0].getSegmentsList()), uuidProperty, uuids[0]);
        cache.put(Fqn.fromList(paths[1].getSegmentsList()), uuidProperty, uuids[1]);
        cache.put(Fqn.fromList(paths[2].getSegmentsList()), uuidProperty, uuids[2]);
        cache.put(Fqn.fromList(paths[3].getSegmentsList()), uuidProperty, uuids[3]);
        Node<Name, Object> nodeA = cache.getNode(Fqn.fromList(paths[0].getSegmentsList()));
        Node<Name, Object> nodeB = cache.getNode(Fqn.fromList(paths[1].getSegmentsList()));
        Node<Name, Object> nodeC = cache.getNode(Fqn.fromList(paths[2].getSegmentsList()));
        Node<Name, Object> nodeD = cache.getNode(Fqn.fromList(paths[3].getSegmentsList()));
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeD, is(notNullValue()));
        assertThat(nodeA.get(uuidProperty), is((Object)uuids[0]));
        assertThat(nodeB.get(uuidProperty), is((Object)uuids[1]));
        assertThat(nodeC.get(uuidProperty), is((Object)uuids[2]));
        assertThat(nodeD.get(uuidProperty), is((Object)uuids[3]));
        // Make sure the new nodes doesn't exist
        Path newPathB = pathFactory.create("/a/d/b");
        Path newPathC = pathFactory.create("/a/d/b/c");
        Node<Name, Object> newNodeB = cache.getNode(Fqn.fromList(newPathB.getSegmentsList()));
        Node<Name, Object> newNodeC = cache.getNode(Fqn.fromList(newPathC.getSegmentsList()));
        assertThat(newNodeB, is(nullValue()));
        assertThat(newNodeC, is(nullValue()));
        // Copy node B and place under node D
        AtomicInteger count = new AtomicInteger();
        connection.copyNode(nodeB, nodeD, true, uuidProperty, count, context);
        assertThat(count.get(), is(2));
        newNodeB = cache.getNode(Fqn.fromList(newPathB.getSegmentsList()));
        newNodeC = cache.getNode(Fqn.fromList(newPathC.getSegmentsList()));
        assertThat(newNodeB, is(notNullValue()));
        assertThat(newNodeC, is(notNullValue()));
        // Make sure the UUIDs are new ...
        assertThat(newNodeB.get(uuidProperty), is(not(nodeB.get(uuidProperty))));
        assertThat(newNodeC.get(uuidProperty), is(not(nodeC.get(uuidProperty))));
    }

    @Test
    public void shouldCreateSameNameSiblingsAndAutomaticallyManageSiblingIndexes() throws Exception {
        // Set up the cache with some data, using different execute calls ...
        Property prop1 = propertyFactory.create(nameFactory.create("dna:prop1"), "value1");
        Property prop2 = propertyFactory.create(nameFactory.create("dna:prop2"), "value1");
        graph.create("/a", prop1, prop2);
        for (int i = 0; i != 20; ++i) {
            graph.create("/a/b", prop1, prop2);
        }

        // Now verify the content ...
        org.jboss.dna.graph.Node aNode = graph.getNodeAt("/a");
        assertThat(aNode, is(notNullValue()));
        assertThat(aNode.getPropertiesByName().get(prop1.getName()), is(prop1));
        assertThat(aNode.getPropertiesByName().get(prop2.getName()), is(prop2));
        assertThat(aNode.getChildren().size(), is(20));
        int index = 1;
        for (Location childLocation : aNode.getChildren()) {
            Path.Segment segment = childLocation.getPath().getLastSegment();
            assertThat(segment.getName().getLocalName(), is("b"));
            assertThat(segment.hasIndex(), is(true));
            assertThat(segment.getIndex(), is(index));
            ++index;
        }
    }

    @Test
    public void shouldCreateSameNameSiblingsAndAutomaticallyManageSiblingIndexesInterspersedWithSiblingsWithOtherNames()
        throws Exception {
        // Set up the cache with some data, using different execute calls ...
        Property prop1 = propertyFactory.create(nameFactory.create("dna:prop1"), "value1");
        Property prop2 = propertyFactory.create(nameFactory.create("dna:prop2"), "value1");
        graph.create("/a", prop1, prop2);
        for (int i = 0; i != 20; ++i) {
            String path = i % 5 == 0 ? "/a/b" : "/a/c" + i;
            graph.create(path, prop1, prop2);
        }

        // Now verify the content ...
        org.jboss.dna.graph.Node aNode = graph.getNodeAt("/a");
        assertThat(aNode, is(notNullValue()));
        assertThat(aNode.getPropertiesByName().get(prop1.getName()), is(prop1));
        assertThat(aNode.getPropertiesByName().get(prop2.getName()), is(prop2));
        assertThat(aNode.getChildren().size(), is(20));
        int index = 1;
        for (Location childLocation : aNode.getChildren()) {
            Path.Segment segment = childLocation.getPath().getLastSegment();
            if (segment.getName().getLocalName().equals("b")) {
                assertThat(segment.getName().getLocalName(), is("b"));
                assertThat(segment.hasIndex(), is(true));
                assertThat(segment.getIndex(), is(index));
                ++index;
            } else {
                assertThat(segment.getName().getLocalName().startsWith("c"), is(true));
                assertThat(segment.hasIndex(), is(false));
            }
        }
    }
}
