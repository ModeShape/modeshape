/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrTools;

public class ConcurrentWriteTest extends SingleUseAbstractTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-concurrent-tests.json"));
        tools = new JcrTools();

        // Set the transaction timeout so that we can debug code called within the transaction ...
        repository.runningState().txnManager().setTransactionTimeout(500);
    }

    /**
     * Create a session, obtain the root node, and close the session. Do this 500x using 16 threads.
     * 
     * @throws Exception
     */
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyGetRootNode() throws Exception {
        runConcurrently(500, 16, new Operation() {
            @Override
            public void run( Session session ) throws RepositoryException {
                session.getRootNode();
            }
        });
    }

    /**
     * Create a session, add a single node under the root, and close the session. Do this twice using 2 threads. Then verify that
     * there are 2 children under the root (except for the "/jcr:system" node).
     * 
     * @throws Exception
     */
    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyCreateSmallNumberOfTopLevelNodes() throws Exception {
        final int totalOperations = 2;
        final int threads = 2;
        runConcurrently(totalOperations, threads, new CreateChildren("/", "nodeX", 1));
        verify(new NumberOfChildren(totalOperations, "/"));
    }

    /**
     * Create a session, add a single node under the root, and close the session. Do this 500x using 16 threads. Then verify that
     * there are 500 children under the root (except for the "/jcr:system" node).
     * 
     * @throws Exception
     */
    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyCreateTopLevelNodes() throws Exception {
        final int totalOperations = 500;
        final int threads = 16;
        runConcurrently(totalOperations, threads, new CreateChildren("/", "nodeX", 1));
        verify(new NumberOfChildren(totalOperations, "/"));
    }

    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyCreateTwoLevelSubgraphUnderRoot() throws Exception {
        final int totalOperations = 200;
        final int threads = 16;
        final int width = 10;
        final int depth = 2;
        runConcurrently(totalOperations, threads, new CreateSubgraph("/", "nodeX", width, depth));
        verify(new NumberOfChildren(totalOperations, "/"));
        verify(new TotalNumberOfNodesExceptSystem(1 + totalOperations * nodesInTree(width, depth), "/"));
    }

    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyCreateThreeLevelSubgraphUnderRoot() throws Exception {
        final int totalOperations = 200;
        final int threads = 16;
        final int width = 10;
        final int depth = 3;
        runConcurrently(totalOperations, threads, new CreateSubgraph("/", "nodeX", width, depth));
        print("/", false);
        verify(new NumberOfChildren(totalOperations, "/"));
        verify(new TotalNumberOfNodesExceptSystem(1 + totalOperations * nodesInTree(width, depth), "/"));
    }

    @FixFor( "MODE-1739" )
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyModifySameNodesInDifferentOrder() throws Exception {
        // Create several nodes right under the root ...
        final int numNodes = 3;
        runOnce(new CreateSubgraph("/", "node", numNodes, 2), false);
        verify(new NumberOfChildren(numNodes, "node1"));
        print("/", false);
        // Simultaneously try to modify the three nodes in different orders ...
        final int totalOperations = 3;
        final int threads = 16;
        runConcurrently(totalOperations, threads, new ModifyPropertiesOnChildren("/node1", "foo", 3));
    }

    @FixFor( "MODE-1817" )
    @Test
    public void shouldAllowMultipleSessionsToConcurrentlyRemoveSameNode() throws Exception {
        // This issue can be replicated by having two separate threads (each using their own transaction) to
        // try removing the same node.
        // First, add some nodes ...
        Node node = session().getRootNode().addNode("/node");
        Node subnode = node.addNode("subnode");
        subnode.getSession().save();

        // Now run two threads that are timed very carefully ...
        int numThreads = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        Operation operation = new Operation() {
            @Override
            public void run( Session session ) throws Exception {
                Node subnode = session.getNode("/node/subnode");
                subnode.remove();
                barrier.await();
                session.save();
            }
        };
        runConcurrently(numThreads, numThreads, operation);

        verify(new NumberOfChildren(0, "node"));
    }

    @FixFor( "MODE-1821" )
    @Test
    public void shouldFailIfSNSAreNotSupported() throws Exception {
        session.workspace().getNodeTypeManager().registerNodeTypes(resourceStream("cnd/no_sns.cnd"), true);

        Node testRoot = session.getRootNode().addNode("/testRoot", "test:nodeWithoutSNS");
        testRoot.addNode("childA", "nt:unstructured");
        session.save();

        try {
            testRoot.addNode("childA", "nt:unstructured");
            fail("Same name sibling are not supported, an exception should've been thrown");
        } catch (ItemExistsException ex) {
            // this is expected since this is not allowed.
        }

        // Now run two threads that are timed very carefully ...
        int numThreads = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        Operation operation = new Operation() {
            @Override
            public void run( Session session ) throws Exception {
                Node testRoot = session.getNode("/testRoot");
                testRoot.addNode("childB", "nt:unstructured");
                barrier.await();
                // one of the saves should fail but it doesn't
                session.save();
            }
        };

        run(2, numThreads, 1, operation);
        verify(new NumberOfChildren(2, "testRoot"));
    }

    @Test
    @FixFor( "MODE-2216" )
    public void shouldMoveFileAndFoldersConcurrently() throws Exception {
        shutdownDefaultRepo();

        FileUtil.delete("target/move_repository");

        int threadCount = 50;
        String sourcePath = "/source";
        String destPath = "/dest";

        //this will import initial content into the source folder (see above)
        repository = TestingUtil.startRepositoryWithConfig("config/repo-config-move.json");
        Session session = repository.login();
        NodeIterator sourceNodes = session.getNode(sourcePath).getNodes();
        long expectedMoveCount = sourceNodes.getSize();

        final List<Callable<String>> tasks = new ArrayList<Callable<String>>();
        while (sourceNodes.hasNext()) {
            final Node node = sourceNodes.nextNode();
            final MoveNodeTask task = new MoveNodeTask(node.getIdentifier(), destPath);
            tasks.add(task);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<Future<String>>();
        for (Callable<String> task : tasks) {
            futures.add(executorService.submit(task));
        }
        Set<String> movedNodeIds = new HashSet<String>();
        for (Future<String> future : futures) {
            movedNodeIds.add(future.get());
        }

        for (String id : movedNodeIds) {
            Node node = session.getNodeByIdentifier(id);
            assertNotNull("The document with " + id + " was not found!", node);
            assertTrue("The document was not moved to destination folder!", node.getPath().startsWith(destPath));
        }

        NodeIterator destNodeIterator = session.getNode(destPath).getNodes();
        while (destNodeIterator.hasNext()) {
            assertNotNull("Node could be read", destNodeIterator.nextNode());
        }

        assertThat("Incorrect number of nodes moved", (long)movedNodeIds.size(), is(expectedMoveCount));
        assertFalse("The source parent is not empty", session.getNode(sourcePath).getNodes().hasNext());
    }
    
    @Test
    @FixFor( "MODE-2418" )
    public void shouldVersionNodesConcurrently() throws Exception {
        shutdownDefaultRepo();
        FileUtil.delete("target/persistent_repository/store");
        repository = TestingUtil.startRepositoryWithConfig("config/repo-config-filesystem-jbosstxn-pessimistic.json");
        
        Session session = repository.login();
        Node uploads = session.getRootNode().addNode("uploads", NodeType.NT_FOLDER);
        uploads.addMixin(NodeType.MIX_VERSIONABLE);
        session.save();
        session.logout();

        ExecutorService pool = Executors.newCachedThreadPool();

        final String path = "/uploads";
        final int repeatCount = 10;
        Future<Void> writerResult = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                try {
                    VersionManager versionManager = session.getWorkspace().getVersionManager();

                    for (int i = 0; i < repeatCount; i++) {
                        versionManager.checkout(path);
                        Node node = session.getNode(path);
                        String uuid = UUID.randomUUID().toString();
                        String name = Thread.currentThread().getName() + "_" + uuid;
                        node.addNode(name, NodeType.NT_FOLDER);
                        session.save();
                        versionManager.checkin(path);
                    }
                } finally {
                    session.logout();
                }
                return null;
            }
        });   
        
        Future<Void> readerResult = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                try {
                    VersionManager versionManager = session.getWorkspace().getVersionManager();
                    for (int i = 0; i < repeatCount; i++) {
                        Version baseVersion = versionManager.getBaseVersion(path);
                        assertNotNull(baseVersion);
                        Thread.sleep(100);
                    }
                    return null;
                } finally {
                    session.logout();                    
                }
            }
        });

        try {
            writerResult.get();
            readerResult.get();
        } finally {
            pool.shutdownNow();    
        }
    }

    private void shutdownDefaultRepo() {
        if (repository != null) {
            try {
                TestingUtil.killRepositories(repository);
            } finally {
                repository = null;
                config = null;
            }
        }
    }

    private class MoveNodeTask implements Callable<String> {

        private String sourceId;
        private String destinationPath;

        public MoveNodeTask( final String sourceId, final String destinationPath ) {
            this.sourceId = sourceId;
            this.destinationPath = destinationPath;
        }

        @Override
        public String call() throws Exception {
            JcrSession session = repository.login();
            final Node item = session.getNodeByIdentifier(sourceId);
            String destAbsPath = destinationPath + "/" + item.getName();
            String sourceAbsPath = item.getPath();
            try {
                if (print) {
                    System.out.println(Thread.currentThread().getName() + String.format(" Moving node from '%s' to '%s'", sourceAbsPath, destAbsPath));
                }
                session.move(item.getPath(), destAbsPath);
                session.save();
                session.save();
                return item.getIdentifier();
            } catch (Exception e) {
                if (print) {
                    System.out.println(Thread.currentThread().getName() + String.format(" Exception moving node from '%s' to '%s'", sourceAbsPath, destAbsPath));
                }
                throw e;
            } finally {
                session.logout();
            }

        }
    }

    /**
     * Method that can be called within a test method to run the supplied {@link Operation} just once in one thread. This is often
     * useful for initializing content.
     * 
     * @param operation the operation to be performed
     * @param async true if the operation should be run in a separate thread, or false if it this thread should block while the
     *        operation completes
     * @throws Exception if there is a problem executing the operation the specified number of times
     */
    protected void runOnce( final Operation operation,
                            boolean async ) throws Exception {
        if (async) {
            run(1, 1, 0, operation);
        } else {
            Session session = repository.login();
            try {
                operation.run(session);
            } finally {
                session.logout();
            }
        }
    }

    /**
     * Method that can be called within a test method to run the supplied {@link Operation} a total number of times using a
     * specific number of threads.
     * 
     * @param totalNumberOfOperations the total number of times the operation should be performed; must be positive
     * @param numberOfConcurrentClients the total number of separate clients/threads that should be used; must be positive
     * @param operation the operation to be performed
     * @throws Exception if there is a problem executing the operation the specified number of times
     */
    protected void runConcurrently( final int totalNumberOfOperations,
                                    final int numberOfConcurrentClients,
                                    final Operation operation ) throws Exception {
        run(totalNumberOfOperations, numberOfConcurrentClients, 0, operation);
    }

    /**
     * Method that can be called within a test method to run the supplied verification {@link Operation} only once.
     * 
     * @param operation the verification operation to be performed
     * @throws Exception if there is a problem executing the operation
     */
    protected void verify( final Operation operation ) throws Exception {
        run(1, 1, 0, operation);
    }

    protected static int nodesInTree( int width,
                                      int depth ) {
        return calculateTotalNumberOfNodesInTree(width, depth - 1, true);
    }

    /**
     * An {@link Operation} that creates a subgraph of nodes, starting with a single node under the specified parent node. The
     * width and depth of the subgraph are configurable.
     */
    @Immutable
    public static class CreateSubgraph implements Operation {
        protected final int depth;
        protected final int width;
        protected final String nodeName;
        protected final String path;
        protected final AtomicInteger counter = new AtomicInteger(1);

        public CreateSubgraph( String parentPath,
                               String nodeName,
                               int width,
                               int depth ) {
            this.depth = depth;
            this.width = width;
            this.path = parentPath;
            this.nodeName = nodeName != null ? nodeName : "";
        }

        @Override
        public void run( Session session ) throws RepositoryException {
            Node parentNode = session.getNode(path);
            Node topLevel = parentNode.addNode(nodeName + Integer.toString(counter.getAndIncrement()));
            topLevel.setProperty("foo", "bar");
            addChildren(topLevel, this.depth - 1);
            session.save();
        }

        protected void addChildren( Node parent,
                                    int depth ) throws RepositoryException {
            if (depth > 0) {
                for (int i = 0; i != width; ++i) {
                    Node child = parent.addNode(nodeName + Integer.toString(i + 1));
                    child.setProperty("foo", "bar" + i);
                    addChildren(child, depth - 1);
                }
            }
        }
    }

    /**
     * An {@link Operation} that creates a set of child nodes under the specified parent.
     */
    @Immutable
    public static class CreateChildren extends CreateSubgraph {

        public CreateChildren( String parentPath,
                               String nodeName,
                               int width ) {
            super(parentPath, nodeName, width, 1);
        }

        @Override
        public void run( Session session ) throws RepositoryException {
            Node parentNode = session.getNode(path);
            addChildren(parentNode, this.depth);
            session.save();
        }
    }

    /**
     * An {@link Operation} that modifies a supplied property on the children of the supplied parent node. Each time this instance
     * is called (perhaps in separate threads), the children will be modified in a different order.
     */
    @Immutable
    public static class ModifyPropertiesOnChildren implements Operation {

        private final String parentPath;
        private final String propertyName;
        private final int childrenToUpdate;
        private final ReentrantLock nextIndexLock = new ReentrantLock();
        private long nextIndex;
        private final AtomicInteger propertyValueCounter = new AtomicInteger(1);

        public ModifyPropertiesOnChildren( String parentPath,
                                           String propertyName,
                                           int childrenToUpdate ) {
            this.parentPath = parentPath;
            this.propertyName = propertyName;
            this.childrenToUpdate = childrenToUpdate;
        }

        @Override
        public void run( Session session ) throws RepositoryException {
            Node parentNode = session.getNode(parentPath);
            NodeIterator childIter = parentNode.getNodes();
            // Get the first iterator that starts at the 'nth' child (each thread starts at a different child) ...
            long numChildren = childIter.getSize();
            long offset = getOffset(numChildren);
            childIter.skip(offset);
            // Modify a set of children ...
            int childrenToUpdate = Math.min(this.childrenToUpdate, (int)numChildren);
            for (int i = 0; i != childrenToUpdate; ++i) {
                childIter = validateIterator(childIter, parentNode);
                Node child = childIter.nextNode();
                child.setProperty(propertyName, "change" + propertyValueCounter.getAndIncrement());
            }
            // Save the changes ...
            session.save();
        }

        protected NodeIterator validateIterator( NodeIterator iterator,
                                                 Node parentNode ) throws RepositoryException {
            if (iterator.hasNext()) return iterator;
            // Otherwise get a new iterator ...
            return parentNode.getNodes();
        }

        protected final long getOffset( long maxNumberOfChildren ) {
            try {
                nextIndexLock.lock();
                ++nextIndex;
                if (nextIndex >= maxNumberOfChildren) {
                    nextIndex = 0;
                }
                assert nextIndex < maxNumberOfChildren;
                return nextIndex;
            } finally {
                nextIndexLock.unlock();
            }
        }
    }

    /**
     * An {@link Operation} that counts the total number of nodes at or below a specified path, always excluding the "/jcr:system"
     * branch.
     */
    @Immutable
    public static class TotalNumberOfNodesExceptSystem implements Operation {
        private final long number;
        private final String relativePath;

        public TotalNumberOfNodesExceptSystem( long number,
                                               String relativePath ) {
            this.number = number;
            this.relativePath = relativePath;
        }

        @Override
        public void run( Session session ) throws RepositoryException {
            Node node = session.getRootNode();
            if (this.relativePath != null && !this.relativePath.equals("/")) {
                node = node.getNode(relativePath);
            }
            long count = countNodes(node);
            assertThat(count, is(this.number));
        }

        protected long countNodes( Node node ) throws RepositoryException {
            long count = 1;
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.getDepth() == 1 && child.getName().equals("jcr:system")) continue;
                count += countNodes(child);
            }
            return count;
        }
    }

    /**
     * An {@link Operation} that counts the total number of children under the node at the specified path. If the path specifies
     * the root node, then the "/jcr:system" node is ignored.
     */
    @Immutable
    public static class NumberOfChildren extends TotalNumberOfNodesExceptSystem {

        public NumberOfChildren( long number,
                                 String relativePath ) {
            super(number, relativePath);
        }

        @Override
        protected long countNodes( Node node ) throws RepositoryException {
            long count = node.getNodes().getSize();
            if (node.getDepth() == 0) --count; // exclude "/jcr:system"
            return count;
        }
    }

    /**
     * An operation that can be run by clients.
     * 
     * @see ConcurrentWriteTest#runConcurrently(int, int, Operation)
     */
    @ThreadSafe
    protected static interface Operation {
        void run( Session session ) throws RepositoryException, Exception;
    }

    private void run( final int totalNumberOfOperations,
                      final int numberOfConcurrentClients,
                      final int numberOfErrorsExpected,
                      final Operation operation ) throws Exception {
        CheckArg.isPositive(totalNumberOfOperations, "totalNumberOfOperations");
        CheckArg.isPositive(numberOfConcurrentClients, "numberOfConcurrentClients");
        CheckArg.isNonNegative(numberOfErrorsExpected, "numberOfErrorsExpected");

        // Create the latch ...
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numberOfConcurrentClients);
        final Results problems = new Results();
        final AtomicInteger actualOperationCount = new AtomicInteger(1);

        // Create a session and thread for each client ...
        final Repository repository = this.repository;
        final Session[] sessions = new Session[numberOfConcurrentClients];
        Thread[] threads = new Thread[numberOfConcurrentClients];
        for (int i = 0; i != numberOfConcurrentClients; ++i) {
            sessions[i] = repository.login();
            final String threadName = "RepoClient" + (i + 1);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        printMessage("Initializing thread '" + threadName + '"');

                        // Block until all threads are ready to start ...
                        startLatch.await();

                        printMessage("Starting thread '" + threadName + '"');

                        // Perform the operation as many times as requested ...
                        int repeatCount = 1;
                        while (true) {
                            int operationNumber = actualOperationCount.getAndIncrement();

                            if (operationNumber > totalNumberOfOperations) break;

                            ++repeatCount;
                            Session session = null;
                            printMessage("Running operation " + repeatCount + " in thread '" + threadName + '"');
                            try {
                                // Create the session ...
                                session = repository.login();

                                // Run the operation ...
                                operation.run(session);

                            } catch (Throwable e) {
                                problems.recordError(threadName, repeatCount, e);
                            } finally {
                                // Always log out of the session ...
                                if (session != null) session.logout();

                                if (operationNumber % 100 == 0 && operationNumber > 0) {
                                    printMessage("Completed " + operationNumber + " operations");
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        e.printStackTrace();
                    } finally {
                        // Thread is done, so count it down ...
                        printMessage("Completing thread '" + threadName + '"');
                        completionLatch.countDown();
                    }
                }
            };
            threads[i] = new Thread(runnable, threadName);
        }

        // Start the threads ...
        for (int i = 0; i != numberOfConcurrentClients; ++i) {
            threads[i].start();
        }

        // Unlock the starting latch ...
        startLatch.countDown();

        // Wait until all threads are finished (or at most 60 seconds)...
        completionLatch.await(60, TimeUnit.SECONDS);

        // Clean up the threads ...
        for (int i = 0; i != numberOfConcurrentClients; ++i) {
            try {
                Thread thread = threads[i];
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            } finally {
                threads[i] = null;
            }
        }

        // Verify that we've performed the requested number of operations ...
        assertThat(actualOperationCount.get() > totalNumberOfOperations, is(true));

        // Verify there are no errors ...
        int problemsCount = problems.size();
        if (problemsCount != numberOfErrorsExpected) {
            if (numberOfConcurrentClients == 1) {
                // Just one thread, so rethrow the exception ...
                Throwable t = problems.getFirstException();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                }
                if (t instanceof Error) {
                    throw (Error)t;
                }
                throw (Exception)t;
            } else if (problemsCount == 0 && numberOfErrorsExpected > 0) {
                fail(numberOfErrorsExpected + " errors expected, but none occurred");
            }
            // Otherwise, multiple clients so log the set of them ...
            fail(problems.toString());
        }
    }

    protected static class Results {
        private List<Error> errors = new CopyOnWriteArrayList<Error>();

        protected void recordError( String threadName,
                                    int iteration,
                                    Throwable error ) {
            errors.add(new Error(threadName, iteration, error));
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int size() {
            return errors.size();
        }

        public Throwable getFirstException() {
            return errors.size() > 0 ? errors.get(0).error : null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Error error : errors) {
                sb.append(error.threadName)
                  .append("{")
                  .append(error.iteration)
                  .append("} -> ")
                  .append(StringUtil.getStackTrace(error.error))
                  .append("\n");
            }
            return sb.toString();
        }

        protected class Error {
            protected final String threadName;
            protected final Throwable error;
            protected final int iteration;

            protected Error( String threadName,
                             int iteration,
                             Throwable error ) {
                this.threadName = threadName;
                this.iteration = iteration;
                this.error = error;
            }
        }
    }

}
