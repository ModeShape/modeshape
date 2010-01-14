/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.connector.store.jpa;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WorkspaceConnectorTest;
import org.junit.Test;

/**
 * These tests verify that the JPA connector behaves correctly when the source is configured to
 * {@link JpaSource#setCreatingWorkspacesAllowed(boolean) not allow creation of workspaces}.
 */
public class JpaConnectorNoCreateWorkspaceTest extends WorkspaceConnectorTest {

    protected String[] predefinedWorkspaces;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        predefinedWorkspaces = new String[] {"default", "workspace1", "workspace2", "workspace3"};

        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);

        // Override the inherited properties, since that's the focus of these tests ...
        source.setCreatingWorkspacesAllowed(false);
        source.setPredefinedWorkspaceNames(predefinedWorkspaces);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 4;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        for (String workspaceName : predefinedWorkspaces) {
            graph.useWorkspace(workspaceName);
            createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.WorkspaceConnectorTest#generateInvalidNamesForNewWorkspaces()
     */
    @Override
    protected String[] generateInvalidNamesForNewWorkspaces() {
        return null; // nothing is considered invalid
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.WorkspaceConnectorTest#generateValidNamesForNewWorkspaces()
     */
    @Override
    protected String[] generateValidNamesForNewWorkspaces() {
        return new String[] {"new workspace1", "new workspace2", "new workspace3", "new workspace4"};
    }

    @Test
    public void shouldReturnListOfWorkspaces() {
        // The the actual names of the workspaces ...
        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : graph.getWorkspaces()) {
            Workspace workspace = graph.useWorkspace(workspaceName);
            workspaceNames.add(workspace.getName());
        }
        // The actual names should be the absolute paths to the directories representing the root ...
        for (String expectedName : predefinedWorkspaces) {
            assertThat(workspaceNames.remove(expectedName), is(true));
        }
        assertThat(workspaceNames.isEmpty(), is(true));

        // The actual names of the workspaces should also be canonical paths ...
        workspaceNames = new HashSet<String>(graph.getWorkspaces());
        for (String expectedName : predefinedWorkspaces) {
            assertThat(workspaceNames.remove(expectedName), is(true));
        }
        assertThat(workspaceNames.isEmpty(), is(true));
    }

}
