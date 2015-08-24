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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;

public class TransactionsTest extends SingleUseAbstractTest {

    @FixFor( "MODE-1819" )
    @Test
    public void shouldBeAbleToMoveNodeWithinUserTransaction() throws Exception {
        startTransaction();
        moveDocument("childX");
        commitTransaction();
    }

    @FixFor( "MODE-1819" )
    @Test
    public void shouldBeAbleToMoveNodeOutsideOfUserTransaction() throws Exception {
        moveDocument("childX");
    }

    @FixFor( "MODE-1819" )
    @Test
    public void shouldBeAbleToUseSeparateSessionsWithinSingleUserTransaction() throws Exception {
        // We'll use separate threads, but we want to have them both do something specific at a given time,
        // so we'll use a barrier ...
        final CyclicBarrier barrier1 = new CyclicBarrier(2);
        final CyclicBarrier barrier2 = new CyclicBarrier(2);

        // The path at which we expect to find a node ...
        final String path = "/childY/grandChildZ";

        // Create a runnable to obtain a session and look for a particular node ...
        final AtomicReference<Exception> separateThreadException = new AtomicReference<Exception>();
        final AtomicReference<Node> separateThreadNode = new AtomicReference<Node>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Wait till we both get to the barrier ...
                Session session = null;
                try {
                    barrier1.await(20, TimeUnit.SECONDS);

                    // Create a second session, which should NOT see the persisted-but-not-committed changes ...
                    session = newSession();
                    Node grandChild2 = session.getNode(path);
                    separateThreadNode.set(grandChild2);

                } catch (Exception err) {
                    separateThreadException.set(err);
                } finally {
                    try {
                        barrier2.await();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        // Start another session in a separate thread that won't participate in our transaction ...
        new Thread(runnable).start();

        // Now start a transaction ...
        startTransaction();

        // Create first session and make some changes ...
        Node node = session.getRootNode().addNode("childY");
        node.setProperty("foo", "bar");
        Node grandChild = node.addNode("grandChildZ");
        assertThat(grandChild.getPath(), is(path));
        session.save(); // persisted but not committed ...

        // Use the same session to find the node ...
        Node grandChild1 = session.getNode(path);
        assertThat(grandChild.isSame(grandChild1), is(true));

        // Create a second session, which should see the persisted-but-not-committed changes ...
        Session session2 = newSession();
        Node grandChild2 = session2.getNode(path);
        assertThat(grandChild.isSame(grandChild2), is(true));
        session2.logout();

        // Sync up with the other thread ...
        barrier1.await();
        // Await while the other thread does its work and looks for the node ...
        barrier2.await(20, TimeUnit.SECONDS);

        // Commit the transaction ...
        commitTransaction();

        // Our other session should not have seen the node and should have gotten a PathNotFoundException ...
        assertThat(separateThreadNode.get(), is(nullValue()));
        assertThat(separateThreadException.get(), is(instanceOf(PathNotFoundException.class)));

        // It should now be visible outside of the transaction ...
        Session session3 = newSession();
        Node grandChild3 = session3.getNode(path);
        assertThat(grandChild.isSame(grandChild3), is(true));
        session3.logout();
    }

    @Test
    public void shouldBeAbleToVersionOutsideOfUserTransaction() throws Exception {
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        session.save();
        vm.checkin(node.getPath());
    }

    @Test
    public void shouldBeAbleToVersionWithinUserTransactionAndDefaultTransactionManager() throws Exception {
        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        session.save();
        vm.checkin(node.getPath());
        commitTransaction();
    }

    @FixFor( "MODE-1822" )
    @Test
    public void shouldBeAbleToVersionWithinUserTransactionAndJBossTransactionManager() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        InputStream config = getClass().getClassLoader().getResourceAsStream("config/repo-config-inmemory-jbosstxn.json");
        assertThat(config, is(notNullValue()));
        startRepositoryWithConfiguration(config);

        // print = true;
        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        printMessage("Looking for root node");
        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        printMessage("Saving new node at " + node.getPath());
        session.save();
        vm.checkin(node.getPath());

        printMessage("Checked in " + node.getPath());

        for (int i = 0; i != 2; ++i) {
            // Check it back out before we commit ...
            node = session.getRootNode().getNode("Test3");
            printMessage("Checking out " + node.getPath());
            vm.checkout(node.getPath());

            // Make some more changes ...
            node.setProperty("code", "fa-lalalal");
            printMessage("Saving changes to " + node.getPath());
            session.save();

            // Check it back in ...
            printMessage("Checking in " + node.getPath());
            vm.checkin(node.getPath());
        }

        commitTransaction();
    }

    @FixFor( "MODE-1822" )
    @Test
    public void shouldBeAbleToVersionWithinSequentialUserTransactionsAndJBossTransactionManager() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        InputStream config = getClass().getClassLoader().getResourceAsStream("config/repo-config-inmemory-jbosstxn.json");
        assertThat(config, is(notNullValue()));
        startRepositoryWithConfiguration(config);

        // print = true;
        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        printMessage("Looking for root node");
        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        printMessage("Saving new node at " + node.getPath());
        session.save();
        vm.checkin(node.getPath());
        commitTransaction();

        printMessage("Checked in " + node.getPath());

        for (int i = 0; i != 2; ++i) {
            // Check it back out before we commit ...
            node = session.getRootNode().getNode("Test3");
            printMessage("Checking out " + node.getPath());
            vm.checkout(node.getPath());

            // Make some more changes ...
            startTransaction();
            node.setProperty("code", "fa-lalalal");
            printMessage("Saving changes to " + node.getPath());
            session.save();

            // Check it back in ...
            printMessage("Checking in " + node.getPath());
            vm.checkin(node.getPath());
            commitTransaction();
        }
    }

