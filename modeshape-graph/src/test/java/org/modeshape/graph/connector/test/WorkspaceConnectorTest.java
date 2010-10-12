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
package org.modeshape.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.ReadNodeRequest;

/**
 * A class that provides standard workspace operations tests for connectors. This class is designed to be extended for each
 * connector, and in each subclass the {@link #setUpSource()} method is defined to provide a valid {@link RepositorySource} for
 * the connector to be tested.
 * <p>
 * Since these tests do modify repository content, the repository is set up for each test, given each test a pristine repository
 * (as {@link #initializeContent(Graph) initialized} by the concrete test case class).
 * </p>
 */
public abstract class WorkspaceConnectorTest extends AbstractConnectorTest {

    /**
     * Method used by some tests when a series of valid workspace names are required when creating or cloning workspaces.
     * 
     * @return an array of valid names; never null
     */
    protected abstract String[] generateValidNamesForNewWorkspaces();

    /**
     * Method used by some tests when a series of invalid workspace names are required so that creating or cloning workspaces will
     * generate {@link InvalidWorkspaceException} errors.
     * 
     * @return an array of invalid names, or null if the connector doesn't care what workspace names are valid or invalid
     */
    protected abstract String[] generateInvalidNamesForNewWorkspaces();

    protected String generateNonExistantWorkspaceName() {
        String workspaceName = "something bogus" + context.getValueFactories().getDateFactory().create().getString();
        Set<String> workspaces = graph.getWorkspaces();
        while (workspaces.contains(workspaceName)) {
            workspaceName = workspaceName + "1"; // keep appending '1' to the name, until it doesn't match
        }
        return workspaceName;
    }

