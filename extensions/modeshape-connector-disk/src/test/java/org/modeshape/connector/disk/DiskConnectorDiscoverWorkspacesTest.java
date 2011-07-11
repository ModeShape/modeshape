package org.modeshape.connector.disk;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.MockRepositoryContext;

public class DiskConnectorDiscoverWorkspacesTest {

    // Extremely low threshold for testing
    private final int LARGE_VALUE_THRESHOLD = 20;
    private final String REPO_ROOT_PATH = "./target/repoRootPath";

    protected ExecutionContext context;

    @Before
    public void beforeEach() {
        File repoRootPath = new File(REPO_ROOT_PATH);
        if (repoRootPath.exists()) {
            FileUtil.delete(repoRootPath);
        }
        repoRootPath.mkdirs();

        context = new ExecutionContext();
    }

    private Graph graphForNewSource() throws Exception {
        DiskSource source = new DiskSource();
        source.setName("Disk Source");
        source.setLargeValueSizeInBytes(LARGE_VALUE_THRESHOLD);
        source.setRepositoryRootPath(REPO_ROOT_PATH);
        source.initialize(new MockRepositoryContext(context));

        return Graph.create(source, context);

    }

    @Test
    public void shouldDiscoverExistingWorkspaces() throws Exception {
        Graph graph = graphForNewSource();

        Set<String> originalWorkspaces = graph.getWorkspaces();
        graph.createWorkspace().named("newWorkspace1");
        graph.createWorkspace().named("newWorkspace2");

        Graph newGraph = graphForNewSource();
        Set<String> newWorkspaces = newGraph.getWorkspaces();

        assertThat(newWorkspaces.size(), is(originalWorkspaces.size() + 2));
        assertTrue(newWorkspaces.containsAll(originalWorkspaces));
        assertTrue(newWorkspaces.contains("newWorkspace1"));
        assertTrue(newWorkspaces.contains("newWorkspace2"));
    }
}
