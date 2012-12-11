/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

public class ConcurrentWriteTest extends SingleUseAbstractTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        // FileUtil.delete("target/persistent_repository");
        //
        // startRepositoryWithConfiguration(getClass().getClassLoader()
        // .getResourceAsStream("config/repo-config-concurrent-tests.json"));
        // tools = new JcrTools();
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        // FileUtil.delete("target/persistent_repository");
    }

    /**
     * Create a session, obtain the root node, and close the session. Do this 500x using 16 threads.
     * 
     * @throws Exception
     */
    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyGetRootNode() throws Exception {
        print = true;
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
        print = true;
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
        print = true;
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
        print = true;
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
        // print = true;
        print("/", false);
        verify(new NumberOfChildren(totalOperations, "/"));
        verify(new TotalNumberOfNodesExceptSystem(1 + totalOperations * nodesInTree(width, depth), "/"));
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
        void run( Session session ) throws RepositoryException;
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
            final int index = i;
            final String threadName = "RepoClient" + (index + 1);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // printMessage("Initializing thread '" + threadName + '"');

                        // Block until all threads are ready to start ...
                        startLatch.await();

                        // printMessage("Starting thread '" + threadName + '"');

                        // Perform the operation as many times as requested ...
                        int repeatCount = 1;
                        while (true) {
                            int operationNumber = actualOperationCount.getAndIncrement();
                            if (operationNumber > totalNumberOfOperations) break;

                            ++repeatCount;
                            Session session = null;
                            // printMessage("Running operation " + repeatCount + " in thread '" + threadName + '"');
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
                        // printMessage("Completing thread '" + threadName + '"');
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
        if (problems.size() != numberOfErrorsExpected) {
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