    protected void initializeContents( Graph graph ) {
        String initialPath = "";
        int depth = 4;
        int numChildrenPerNode = 4;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldBeAtLeastOneWorkspaceAvailableInSource() {
        Set<String> workspaces = graph.getWorkspaces();
        assertThat(workspaces, is(notNullValue()));
        assertThat(workspaces.isEmpty(), is(false));
    }

    @Test
    public void shouldGetRootNodeInEachWorkspace() {
        for (String workspaceName : graph.getWorkspaces()) {
            Location root = graph.useWorkspace(workspaceName).getRoot();
            assertThat(root, is(notNullValue()));

            // Read the node thoroughly (every way we know how) ...
            readNodeThoroughly(root);
        }
    }

    @Test
    public void shouldReadTheChildrenOfTheRootNodeInEachWorkspace() {
        for (String workspaceName : graph.getWorkspaces()) {
            Location root = graph.useWorkspace(workspaceName).getRoot();
            List<Location> children = graph.getChildren().of(root);
            assertThat(children, is(notNullValue()));
            for (Location child : children) {
                // Check the location has a path that has the root as a parent ...
                assertThat(child.hasPath(), is(true));
                assertThat(child.getPath().getParent().isRoot(), is(true));

                // Verify that each node can be read multiple ways ...
                readNodeThoroughly(child);
            }
        }
    }

    @Test( expected = InvalidWorkspaceException.class )
    public void shouldNotReadNodesInWorkspacesThatDoNotExist() {
        // Generate the name of a workspace that is not an existing node ...
        String nonExistantWorkspaceName = generateNonExistantWorkspaceName();

        // Now try to get the root ...
        ReadNodeRequest request = new ReadNodeRequest(location("/"), nonExistantWorkspaceName);
        execute(request);
    }

    @Test
    public void shouldNotAllowCreatingWorkspacesIfCapabilitiesSayCreatingWorkspacesIsNotSupported() {
        if (!source.getCapabilities().supportsCreatingWorkspaces()) {

            // Should not allow creating workspaces using name alteration ...
            try {
                graph.createWorkspace().namedSomethingLike("argle bargle");
                fail("Should not allow creating workspaces when source capabilities say it's not supported");
            } catch (InvalidRequestException error) {
                // expected
            }

            // Should not allow creating workspaces with exact name...
            String nonExistWorkspaceName = generateNonExistantWorkspaceName();
            try {
                graph.createWorkspace().named(nonExistWorkspaceName);
                fail("Should not allow creating workspaces when source capabilities say it's not supported");
            } catch (InvalidRequestException error) {
                // expected
            }
        }
    }

    @Test
    public void shouldNotAllowCloningWorkspacesIfCapabilitiesSayCreatingWorkspacesIsNotSupported() {
        if (!source.getCapabilities().supportsCreatingWorkspaces()) {

            // Should not allow cloning workspaces using name alteration ...
            String existingWorkspaceName = graph.getWorkspaces().iterator().next();
            String nonExistWorkspaceName = generateNonExistantWorkspaceName();
            try {
                graph.createWorkspace().clonedFrom(existingWorkspaceName).namedSomethingLike("something");
                fail("Should not allow cloning workspaces when source capabilities say it's not supported");
            } catch (InvalidRequestException error) {
                // expected
            }

            // Should not allow cloning workspaces with exact name ...
            try {
                graph.createWorkspace().clonedFrom(existingWorkspaceName).named(nonExistWorkspaceName);
                fail("Should not allow cloning workspaces when source capabilities say it's not supported");
            } catch (InvalidRequestException error) {
                // expected
            }
        }
    }

    @Test
    public void shouldNotAllowCloningWorkspaceFromWorkspaceWithSameNameIfAllowedByCapabilities() {
        if (source.getCapabilities().supportsCreatingWorkspaces()) {
            try {
                // Find an existing workspace or create a new one ...
                Workspace existing = null;
                Set<String> existingWorkspaceNames = graph.getWorkspaces();
                if (existingWorkspaceNames.isEmpty()) {
                    String[] validWorkspaceNames = generateValidNamesForNewWorkspaces();
                    if (validWorkspaceNames.length == 0) return;
                    existing = graph.createWorkspace().namedSomethingLike(validWorkspaceNames[0]);
                    assertThat(existing, is(notNullValue()));
                    String workspaceName1 = existing.getName();
                    assertThat(workspaceName1, is(notNullValue()));
                    assertThat(workspaceName1.trim().length(), is(not(0)));

                    // Initialize workspace one with some content ...
                    initializeContents(graph);
                } else {
                    existing = graph.useWorkspace(existingWorkspaceNames.iterator().next());
                }

                // Clone 'workspace1' into workspace1' ... yes this is invalid
                try {
                    graph.createWorkspace().clonedFrom(existing.getName()).named(existing.getName());
                    fail("No error reported after attempting to create a cloned workspace that was same name as clone");
                } catch (InvalidWorkspaceException error) {
                    // expected
                }
            } catch (InvalidRequestException error) {
                // Updates may not be supported, but if they are then this is a failure ...
                if (source.getCapabilities().supportsUpdates()) throw error;
            }
        }
    }

    @Test
    public void shouldAllowCreatingWorkspaceWithValidNameIfAllowedByCapabilities() {
        if (source.getCapabilities().supportsCreatingWorkspaces()) {
            String[] validNames = generateValidNamesForNewWorkspaces();
            for (String validName : validNames) {
                Workspace workspace = graph.createWorkspace().named(validName);

                assertThat(workspace, is(notNullValue()));
                String workspaceName1 = workspace.getName();
                assertThat(workspaceName1, is(notNullValue()));
                assertThat(workspaceName1.trim().length(), is(not(0)));
            }
        }
    }

    @Test
    public void shouldNotAllowCreatingWorkspaceWithInvalidNameIfAllowedByCapabilities() {
        if (source.getCapabilities().supportsCreatingWorkspaces()) {
            String[] invalidNames = generateInvalidNamesForNewWorkspaces();
            if (invalidNames != null) {
                for (String invalidName : invalidNames) {
                    try {
                        graph.createWorkspace().named(invalidName);
                        fail("Did not fail to create workspace with name that should be invalid");
                    } catch (InvalidWorkspaceException error) {
                        // expected
                    }
                }
            }
        }
    }

    @Test
    public void shouldAllowCreatingWorkspaceByCloningAnExistingWorkspaceIfAllowedByCapabilities() {
        if (source.getCapabilities().supportsCreatingWorkspaces()) {
            String[] validWorkspaceNames = generateValidNamesForNewWorkspaces();
            if (validWorkspaceNames.length < 1) return;

            try {
                // Is there an existing workspace?
                Set<String> existingWorkspaceNames = graph.getWorkspaces();
                Workspace workspace1 = null;
                String newWorkspaceName = null;
                if (existingWorkspaceNames.isEmpty()) {
                    workspace1 = graph.createWorkspace().namedSomethingLike(validWorkspaceNames[0]);
                    newWorkspaceName = validWorkspaceNames[1];
                    assertThat(workspace1, is(notNullValue()));
                    String workspaceName1 = workspace1.getName();
                    assertThat(workspaceName1, is(notNullValue()));
                    assertThat(workspaceName1.trim().length(), is(not(0)));

                    // Initialize workspace one with some content ...
                    initializeContents(graph);
                } else {
                    workspace1 = graph.useWorkspace(existingWorkspaceNames.iterator().next());
                    newWorkspaceName = validWorkspaceNames[0];
                }
                assert workspace1 != null;
                assert newWorkspaceName != null;

                // Clone 'workspace1' into 'workspace2'
                String workspaceName = workspace1.getName();
                Workspace workspace2 = graph.createWorkspace().clonedFrom(workspaceName).named(newWorkspaceName);

                // Verify that the content of 'workspace1' matches that of 'workspace2'
                Subgraph subgraph1 = graph.getSubgraphOfDepth(100000).at(workspace1.getRoot());
                Subgraph subgraph2 = graph.getSubgraphOfDepth(100000).at(workspace2.getRoot());
                assertEquivalentSubgraphs(subgraph1, subgraph2, true, true);
            } catch (InvalidRequestException error) {
                // Updates may not be supported, but if they are then this is a failure ...
                if (source.getCapabilities().supportsUpdates()) throw error;
            }
        }
    }
}
