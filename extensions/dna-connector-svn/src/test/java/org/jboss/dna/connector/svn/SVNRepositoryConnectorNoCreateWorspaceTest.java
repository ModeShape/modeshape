package org.jboss.dna.connector.svn;

import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WorkspaceConnectorTest;

public class SVNRepositoryConnectorNoCreateWorspaceTest extends WorkspaceConnectorTest {

    
    private String repositoryRootURL;
    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        repositoryRootURL = SVNConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");
        String[] predefinedWorkspaceNames = new String[]{repositoryRootURL+"trunk", repositoryRootURL+"tags"};
        SVNRepositorySource source = new SVNRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootURL(repositoryRootURL);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDirectoryForDefaultWorkspace(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
        
        return source;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
        // No need to initialize any content ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.WorkspaceConnectorTest#generateInvalidNamesForNewWorkspaces()
     */
    @Override
    protected String[] generateInvalidNamesForNewWorkspaces() {
        return null; // nothing is considered invalid
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.WorkspaceConnectorTest#generateValidNamesForNewWorkspaces()
     */
    @Override
    protected String[] generateValidNamesForNewWorkspaces() {
        return new String[] {repositoryRootURL + "branches"};
    }

}
