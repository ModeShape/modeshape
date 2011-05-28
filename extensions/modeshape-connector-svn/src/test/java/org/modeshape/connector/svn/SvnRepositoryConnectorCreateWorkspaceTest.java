package org.modeshape.connector.svn;

import org.junit.BeforeClass;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WorkspaceConnectorTest;

public class SvnRepositoryConnectorCreateWorkspaceTest extends WorkspaceConnectorTest {

    private static String url;

    @BeforeClass
    public static void beforeAny() throws Exception {
        url = SvnConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of_dummy_svn_repos");

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        String[] predefinedWorkspaceNames = new String[] {"trunk"};
        SvnRepositorySource source = new SvnRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootUrl(url);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);
        source.setUpdatesAllowed(false);

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
        return new String[] {"tags"};
    }

}
