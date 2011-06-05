package org.modeshape.connector.disk;

import java.io.File;
import java.io.IOException;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.xml.sax.SAXException;

public class DiskConnectorWritableTest extends WritableConnectorTest {

    private final String REPOSITORY_ROOT = "./target/repositoryRoot";

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        String[] predefinedWorkspaceNames = new String[] {"default"};
        DiskSource source = new DiskSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);
        source.setRepositoryRootPath(REPOSITORY_ROOT);
        source.setLockFileUsed(true);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws SAXException
     * @throws IOException
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws IOException, SAXException {
        File repositoryRoot = new File(REPOSITORY_ROOT);

        FileUtil.delete(repositoryRoot);
        repositoryRoot.mkdir();
    }
}