    @FixFor( "MODE-1822" )
    @Test
    public void shouldBeAbleToVersionWithinImmediatelySequentialUserTransactionsAndJBossTransactionManager() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        InputStream config = getClass().getClassLoader().getResourceAsStream("config/repo-config-inmemory-jbosstxn.json");
        assertThat(config, is(notNullValue()));
        startRepositoryWithConfiguration(config);

        // print = true;
        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        printMessage("Looking for root node");
        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        printMessage("Saving new node at " + node.getPath());
        session.save();
        vm.checkin(node.getPath());
        commitTransaction();

        printMessage("Checked in " + node.getPath());

        for (int i = 0; i != 2; ++i) {
            startTransaction();
            // Check it back out before we change anything ...
            printMessage("Checking out " + node.getPath());
            vm.checkout(node.getPath());
            printMessage("Checked out " + node.getPath());

            // Make some more changes ...
            node = session.getRootNode().getNode("Test3");
            node.setProperty("code", "fa-lalalal");
            printMessage("Saving changes to " + node.getPath());
            session.save();

            // Check it back in ...
            printMessage("Checking in " + node.getPath());
            vm.checkin(node.getPath());
            commitTransaction();
        }
    }

    @FixFor( "MODE-1822" )
    @Test
    public void shouldBeAbleToVersionWithinUserTransactionAndAtomikosTransactionManager() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        InputStream config = getClass().getClassLoader().getResourceAsStream("config/repo-config-inmemory-atomikos.json");
        assertThat(config, is(notNullValue()));
        startRepositoryWithConfiguration(config);

        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        session.save();
        vm.checkin(node.getPath());
        commitTransaction();
    }

    @Test
    @FixFor( "MODE-2050" )
    public void shouldBeAbleToUseNoClientTransactionsInMultithreadedEnvironment() throws Exception {
        InputStream configFile = getClass().getClassLoader()
                                           .getResourceAsStream(
                                                   "config/repo-config-inmemory-local-environment-no-client-tx.json");
        startRepositoryWithConfiguration(configFile);
        int threadsCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Future<Void>> results = new ArrayList<Future<Void>>(threadsCount);
        final int nodesCount = 5;
        for (int i = 0; i < threadsCount; i++) {
            Future<Void> result = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Session session = repository.login();
                    try {
                        String threadName = Thread.currentThread().getName();
                        for (int i = 0; i < nodesCount; i++) {
                            session.getRootNode().addNode("test_" + threadName);
                            session.save();
                        }
                        return null;
                    } finally {
                        session.logout();
                    }
                }
            });
            results.add(result);
        }

        for (Future<Void> result : results) {
            result.get(10, TimeUnit.SECONDS);
            result.cancel(true);
        }
        executorService.shutdown();
    }

    @Test
    @FixFor( "MODE-2352 ")
    public void shouldSupportConcurrentWritersUpdatingTheSameNodeWithSeparateUserTransactions() throws Exception {
        FileUtil.delete("target/persistent_repository");
        InputStream configFile = getClass().getClassLoader()
                                           .getResourceAsStream("config/repo-config-filesystem-jbosstxn-pessimistic.json");
        startRepositoryWithConfiguration(configFile);
        int threadsCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Future<Void>> results = new ArrayList<>(threadsCount);
        final AtomicInteger counter = new AtomicInteger(1);
        for (int i = 0; i < threadsCount; i++) {
            Future<Void> result = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    startTransaction();
                    Session session = repository.login();
                    session.getRootNode().addNode("test_" + counter.incrementAndGet());
                    session.save();
                    session.logout();
                    commitTransaction();
                    return null;
                }
            });
            results.add(result);
        }

        for (Future<Void> result : results) {
            result.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();
        Session session = repository.login();
        // don't count jcr:system
        assertEquals(threadsCount, session.getNode("/").getNodes().getSize() - 1);
        session.logout();
    }

    @FixFor( "MODE-2371" )
    @Test
    public void shouldInitializeWorkspacesWithOngoingUserTransaction() throws Exception {
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-inmemory-jbosstxn.json");
        startRepositoryWithConfiguration(configStream);

        startTransaction();
        // the active tx should be suspended for the next call
        Session otherSession = repository.login("otherWorkspace");
        otherSession.logout();
        commitTransaction();
        
        startTransaction();
        session.getWorkspace().createWorkspace("newWS");
        session.logout();
        commitTransaction();
        otherSession = repository.login("newWS");
        otherSession.logout();

        startTransaction();
        session = repository.login();
        session.getWorkspace().createWorkspace("newWS1");
        session.logout();
        otherSession = repository.login("newWS1");
        otherSession.logout();
        commitTransaction();
    }
    
    @Test
    @FixFor( "MODE-2395 ")
    public void shouldSupportMultipleUpdatesFromTheSameSessionWithUserTransactions() throws Exception {
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-inmemory-jbosstxn.json");
        startRepositoryWithConfiguration(configStream);
        final JcrSession mainSession = repository.login();
        
        startTransaction();
        Node node1 = mainSession.getRootNode().addNode("node1");
        node1.setProperty("prop", "foo");
        mainSession.save();
        commitTransaction();
        
        startTransaction();
        Node node2 = mainSession.getRootNode().addNode("node2");
        node2.setProperty("prop", "foo");
        mainSession.save();
        commitTransaction();
        // re-read the node to make sure it's in the cache
        node2 = mainSession.getNode("/node2");
        
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Void> updaterResult = executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                JcrSession updater = repository.login();
                startTransaction();
                AbstractJcrNode node2 = updater.getNode("/node2");
                node2.setProperty("prop", "bar");
                updater.save();
                commitTransaction();
                updater.logout();
                return null;
            }
        });
        updaterResult.get();
        node2 = mainSession.getNode("/node2");
        assertEquals("bar", node2.getProperty("prop").getString());
        mainSession.logout();
    }

    @Test
    @FixFor( "MODE-2495" )
    public void shouldSupportMultipleThreadsChangingTheSameUserTransaction() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        InputStream config = getClass().getClassLoader().getResourceAsStream("config/repo-config-inmemory-jbosstxn.json");
        assertThat(config, is(notNullValue()));
        startRepositoryWithConfiguration(config);

        // STEP 1: create and checkin parent nodes
        Node root = session.getRootNode();
        Node parent = root.addNode("parent");
        parent.addMixin("mix:versionable");
        parent.addNode("nested");
        session.save();

        VersionManager vm = session.getWorkspace().getVersionManager();
        vm.checkin("/parent");
        
        // STEP 2: checkout, create child and checkin
        vm = session.getWorkspace().getVersionManager();
        vm.checkout("/parent");
        Node nested = session.getNode("/parent/nested");
        nested.addNode("child");
        session.save();
        vm.checkin("/parent");
      
        // long transaction
        final Transaction longTx = startTransaction();

        // STEP 3: resume, checkout, suspend
        vm = session.getWorkspace().getVersionManager();
        vm.checkout("/parent");
        session.removeItem("/parent/nested/child");
        session.save();
        suspendTransaction();
        
        // STEP 4: check if child is still exists outside of longTx
        Session s = repository.login();
        s.getNode("/parent/nested/child");
        s.logout();

        // STEP 5: resume, checkin, commit
        Thread t5 = new Thread() {
            @Override
            public void run() {
                try {
                    resumeTransaction(longTx);
                    VersionManager vm = session.getWorkspace().getVersionManager();
                    vm.checkin("/parent");
                    commitTransaction();
                } catch (Exception e) {
                   throw new RuntimeException(e);
                }
            }
        };
        t5.start();
        t5.join();

        // STEP 6: check if child is gone
        try {
            Session s1 = repository.login();
            s1.getNode("/parent/nested/child");
            fail("should fail");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected Transaction suspendTransaction() throws SystemException {
        TransactionManager txnMgr = transactionManager();
        return txnMgr.suspend();
    }

    protected void resumeTransaction(Transaction t) throws InvalidTransactionException, IllegalStateException, SystemException {
        TransactionManager txnMgr = transactionManager();
        txnMgr.resume(t);
    }
    
    protected Transaction  startTransaction() throws NotSupportedException, SystemException {
        TransactionManager txnMgr = transactionManager();
        // Change this to true if/when debugging ...
        if (true) {
            try {
                txnMgr.setTransactionTimeout(1000);
            } catch (Exception e) {
                // ignore
            }
        }
        txnMgr.begin();
        return txnMgr.getTransaction();
    }

    protected void commitTransaction()
        throws SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
        HeuristicRollbackException {
        TransactionManager txnMgr = transactionManager();
        txnMgr.commit();
    }

    protected TransactionManager transactionManager() {
        return session.getRepository().transactionManager();
    }

    private void moveDocument( String nodeName ) throws Exception {
        Node section = session.getRootNode().addNode(nodeName);
        section.setProperty("name", nodeName);

        section.addNode("temppath");
        session.save();

        String srcAbsPath = "/" + nodeName + "/temppath";
        String destAbsPath = "/" + nodeName + "/20130104";

        session.move(srcAbsPath, destAbsPath);
        session.save();

        NodeIterator nitr = section.getNodes();

        if (print) {
            System.err.println("Child Nodes of " + nodeName + " are:");
            while (nitr.hasNext()) {
                Node n = nitr.nextNode();
                System.err.println("  Node: " + n.getName());
            }
        }
    }
}
