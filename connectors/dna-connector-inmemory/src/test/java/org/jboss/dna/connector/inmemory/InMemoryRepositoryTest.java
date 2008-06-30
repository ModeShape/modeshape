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
package org.jboss.dna.connector.inmemory;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.impl.BasicNamespaceRegistry;
import org.jboss.dna.spi.graph.impl.BasicPropertyFactory;
import org.jboss.dna.spi.graph.impl.StandardValueFactories;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class InMemoryRepositoryTest {

    private InMemoryRepository repository;
    private String name;
    private UUID rootUuid;
    private ValueFactories valueFactories;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private PropertyFactory propertyFactory;
    @Mock
    private ExecutionEnvironment env;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        valueFactories = new StandardValueFactories(new BasicNamespaceRegistry());
        pathFactory = valueFactories.getPathFactory();
        nameFactory = valueFactories.getNameFactory();
        propertyFactory = new BasicPropertyFactory(valueFactories);
        name = "Test repository";
        rootUuid = UUID.randomUUID();
        repository = new InMemoryRepository(name, rootUuid);
        stub(env.getValueFactories()).toReturn(valueFactories);
        stub(env.getPropertyFactory()).toReturn(propertyFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameInConstructor() {
        new InMemoryRepository(null, rootUuid);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankNameInConstructor() {
        new InMemoryRepository("  \t  ", rootUuid);
    }

    @Test
    public void shouldHaveLock() {
        assertThat(repository.getLock(), is(notNullValue()));
    }

    @Test
    public void shouldHaveRootNodeAfterInstantiating() {
        assertThat(repository.getRoot(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNameAfterInstantiating() {
        assertThat(repository.getName(), is(name));
    }

    @Test
    public void shouldHaveRootNodeWithRootUuid() {
        assertThat(repository.getRoot().getUuid(), is(rootUuid));
    }

    @Test
    public void shouldGenerateUuids() {
        Set<UUID> uuids = new HashSet<UUID>();
        for (int i = 0; i != 100; ++i) {
            assertThat(uuids.add(repository.generateUuid()), is(true));
        }
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowRootToBeRemoved() {
        repository.removeNode(env, repository.getRoot());
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowRootToBeMoved() {
        Node node = mock(Node.class);
        repository.moveNode(env, repository.getRoot(), node);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNodeToBeMovedUsingNullEnvironment() {
        Node node = mock(Node.class);
        Node newParent = mock(Node.class);
        repository.moveNode(null, node, newParent);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullNodeToBeMoved() {
        Node newParent = mock(Node.class);
        repository.moveNode(env, null, newParent);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNodeToBeRemovedUsingNullEnvironment() {
        Node node = mock(Node.class);
        repository.removeNode(null, node);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullNodeToBeRemoved() {
        repository.removeNode(env, null);
    }

    @Test
    public void shouldCreateNodesByPath() {
        Name name_a = nameFactory.create("a");
        Node node_a = repository.createNode(env, repository.getRoot(), name_a);
        assertThat(node_a, is(notNullValue()));
        assertThat(node_a.getParent(), is(repository.getRoot()));
        assertThat(node_a.getName().getName(), is(name_a));
        assertThat(node_a.getName().hasIndex(), is(false));

        Name name_b = nameFactory.create("b");
        Node node_b = repository.createNode(env, node_a, name_b);
        assertThat(node_b, is(notNullValue()));
        assertThat(node_b.getParent(), is(node_a));
        assertThat(node_b.getName().getName(), is(name_b));
        assertThat(node_b.getName().hasIndex(), is(false));

        Name name_c = nameFactory.create("c");
        Node node_c = repository.createNode(env, node_b, name_c);
        assertThat(node_c, is(notNullValue()));
        assertThat(node_c.getParent(), is(node_b));
        assertThat(node_c.getName().getName(), is(name_c));
        assertThat(node_c.getName().hasIndex(), is(false));

        assertThat(repository.getNodesByUuid().size(), is(4));
        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
    }

    @Test
    public void shouldNotFindNodesThatDoNotExist() {
        Node node_a = repository.createNode(env, repository.getRoot(), nameFactory.create("a"));
        Node node_b = repository.createNode(env, node_a, nameFactory.create("b"));
        /*Node node_c =*/repository.createNode(env, node_b, nameFactory.create("c"));

        assertThat(repository.getNodesByUuid().size(), is(4));
        assertThat(repository.getNode(pathFactory.create("/a[1]")), is(nullValue()));
        assertThat(repository.getNode(pathFactory.create("/d")), is(nullValue()));
    }

    @Test
    public void shouldCorrectlyManageIndexesOfSiblingsWithSameNames() {
        Name name_a1 = nameFactory.create("a");
        Node node_a1 = repository.createNode(env, repository.getRoot(), name_a1);
        assertThat(node_a1, is(notNullValue()));
        assertThat(node_a1.getParent(), is(repository.getRoot()));
        assertThat(node_a1.getName().getName(), is(name_a1));
        assertThat(node_a1.getName().hasIndex(), is(false));

        Name name_a2 = nameFactory.create("a");
        Node node_a2 = repository.createNode(env, repository.getRoot(), name_a2);
        assertThat(node_a2, is(notNullValue()));
        assertThat(node_a2.getParent(), is(repository.getRoot()));
        assertThat(node_a2.getName().getName(), is(name_a2));
        assertThat(node_a2.getName().hasIndex(), is(true));
        assertThat(node_a2.getName().getIndex(), is(2));

        // node 1 should now have an index ...
        assertThat(node_a1.getName().getIndex(), is(1));

        // Add another node without the same name ...
        Name name_b = nameFactory.create("b");
        Node node_b = repository.createNode(env, repository.getRoot(), name_b);
        assertThat(node_b, is(notNullValue()));
        assertThat(node_b.getParent(), is(repository.getRoot()));
        assertThat(node_b.getName().getName(), is(name_b));
        assertThat(node_b.getName().hasIndex(), is(false));

        // Add a third node with the same name ...
        Name name_a3 = nameFactory.create("a");
        Node node_a3 = repository.createNode(env, repository.getRoot(), name_a3);
        assertThat(node_a3, is(notNullValue()));
        assertThat(node_a3.getParent(), is(repository.getRoot()));
        assertThat(node_a3.getName().getName(), is(name_a3));
        assertThat(node_a3.getName().hasIndex(), is(true));
        assertThat(node_a3.getName().getIndex(), is(3));

        // Check the number of children ...
        assertThat(repository.getRoot().getChildren().size(), is(4));
        assertThat(repository.getRoot().getChildren(), hasItems(node_a1, node_a2, node_b, node_a3));
        assertThat(repository.getNodesByUuid().size(), is(5));
        assertThat(repository.getNode(pathFactory.create("/a[1]")), is(sameInstance(node_a1)));
        assertThat(repository.getNode(pathFactory.create("/a[2]")), is(sameInstance(node_a2)));
        assertThat(repository.getNode(pathFactory.create("/a[3]")), is(sameInstance(node_a3)));
        assertThat(repository.getNode(pathFactory.create("/b")), is(sameInstance(node_b)));

        // Removing a node with the same name will reduce the index ...
        repository.removeNode(env, node_a2);
        assertThat(repository.getRoot().getChildren().size(), is(3));
        assertThat(repository.getRoot().getChildren(), hasItems(node_a1, node_b, node_a3));
        assertThat(node_a1.getName().getIndex(), is(1));
        assertThat(node_b.getName().hasIndex(), is(false));
        assertThat(node_a3.getName().getIndex(), is(2));

        // Removing a node with the same name will reduce the index ...
        repository.removeNode(env, node_a1);
        assertThat(repository.getRoot().getChildren().size(), is(2));
        assertThat(repository.getRoot().getChildren(), hasItems(node_b, node_a3));
        assertThat(node_b.getName().hasIndex(), is(false));
        assertThat(node_a3.getName().hasIndex(), is(false));
        assertThat(repository.getNodesByUuid().size(), is(3));
    }

    @Test
    public void shouldMoveNodes() {
        Node root = repository.getRoot();
        Node node_a = repository.createNode(env, root, nameFactory.create("a"));
        Node node_b = repository.createNode(env, node_a, nameFactory.create("b"));
        Node node_c = repository.createNode(env, node_b, nameFactory.create("c"));
        Node node_d = repository.createNode(env, root, nameFactory.create("d"));
        Node node_e = repository.createNode(env, node_d, nameFactory.create("e"));
        Node node_b2 = repository.createNode(env, node_d, nameFactory.create("b"));

        assertThat(repository.getNodesByUuid().size(), is(7));
        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(repository.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(repository.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(repository.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        repository.moveNode(env, node_b, node_d);

        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(repository.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(repository.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(node_b2)));
        assertThat(repository.getNode(pathFactory.create("/d/b[2]")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/d/b[2]/c")), is(sameInstance(node_c)));

        repository.moveNode(env, node_b, node_e);

        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(repository.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(repository.getNode(pathFactory.create("/d/e/b")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/d/e/b/c")), is(sameInstance(node_c)));
        assertThat(repository.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
    }

    @Test
    public void shouldCopyNodes() {
        Node root = repository.getRoot();
        Node node_a = repository.createNode(env, root, nameFactory.create("a"));
        Node node_b = repository.createNode(env, node_a, nameFactory.create("b"));
        Node node_c = repository.createNode(env, node_b, nameFactory.create("c"));
        Node node_d = repository.createNode(env, root, nameFactory.create("d"));
        Node node_e = repository.createNode(env, node_d, nameFactory.create("e"));
        Node node_b2 = repository.createNode(env, node_d, nameFactory.create("b"));

        ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.getProperties().put(propertyName, property);

        assertThat(repository.getNodesByUuid().size(), is(7));
        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(repository.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(repository.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(repository.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(repository.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        repository.copyNode(env, node_b, node_d, true);

        assertThat(repository.getNodesByUuid().size(), is(9));
        assertThat(repository.getNode(pathFactory.create("/")), is(sameInstance(repository.getRoot())));
        assertThat(repository.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(repository.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(repository.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(repository.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(repository.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(repository.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(node_b2)));
        assertThat(repository.getNode(pathFactory.create("/d/b[2]")), is(notNullValue()));
        assertThat(repository.getNode(pathFactory.create("/d/b[2]/c")), is(notNullValue()));

        assertThat(repository.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));
        assertThat(repository.getNode(pathFactory.create("/d/b[2]")).getProperties().get(propertyName), is(property));
    }

    @Test
    public void shouldCreateRepositoryStructure() {
        repository.createNode(env, "/a").setProperty(env, "name", "value").setProperty(env, "desc", "Some description");
        repository.createNode(env, "/a/b").setProperty(env, "name", "value2").setProperty(env, "desc", "Some description 2");
        assertThat(repository.getNode(env, "/a").getProperty(env, "name").getValuesAsArray(), is(new Object[] {"value"}));
        assertThat(repository.getNode(env, "/a").getProperty(env, "desc").getValuesAsArray(),
                   is(new Object[] {"Some description"}));
        assertThat(repository.getNode(env, "/a/b").getProperty(env, "name").getValuesAsArray(), is(new Object[] {"value2"}));
        assertThat(repository.getNode(env, "/a/b").getProperty(env, "desc").getValuesAsArray(),
                   is(new Object[] {"Some description 2"}));
    }
}
