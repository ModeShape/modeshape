package org.modeshape.connector.infinispan;

import java.io.File;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WorkspaceConnectorTest;

public class InfinispanConnectorNoCreateWorkspaceTest extends WorkspaceConnectorTest {

    private String pathToRepositories;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        // Set the connection properties to be use the content of "./src/test/resources/repositories" as a repository ...
        pathToRepositories = new File(".").getAbsolutePath() + "/src/test/resources/repositories/";
        String[] predefinedWorkspaceNames = new String[] {pathToRepositories + "airplanes", pathToRepositories + "cars"};
        InfinispanSource source = new InfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);

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
}
