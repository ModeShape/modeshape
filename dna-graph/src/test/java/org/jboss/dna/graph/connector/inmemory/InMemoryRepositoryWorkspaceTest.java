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
package org.jboss.dna.graph.connector.inmemory;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepository.Workspace;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.basic.BasicPropertyFactory;
import org.jboss.dna.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class InMemoryRepositoryWorkspaceTest {

    private InMemoryRepository repository;
    private String repositoryName;
    private UUID rootUuid;
    private String workspaceName;
    private InMemoryRepository.Workspace workspace;

    private ExecutionContext context;
    private ValueFactories valueFactories;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private PropertyFactory propertyFactory;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        valueFactories = context.getValueFactories();
        pathFactory = valueFactories.getPathFactory();
        nameFactory = valueFactories.getNameFactory();
        propertyFactory = new BasicPropertyFactory(valueFactories);
        repositoryName = "Test repository";
        rootUuid = UUID.randomUUID();
        repository = new InMemoryRepository(repositoryName, rootUuid);
        workspaceName = "My Workspace";
        workspace = repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE);
    }

    @Test
    public void shouldHaveRootNodeAfterInstantiating() {
        assertThat(workspace.getRoot(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNameAfterInstantiating() {
        assertThat(workspace.getName(), is(workspaceName));
    }

    @Test
    public void shouldHaveRootNodeWithRootUuid() {
        assertThat(workspace.getRoot().getUuid(), is(rootUuid));
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowRootToBeRemoved() {
        workspace.removeNode(context, workspace.getRoot());
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNodeToBeRemovedUsingNullEnvironment() {
        InMemoryNode node = mock(InMemoryNode.class);
        workspace.removeNode(null, node);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullNodeToBeRemoved() {
        workspace.removeNode(context, null);
    }

    @Test
    public void shouldCreateNodesByPath() {
        Name name_a = nameFactory.create("a");
        InMemoryNode node_a = workspace.createNode(context, workspace.getRoot(), name_a, null);
        assertThat(node_a, is(notNullValue()));
        assertThat(node_a.getParent(), is(workspace.getRoot()));
        assertThat(node_a.getName().getName(), is(name_a));
        assertThat(node_a.getName().hasIndex(), is(false));

        Name name_b = nameFactory.create("b");
        InMemoryNode node_b = workspace.createNode(context, node_a, name_b, null);
        assertThat(node_b, is(notNullValue()));
        assertThat(node_b.getParent(), is(node_a));
        assertThat(node_b.getName().getName(), is(name_b));
        assertThat(node_b.getName().hasIndex(), is(false));

        Name name_c = nameFactory.create("c");
        InMemoryNode node_c = workspace.createNode(context, node_b, name_c, null);
        assertThat(node_c, is(notNullValue()));
        assertThat(node_c.getParent(), is(node_b));
        assertThat(node_c.getName().getName(), is(name_c));
        assertThat(node_c.getName().hasIndex(), is(false));

        assertThat(workspace.getNodesByUuid().size(), is(4));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
    }

    @Test
    public void shouldNotFindNodesThatDoNotExist() {
        InMemoryNode node_a = workspace.createNode(context, workspace.getRoot(), nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        /*Node node_c =*/workspace.createNode(context, node_b, nameFactory.create("c"), null);

        assertThat(workspace.getNodesByUuid().size(), is(4));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(node_a));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(node_b));
        assertThat(workspace.getNode(pathFactory.create("/a[1]")), is(node_a));
        assertThat(workspace.getNode(pathFactory.create("/a/b[1]")), is(node_b));
        assertThat(workspace.getNode(pathFactory.create("/a[1]/b[1]")), is(node_b));
        assertThat(workspace.getNode(pathFactory.create("/a[2]")), is(nullValue()));
        assertThat(workspace.getNode(pathFactory.create("/b[2]")), is(nullValue()));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(nullValue()));
    }

    @Test
    public void shouldCorrectlyManageIndexesOfSiblingsWithSameNames() {
        Name name_a1 = nameFactory.create("a");
        InMemoryNode node_a1 = workspace.createNode(context, workspace.getRoot(), name_a1, null);
        assertThat(node_a1, is(notNullValue()));
        assertThat(node_a1.getParent(), is(workspace.getRoot()));
        assertThat(node_a1.getName().getName(), is(name_a1));
        assertThat(node_a1.getName().hasIndex(), is(false));

        Name name_a2 = nameFactory.create("a");
        InMemoryNode node_a2 = workspace.createNode(context, workspace.getRoot(), name_a2, null);
        assertThat(node_a2, is(notNullValue()));
        assertThat(node_a2.getParent(), is(workspace.getRoot()));
        assertThat(node_a2.getName().getName(), is(name_a2));
        assertThat(node_a2.getName().hasIndex(), is(true));
        assertThat(node_a2.getName().getIndex(), is(2));

        // node 1 should now have an index ...
        assertThat(node_a1.getName().getIndex(), is(1));

        // Add another node without the same name ...
        Name name_b = nameFactory.create("b");
        InMemoryNode node_b = workspace.createNode(context, workspace.getRoot(), name_b, null);
        assertThat(node_b, is(notNullValue()));
        assertThat(node_b.getParent(), is(workspace.getRoot()));
        assertThat(node_b.getName().getName(), is(name_b));
        assertThat(node_b.getName().hasIndex(), is(false));

        // Add a third node with the same name ...
        Name name_a3 = nameFactory.create("a");
        InMemoryNode node_a3 = workspace.createNode(context, workspace.getRoot(), name_a3, null);
        assertThat(node_a3, is(notNullValue()));
        assertThat(node_a3.getParent(), is(workspace.getRoot()));
        assertThat(node_a3.getName().getName(), is(name_a3));
        assertThat(node_a3.getName().hasIndex(), is(true));
        assertThat(node_a3.getName().getIndex(), is(3));

        // Check the number of children ...
        assertThat(workspace.getRoot().getChildren().size(), is(4));
        assertThat(workspace.getRoot().getChildren(), hasItems(node_a1, node_a2, node_b, node_a3));
        assertThat(workspace.getNodesByUuid().size(), is(5));
        assertThat(workspace.getNode(pathFactory.create("/a[1]")), is(sameInstance(node_a1)));
        assertThat(workspace.getNode(pathFactory.create("/a[2]")), is(sameInstance(node_a2)));
        assertThat(workspace.getNode(pathFactory.create("/a[3]")), is(sameInstance(node_a3)));
        assertThat(workspace.getNode(pathFactory.create("/b")), is(sameInstance(node_b)));

        // Removing a node with the same name will reduce the index ...
        workspace.removeNode(context, node_a2);
        assertThat(workspace.getRoot().getChildren().size(), is(3));
        assertThat(workspace.getRoot().getChildren(), hasItems(node_a1, node_b, node_a3));
        assertThat(node_a1.getName().getIndex(), is(1));
        assertThat(node_b.getName().hasIndex(), is(false));
        assertThat(node_a3.getName().getIndex(), is(2));

        // Removing a node with the same name will reduce the index ...
        workspace.removeNode(context, node_a1);
        assertThat(workspace.getRoot().getChildren().size(), is(2));
        assertThat(workspace.getRoot().getChildren(), hasItems(node_b, node_a3));
        assertThat(node_b.getName().hasIndex(), is(false));
        assertThat(node_a3.getName().hasIndex(), is(false));
        assertThat(workspace.getNodesByUuid().size(), is(3));
    }

    @Test
    public void shouldMoveNodesWithinSameWorkspace() {
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        workspace.moveNode(context, node_b, workspace, node_d);

        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/d/b[2]")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/d/b[2]/c")), is(sameInstance(node_c)));

        workspace.moveNode(context, node_b, workspace, node_e);

        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/e/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/d/e/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
    }

    @Test
    public void shouldMoveNodesFromOneWorkspaceToAnother() {
        // Populate the workspace with some content ...
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        // Create the second workspace and populate it with some content ...
        Workspace new_workspace = repository.createWorkspace(context, "Second Workspace", CreateConflictBehavior.DO_NOT_CREATE);
        assertThat(new_workspace, is(notNullValue()));

        InMemoryNode new_root = new_workspace.getRoot();
        InMemoryNode new_node_a = new_workspace.createNode(context, new_root, nameFactory.create("a"), null);
        InMemoryNode new_node_b = new_workspace.createNode(context, new_node_a, nameFactory.create("b"), null);
        InMemoryNode new_node_c = new_workspace.createNode(context, new_node_b, nameFactory.create("c"), null);
        InMemoryNode new_node_d = new_workspace.createNode(context, new_root, nameFactory.create("d"), null);
        InMemoryNode new_node_e = new_workspace.createNode(context, new_node_d, nameFactory.create("e"), null);
        InMemoryNode new_node_b2 = new_workspace.createNode(context, new_node_d, nameFactory.create("b"), null);

        assertThat(new_workspace.getNodesByUuid().size(), is(7));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(new_node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(new_node_c)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(new_node_b2)));

        // Move 'workspace::/a/b' into 'newWorkspace::/d'
        workspace.moveNode(context, node_b, new_workspace, new_node_d);

        assertThat(workspace.getNodesByUuid().size(), is(5));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(new_workspace.getNodesByUuid().size(), is(9));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(new_node_b2)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]")), is(sameInstance(node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]/c")), is(sameInstance(node_c)));
    }

    @Test
    public void shouldCopyNodesWithinSameWorkspace() {
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.getProperties().put(propertyName, property);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        workspace.copyNode(context, node_b, workspace, node_d, null, true, new HashMap<UUID, UUID>());

        assertThat(workspace.getNodesByUuid().size(), is(9));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/d/b[2]")), is(notNullValue()));
        assertThat(workspace.getNode(pathFactory.create("/d/b[2]/c")), is(notNullValue()));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));
        assertThat(workspace.getNode(pathFactory.create("/d/b[2]")).getProperties().get(propertyName), is(property));
    }

    @Test
    public void shouldCopyNodesFromOneWorkspaceToAnotherAndKeepSameUuids() {
        // Populate the workspace with some content ...
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.getProperties().put(propertyName, property);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        // Create the second workspace and populate it with some content ...
        Workspace new_workspace = repository.createWorkspace(context, "Second Workspace", CreateConflictBehavior.DO_NOT_CREATE);
        assertThat(new_workspace, is(notNullValue()));

        InMemoryNode new_root = new_workspace.getRoot();
        InMemoryNode new_node_a = new_workspace.createNode(context, new_root, nameFactory.create("a"), null);
        InMemoryNode new_node_b = new_workspace.createNode(context, new_node_a, nameFactory.create("b"), null);
        InMemoryNode new_node_c = new_workspace.createNode(context, new_node_b, nameFactory.create("c"), null);
        InMemoryNode new_node_d = new_workspace.createNode(context, new_root, nameFactory.create("d"), null);
        InMemoryNode new_node_e = new_workspace.createNode(context, new_node_d, nameFactory.create("e"), null);
        InMemoryNode new_node_b2 = new_workspace.createNode(context, new_node_d, nameFactory.create("b"), null);

        assertThat(new_workspace.getNodesByUuid().size(), is(7));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(new_node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(new_node_c)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(new_node_b2)));

        // Copy 'workspace::/a/b' into 'newWorkspace::/d'
        workspace.copyNode(context, node_b, new_workspace, new_node_d, null, true, null);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        assertThat(new_workspace.getNodesByUuid().size(), is(9));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(new_node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(new_node_c)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(new_node_b2)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]")), is(notNullValue()));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]/c")), is(notNullValue()));

        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]")).getProperties().get(propertyName), is(property));

        // The new copy should have the same UUIDs as in the original, since we specified no UUID map ...
        InMemoryNode new_copy_b = new_workspace.getNode(pathFactory.create("/d/b[2]"));
        InMemoryNode new_copy_c = new_workspace.getNode(pathFactory.create("/d/b[2]/c"));
        assertThat(new_copy_b, is(notNullValue()));
        assertThat(new_copy_c, is(notNullValue()));
        assertThat(new_copy_b.getUuid(), is(node_b.getUuid()));
        assertThat(new_copy_c.getUuid(), is(node_c.getUuid()));
    }

    @Test
    public void shouldCopyNodesFromOneWorkspaceToAnotherAndGenerateNewUuids() {
        // Populate the workspace with some content ...
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.getProperties().put(propertyName, property);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        // Create the second workspace and populate it with some content ...
        Workspace new_workspace = repository.createWorkspace(context, "Second Workspace", CreateConflictBehavior.DO_NOT_CREATE);
        assertThat(new_workspace, is(notNullValue()));

        InMemoryNode new_root = new_workspace.getRoot();
        InMemoryNode new_node_a = new_workspace.createNode(context, new_root, nameFactory.create("a"), null);
        InMemoryNode new_node_b = new_workspace.createNode(context, new_node_a, nameFactory.create("b"), null);
        InMemoryNode new_node_c = new_workspace.createNode(context, new_node_b, nameFactory.create("c"), null);
        InMemoryNode new_node_d = new_workspace.createNode(context, new_root, nameFactory.create("d"), null);
        InMemoryNode new_node_e = new_workspace.createNode(context, new_node_d, nameFactory.create("e"), null);
        InMemoryNode new_node_b2 = new_workspace.createNode(context, new_node_d, nameFactory.create("b"), null);

        assertThat(new_workspace.getNodesByUuid().size(), is(7));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(new_node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(new_node_c)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(new_node_b2)));

        // Copy 'workspace::/a/b' into 'newWorkspace::/d'
        Map<UUID, UUID> oldToNewUuids = new HashMap<UUID, UUID>();
        workspace.copyNode(context, node_b, new_workspace, new_node_d, null, true, oldToNewUuids);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        assertThat(new_workspace.getNodesByUuid().size(), is(9));
        assertThat(new_workspace.getNode(pathFactory.create("/")), is(sameInstance(new_root)));
        assertThat(new_workspace.getNode(pathFactory.create("/a")), is(sameInstance(new_node_a)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(new_node_b)));
        assertThat(new_workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(new_node_c)));
        assertThat(new_workspace.getNode(pathFactory.create("/d")), is(sameInstance(new_node_d)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(new_node_e)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[1]")), is(sameInstance(new_node_b2)));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]")), is(notNullValue()));
        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]/c")), is(notNullValue()));

        assertThat(new_workspace.getNode(pathFactory.create("/d/b[2]")).getProperties().get(propertyName), is(property));

        // The new copy should have different UUIDs than in the original, since we did specify a UUID map ...
        InMemoryNode new_copy_b = new_workspace.getNode(pathFactory.create("/d/b[2]"));
        InMemoryNode new_copy_c = new_workspace.getNode(pathFactory.create("/d/b[2]/c"));
        assertThat(new_copy_b, is(notNullValue()));
        assertThat(new_copy_c, is(notNullValue()));
        assertThat(new_copy_b.getUuid(), is(not(node_b.getUuid())));
        assertThat(new_copy_c.getUuid(), is(not(node_c.getUuid())));
        assertThat(new_copy_b.getUuid(), is(oldToNewUuids.get(node_b.getUuid())));
        assertThat(new_copy_c.getUuid(), is(oldToNewUuids.get(node_c.getUuid())));
    }

    @Test
    public void shouldCopyNodesWhenDesiredNameIsSpecified() {
        InMemoryNode root = workspace.getRoot();
        InMemoryNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        InMemoryNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        InMemoryNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        InMemoryNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        InMemoryNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        InMemoryNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.getProperties().put(propertyName, property);

        assertThat(workspace.getNodesByUuid().size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));

        workspace.copyNode(context, node_b, workspace, node_d, nameFactory.create("x"), true, new HashMap<UUID, UUID>());

        assertThat(workspace.getNodesByUuid().size(), is(9));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/d/x")), is(notNullValue()));
        assertThat(workspace.getNode(pathFactory.create("/d/x/c")), is(notNullValue()));

        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperties().get(propertyName), is(property));
        assertThat(workspace.getNode(pathFactory.create("/d/x")).getProperties().get(propertyName), is(property));
    }

    @Test
    public void shouldCreateRepositoryStructure() {
        workspace.createNode(context, "/a").setProperty(context, "name", "value").setProperty(context, "desc", "Some description");
        workspace.createNode(context, "/a/b").setProperty(context, "name", "value2").setProperty(context,
                                                                                                 "desc",
                                                                                                 "Some description 2");
        assertThat(workspace.getNode(context, "/a").getProperty(context, "name").getValuesAsArray(), is(new Object[] {"value"}));
        assertThat(workspace.getNode(context, "/a").getProperty(context, "desc").getValuesAsArray(),
                   is(new Object[] {"Some description"}));
        assertThat(workspace.getNode(context, "/a/b").getProperty(context, "name").getValuesAsArray(),
                   is(new Object[] {"value2"}));
        assertThat(workspace.getNode(context, "/a/b").getProperty(context, "desc").getValuesAsArray(),
                   is(new Object[] {"Some description 2"}));
    }
}
