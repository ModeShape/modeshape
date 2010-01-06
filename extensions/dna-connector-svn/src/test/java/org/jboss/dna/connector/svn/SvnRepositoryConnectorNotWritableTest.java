package org.jboss.dna.connector.svn;

import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.NotWritableConnectorTest;
import org.junit.BeforeClass;

public class SvnRepositoryConnectorNotWritableTest extends NotWritableConnectorTest {

    private static String url;

    @BeforeClass
    public static void beforeAny() throws Exception {
        url = SvnConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        String[] predefinedWorkspaceNames = new String[] {"trunk", "tags"};
        SvnRepositorySource source = new SvnRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootUrl(url);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
        
        return source;
    }


    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        // No need to initialize any content ...
    }
}
