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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapWorkspace;
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
public class InMemoryRepositoryTest {

    private InMemoryRepository repository;
    private String repositoryName;
    private UUID rootUuid;

    private ExecutionContext context;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private PropertyFactory propertyFactory;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        ValueFactories valueFactories = context.getValueFactories();
        pathFactory = valueFactories.getPathFactory();
        nameFactory = valueFactories.getNameFactory();
        propertyFactory = new BasicPropertyFactory(valueFactories);
        repositoryName = "Test repository";
        rootUuid = UUID.randomUUID();
        repository = new InMemoryRepository(repositoryName, rootUuid);
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
    public void shouldNotCreateWorkspaceIfNameIsAlreadyUsedAndConflictOptionIsToNotCreate() {
        String workspaceName = "New Workspace";
        assertThat(repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE), is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName));
        assertThat(repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE), is(nullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName));
    }

    @Test
    public void shouldCreateWorkspaceWithUniqueNameIfSpecifiedNameIsAlreadyUsedAndConflictOptionIsToCreateWithAdjustedName() {
        String workspaceName = "New Workspace";
        assertThat(repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE), is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName));
        MapWorkspace secondWorkspace = repository.createWorkspace(context,
                                                               workspaceName,
                                                               CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        assertThat(secondWorkspace, is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName, secondWorkspace.getName()));
    }

    @Test
    public void shouldNotDestroyWorkspaceIfNameDoesNotMatchExistingWorkspace() {
        String workspaceName = "New Workspace";
        assertThat(repository.getWorkspaceNames().contains(workspaceName), is(false));
        assertThat(repository.destroyWorkspace(workspaceName), is(false));
    }

    @Test
    public void shouldDestroyWorkspaceIfNameMatchesExistingWorkspace() {
        String workspaceName = "New Workspace";
        assertThat(repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE), is(notNullValue()));
        assertThat(repository.getWorkspaceNames().contains(workspaceName), is(true));
        assertThat(repository.destroyWorkspace(workspaceName), is(true));
    }

    @Test
    public void shouldCloneWorkspaceAndCopyContentsIfWorkspaceWithSpecifiedNameExists() {
        String workspaceName = "Original Workspace";
        MapWorkspace workspace = repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE);
        assertThat(workspace, is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName));

        // Populate the workspace with a few nodes ...
        MapNode root = workspace.getRoot();
        MapNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        MapNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        MapNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        MapNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        MapNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        MapNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.setProperty(property);

        assertThat(((InMemoryRepository.Workspace) workspace).size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperty(propertyName), is(property));

        // Now clone the workspace ...
        String newWorkspaceName = "New Workspace";
        MapWorkspace new_workspace = repository.createWorkspace(context,
                                                             newWorkspaceName,
                                                             CreateConflictBehavior.DO_NOT_CREATE,
                                                             workspaceName);
        assertThat(new_workspace, is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName, newWorkspaceName));

        // Now check that the original workspace still has its content ...
        assertThat(((InMemoryRepository.Workspace) workspace).size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperty(propertyName), is(property));

        // Now check that the new workspace has its content ...
        assertThat(((InMemoryRepository.Workspace) new_workspace).size(), is(7));

        // Since we cloned workspaces, the UUIDs should be the same in each workspace ...
        assertThat(workspace.getNode(pathFactory.create("/")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/a")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/a")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/a/b")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/a/b")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/a/b/c")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/d")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/d")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/d/e")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/d/e")).getUuid()));
        assertThat(workspace.getNode(pathFactory.create("/d/b")).getUuid(),
                   is(new_workspace.getNode(pathFactory.create("/d/b")).getUuid()));
    }

    @Test
    public void shouldCloneWorkspaceButShouldNotCopyContentsIfWorkspaceWithSpecifiedNameDoesNotExist() {
        String workspaceName = "Original Workspace";
        MapWorkspace workspace = repository.createWorkspace(context, workspaceName, CreateConflictBehavior.DO_NOT_CREATE);
        assertThat(workspace, is(notNullValue()));
        assertThat(repository.getWorkspaceNames(), hasItems(workspaceName));

        // Populate the workspace with a few nodes ...
        MapNode root = workspace.getRoot();
        MapNode node_a = workspace.createNode(context, root, nameFactory.create("a"), null);
        MapNode node_b = workspace.createNode(context, node_a, nameFactory.create("b"), null);
        MapNode node_c = workspace.createNode(context, node_b, nameFactory.create("c"), null);
        MapNode node_d = workspace.createNode(context, root, nameFactory.create("d"), null);
        MapNode node_e = workspace.createNode(context, node_d, nameFactory.create("e"), null);
        MapNode node_b2 = workspace.createNode(context, node_d, nameFactory.create("b"), null);

        ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
        Name propertyName = nameFactory.create("something");
        Property property = propertyFactory.create(propertyName, stringFactory.create("Worth the wait"));
        node_b.setProperty(property);

        assertThat(((InMemoryRepository.Workspace) workspace).size(), is(7));
        assertThat(workspace.getNode(pathFactory.create("/")), is(sameInstance(workspace.getRoot())));
        assertThat(workspace.getNode(pathFactory.create("/a")), is(sameInstance(node_a)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")), is(sameInstance(node_b)));
        assertThat(workspace.getNode(pathFactory.create("/a/b/c")), is(sameInstance(node_c)));
        assertThat(workspace.getNode(pathFactory.create("/d")), is(sameInstance(node_d)));
        assertThat(workspace.getNode(pathFactory.create("/d/e")), is(sameInstance(node_e)));
        assertThat(workspace.getNode(pathFactory.create("/d/b")), is(sameInstance(node_b2)));
        assertThat(workspace.getNode(pathFactory.create("/a/b")).getProperty(propertyName), is(property));

        // Now clone the workspace ...
        String newWorkspaceName = "New Workspace";
        MapWorkspace new_workspace = repository.createWorkspace(context,
                                                             newWorkspaceName,
                                                             CreateConflictBehavior.DO_NOT_CREATE,
                                                             "non-existant workspace");
        assertThat(new_workspace.getRoot(), is(notNullValue()));
        assertThat(new_workspace.getRoot().getUuid(), is(rootUuid));
        assertThat(new_workspace.getRoot().getChildren().isEmpty(), is(true));
    }

}
