package org.modeshape.connector.disk;

import java.io.File;
import java.io.IOException;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.ReadableConnectorTest;
import org.xml.sax.SAXException;

public class DiskConnectorReadableTest extends ReadableConnectorTest {

    private final String REPOSITORY_ROOT = "./target/repositoryRoot";

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        String[] predefinedWorkspaceNames = new String[] {"aircraft", "cars"};
        DiskSource source = new DiskSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
        source.setRepositoryRootPath(REPOSITORY_ROOT);

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

        if (repositoryRoot.exists()) FileUtil.delete(repositoryRoot);
        repositoryRoot.mkdir();

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 4;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        output = null;

        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    // @Test
    // public void readEntireGraph() throws Exception {
    // Stopwatch sw = new Stopwatch();
    // sw.start();
    // Subgraph sg = graph.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
    //
    // sw.stop();
    //
    // int count = 0;
    //
    // for (SubgraphNode sgn : sg) {
    // count++;
    // }
    //
    // System.out.println("Loaded " + count + " nodes in " + sw.getTotalDuration());
    // System.out.println("avg = " + (sw.getTotalDuration().getDuratinInNanoseconds() / (count * 1000)) + " us");
    // }

}
