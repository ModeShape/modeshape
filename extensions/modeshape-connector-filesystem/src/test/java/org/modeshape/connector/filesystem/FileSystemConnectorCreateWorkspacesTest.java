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
package org.modeshape.connector.filesystem;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WorkspaceConnectorTest;

/**
 * These tests verify that the file system connector behaves correctly when the source is configured to
 * {@link FileSystemSource#setCreatingWorkspacesAllowed(boolean) allow the creation of workspaces}.
 */
public class FileSystemConnectorCreateWorkspacesTest extends WorkspaceConnectorTest {

    private String pathToRepositories;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        // Set the connection properties to be use the content of "./src/test/resources/repositories" as a repository ...
        pathToRepositories = "./src/test/resources/repositories/";
        String[] predefinedWorkspaceNames = new String[] {pathToRepositories + "airplanes", pathToRepositories + "cars"};
        FileSystemSource source = new FileSystemSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);
        source.setUpdatesAllowed(true);
        source.setExclusionPattern("\\.svn");

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
        // No need to initialize any content ...
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
        return new String[] {pathToRepositories + "trains"};
    }

    @Test
    public void shouldReturnListOfWorkspacesMatchingAbsoluteCanonicalPathsToDirectories() {
        // The the actual names of the workspaces ...
        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : graph.getWorkspaces()) {
            Workspace workspace = graph.useWorkspace(workspaceName);
            workspaceNames.add(workspace.getName());
        }
        // The actual names should be the absolute paths to the directories representing the root ...
        String absolutePathToRepositories = "./src/test/resources/repositories/";

        assertThat(workspaceNames.remove(absolutePathToRepositories + "airplanes"), is(true));
        assertThat(workspaceNames.remove(absolutePathToRepositories + "cars"), is(true));
        assertThat(workspaceNames.isEmpty(), is(true));

        // The actual names of the workspaces should also be canonical paths ...
        workspaceNames = new HashSet<String>(graph.getWorkspaces());
        assertThat(workspaceNames.remove(absolutePathToRepositories + "airplanes"), is(true));
        assertThat(workspaceNames.remove(absolutePathToRepositories + "cars"), is(true));
        assertThat(workspaceNames.isEmpty(), is(true));
    }
}
