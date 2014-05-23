package org.modeshape.jcr;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.modeshape.common.util.FileUtil;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Move operation on cluster causes InvalidItemStateException exception
 *
 * @author evgeniy.shevchenko
 * @version 1.0 5/20/14
 */

public class ClusteredRepositoryMoveTest {
    public static final Random RANDOM = new Random();

    /**
     * Count of cluster nodes.
     */
    protected static int CLUSTER_NODES_COUNT = 3;


    protected static final String SOURCE_PATH = "/source";
    protected static final String DEST_PATH = "/dest";
    private static final String EXPECTED_CONTENT = "Lorem ipsum";


    protected JcrRepository[] repositories;

    private static final String OUTPUT_FOLDER = "../modeshape-jcr/target/clustered/%s";
    protected long sourceFolderSize;

    /**
     * Cluster name
     */
    private String clusterName;

    /**
     * List of documents identifiers, which will be validated on all
     * cluster nodes.
     */
    private Set<String> taskResults =
            new HashSet<String>();


    /**
     * Initialize cluster name.
     */
    @Rule
    public TestRule rule = new TestWatcher() {
        /**
         * Initialize the name of test method, it will be used
         * in configuration files as cluster name.
         * Run the test action.
         * @param base
         * @param description
         * @return
         */
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    clusterName = description.getMethodName();
                    base.evaluate();
                }
            };
        }
    };


    /**
     * Start repositories.
     *
     * @throws Exception
     */
    @Before
    public void before()
            throws Exception {
        FileUtil.delete(String.format(OUTPUT_FOLDER, clusterName));
        System.setProperty("cluster.testname", clusterName);
        repositories = new JcrRepository[CLUSTER_NODES_COUNT];
        for (int i = 0; i < CLUSTER_NODES_COUNT; i++) {
            System.setProperty(
                    "jgroups.tcp.port",
                    new Integer(17900 + i).toString());
            System.setProperty(
                    "cluster.item.number",
                    new Integer(i).toString());
            repositories[i] =
                    TestingUtil
                            .startRepositoryWithConfig(
                                    String.format(
                                            "cluster/repo.json", clusterName));
            assertNotNull(repositories[i]);
            assertThat("", repositories[i].getState(), is(ModeShapeEngine.State.RUNNING));
        }
        Session session = repositories[RANDOM.nextInt(repositories.length)].login();
        sourceFolderSize = session.getNode(SOURCE_PATH).getNodes().getSize();
        session.logout();

    }

    /**
     * Validate that all changes were replicated to all repositories.
     * Stop repositories.
     *
     * @throws Exception
     */
    @After
    public void after() throws Exception {
        try {
            Thread.sleep(3000);
            validate();
        } finally {
            TestingUtil.killRepositories(repositories);
        }
    }

    /**
     * Validate that all objects are available in all repositories
     * after replication.
     *
     * @throws Exception on error
     */
    private void validate() throws Exception {
        for (JcrRepository repository : repositories) {
            JcrSession session = null;
            try {
                session = repository.login();
                validateRepository(
                        session,
                        taskResults,
                        DEST_PATH,
                        EXPECTED_CONTENT,
                        sourceFolderSize,
                        true);
                NodeIterator srcNodeIterator =
                        ((Node) session.getNode(SOURCE_PATH)).getNodes();
                assertFalse("Source folder is empty", srcNodeIterator.hasNext());
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }


    private void validateJcrContent(final Node node, final String expectedContent, final boolean isBas64Encoded) throws Exception {
        Node jcrContent = node.getNode("jcr:content");
        InputStream is = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            is = jcrContent.getProperty("jcr:data").getBinary().getStream();
            bis = new BufferedInputStream(is);
            int result = bis.read();
            while (result != -1) {
                byte b = (byte) result;
                buf.write(b);
                result = bis.read();
            }
            String body = isBas64Encoded ?
                    new String(Base64.decodeBase64(buf.toByteArray())) :
                    buf.toString();
            assertTrue(
                    "Document contain appropriate body",
                    body.contains(expectedContent));
        } finally {

            if (bis != null) {
                bis.close();
            }
            if (is != null) {
                is.close();
            }
            if (buf != null) {
                buf.close();
            }
        }

    }

    private void validateFolder(
            final Node node,
            final String destPath,
            final String expectedContent,
            final boolean isBase64Encoded) throws Exception {
        assertTrue("Node is folder", node.isNodeType("nt:folder"));
        assertNotNull(
                "The document with '%s' identifier was found!",
                node);
        assertTrue(
                "The document was moved to destination folder!",
                node.getPath().startsWith(destPath));
        NodeIterator iterator = node.getNodes();
        while (iterator.hasNext()) {
            Node item = iterator.nextNode();
            if (item.isNodeType("nt:folder")) {
                validateFolder(item, destPath, expectedContent, isBase64Encoded);
            } else {
                validateJcrContent(item, expectedContent, isBase64Encoded);
            }
        }
    }

    /**
     * Validate that all objects are available in repository
     * after replication.
     *
     * @throws Exception on error
     */
    private void validateRepository(
            final JcrSession session,
            final Set<String> taskResults,
            final String destPath,
            final String expectedContent,
            final long expectedCount,
            final boolean isBase64Encoded) throws Exception {
        for (String id : taskResults) {
            Node node = session.getNodeByIdentifier(id);
            assertNotNull(
                    "The document with '%s' identifier was found!",
                    node);
            assertTrue(
                    "The document was moved to destination folder!",
                    node.getPath().startsWith(destPath));
            if (node.isNodeType("nt:file")) {
                validateJcrContent(node, expectedContent, isBase64Encoded);
            } else {
                validateFolder(node, destPath, expectedContent, isBase64Encoded);
            }
        }

        NodeIterator destNodeIterator =
                ((Node) session.getNode(destPath)).getNodes();
        while (destNodeIterator.hasNext()) {
            Node node = destNodeIterator.nextNode();
            assertNotNull("Node could be read", node);
        }

        assertThat(
                "There were created appropriate child nodes count",
                (long) taskResults.size(),
                is(expectedCount));
    }

    /**
     * Execute {@see Callable} tasks.
     *
     * @throws Exception on error
     */
    private void executeTest(final int poolSize, List<Callable<String>> tasks)
            throws Exception {
        CompletionService<String> completionService =
                new ExecutorCompletionService(
                        Executors.newFixedThreadPool(poolSize));
        for (Callable task : tasks) {
            completionService.submit(task);
        }
        for (int i = 0; i < tasks.size(); i++) {
            String taskResult =
                    completionService.take().get();
            if (taskResult != null) {
                taskResults.add(taskResult);
            }


        }
    }


    /**
     * Test Move operation on cluster.
     * Move operations are executing concurrently with 25 threads.
     * Source folder has 100 nodes.
     * Repository configuration can be found by this
     * <a href="file:////src/test/resources/cluster/repo.json">link</a>
     *
     * @throws Exception on error
     */
    @Test
    public void shouldMoveConcurrentlyWith25Threads() throws Exception {
        executeTest(25, generateTasks());
    }

    /**
     * Test Move operation on cluster.
     * Move operations are executing concurrently with 50 threads.
     * Source folder has 100 nodes.
     * Repository configuration can be found by this
     * <a href="file:////src/test/resources/cluster/repo.json">link</a>
     *
     * @throws Exception on error
     */
    @Test
    public void shouldMoveConcurrentlyWith50Threads() throws Exception {
        executeTest(50, generateTasks());
    }

    /**
     * Test Move operation on cluster.
     * Move operations are executing concurrently with 75 threads.
     * Source folder has 100 nodes.
     * Repository configuration can be found by this
     * <a href="file:////src/test/resources/cluster/repo.json">link</a>
     *
     * @throws Exception on error
     */
    @Test
    public void shouldMoveConcurrentlyWith75Threads() throws Exception {
        executeTest(75, generateTasks());
    }

    /**
     * Test Move operation on cluster.
     * Move operations are executing concurrently with 100 threads.
     * Source folder has 100 nodes.
     * Repository configuration can be found by this
     * <a href="file:////src/test/resources/cluster/repo.json">link</a>
     *
     * @throws Exception on error
     */
    @Test
    public void shouldMoveConcurrentlyWith100Threads() throws Exception {
        executeTest(100, generateTasks());
    }


    /**
     * Generate list of {@see Callable} tasks.
     * Read the source folder and create {@see List} of {@see Callable}
     * tasks for each node.
     * Where one task will login to random repo in cluster and
     * execute "move operation" for correct node. The size of list will be
     * equal size of source folder(One task per one node to exclude concurrent
     * move for same node).
     *
     * @return List of tasks
     */

    private List<Callable<String>> generateTasks() throws RepositoryException {
        final List<Callable<String>> tasks =
                new ArrayList<Callable<String>>();
        final JcrRepository repository = repositories[
                RANDOM.nextInt(repositories.length)];
        final JcrSession session = repository.login();
        final Node sourceFolder = session.getNode(SOURCE_PATH);
        final NodeIterator sourceFolderIterator = sourceFolder.getNodes();
        while (sourceFolderIterator.hasNext()) {
            final Node node = sourceFolderIterator.nextNode();
            final MoveNodeTask task =
                    new MoveNodeTask(
                            repository,
                            node.getIdentifier(),
                            DEST_PATH);
            tasks.add(task);
        }

        return tasks;
    }


}

