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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.security.acl.JcrAccessControlList;
import org.modeshape.jcr.security.acl.Privileges;

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
        Runnable runnable = () -> {
            // Wait till we both get to the barrier ...
            Session session1 = null;
            try {
                barrier1.await(20, TimeUnit.SECONDS);

                // Create a second session, which should NOT see the persisted-but-not-committed changes ...
                session1 = newSession();
                Node grandChild2 = session1.getNode(path);
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
        };

        // Start another session in a separate thread that won't participate in our transaction ...
        new Thread(runnable).start();

        // Now start a transaction ...
        startTransaction();

        // Create first session and make some changes ...
        Node node = session.getRootNode().addNode("childY");
        node.setProperty("foo", "bar");
        Node grandChild = node.addNode("grandChildZ");
        grandChild.setProperty("foo", "bar");
        assertThat(grandChild.getPath(), is(path));
        session.save(); // persisted but not committed ...

        session.getNode("/childY/grandChildZ").setProperty("bar", "baz");
        session.save();
        
        // Use the same session to find the node ...
        Node grandChild1 = session.getNode(path);
        assertThat(grandChild.isSame(grandChild1), is(true));
        assertEquals("bar", grandChild1.getProperty("foo").getString());
        assertEquals("baz", grandChild1.getProperty("bar").getString());
        
        // Create a second session, which should see the persisted-but-not-committed changes ...
        Session session2 = newSession();
        Node grandChild2 = session2.getNode(path);
        assertThat(grandChild.isSame(grandChild2), is(true));
        assertEquals("bar", grandChild2.getProperty("foo").getString());
        assertEquals("baz", grandChild2.getProperty("bar").getString());

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
        assertEquals("bar", grandChild3.getProperty("foo").getString());
        assertEquals("baz", grandChild3.getProperty("bar").getString());        
        
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
    @FixFor( "MODE-2642" )
    public void shouldBeAbleToVersionWithinUserTransactionAndDefaultTransactionManager() throws Exception {
        startTransaction();
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node node = session.getRootNode().addNode("Test3");
        node.addMixin("mix:versionable");
        node.setProperty("name", "lalalal");
        node.setProperty("code", "lalalal");
        session.save();
        vm.checkin(node.getPath());
        assertFalse(node.isCheckedOut());
        try {
            node.addMixin("mix:lockable");
            fail("Expected a version exception because the node is checked in");
        } catch (VersionException e) {
            //expected                                                                                        
        }
        commitTransaction();
    }

    @FixFor( "MODE-1822" )
    @Test
    public void shouldBeAbleToVersionWithinUserTransaction() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");

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
    public void shouldBeAbleToVersionWithinSequentialUserTransactions() throws Exception {
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");

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
    public void shouldBeAbleToVersionWithinImmediatelySequentialUserTransactions() throws Exception {
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");

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
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-atomikos.json");

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
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");
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

        try {
            for (Future<Void> result : results) {
                result.get(10, TimeUnit.SECONDS);
                result.cancel(true);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @FixFor( "MODE-2352 " )
    public void shouldSupportConcurrentWritersUpdatingTheSameNodeWithSeparateUserTransactions() throws Exception {
        FileUtil.delete("target/persistent_repository");
        startRepositoryWithConfigurationFrom("config/repo-config-new-workspaces.json");
        int threadsCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Future<Void>> results = new ArrayList<>(threadsCount);
        final AtomicInteger counter = new AtomicInteger(1);
        for (int i = 0; i < threadsCount; i++) {
            Future<Void> result = executorService.submit(() -> {
                startTransaction();
                Session session1 = repository.login();
                session1.getRootNode().addNode("test_" + counter.incrementAndGet());
                session1.save();
                session1.logout();
                commitTransaction();
                return null;
            });
            results.add(result);
        }

        try {
            for (Future<Void> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
            Session session = repository.login();
            // don't count jcr:system
            assertEquals(threadsCount, session.getNode("/").getNodes().getSize() - 1);
            session.logout();
        } finally {
            executorService.shutdownNow();
        }
    }

    @FixFor( "MODE-2371" )
    @Test
    public void shouldInitializeWorkspacesWithOngoingUserTransaction() throws Exception {
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");

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
    @FixFor( "MODE-2395 " )
    public void shouldSupportMultipleUpdatesFromTheSameSessionWithUserTransactions() throws Exception {
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");
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
        try {
            Future<Void> updaterResult = executorService.submit((Callable<Void>) () -> {
                JcrSession updater = repository.login();
                startTransaction();
                AbstractJcrNode node21 = updater.getNode("/node2");
                node21.setProperty("prop", "bar");
                updater.save();
                commitTransaction();
                updater.logout();
                return null;
            });
            updaterResult.get();
            node2 = mainSession.getNode("/node2");
            assertEquals("bar", node2.getProperty("prop").getString());
            mainSession.logout();
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @FixFor( "MODE-2495" )
    @Ignore( "ModeShape 5 requires thread confinement" )
    public void shouldSupportMultipleThreadsChangingTheSameUserTransaction() throws Exception {
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");

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
        Thread t5 = new Thread(() -> {
            try {
                resumeTransaction(longTx);
                VersionManager vm1 = session.getWorkspace().getVersionManager();
                vm1.checkin("/parent");
                commitTransaction();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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

    @Test
    @FixFor( "MODE-2558" )
    public void shouldNotCorruptDataWhenConcurrentlyWritingAndQuerying() throws Exception {
        int threadCount = 150;
        IntStream.range(0, threadCount).parallel().forEach(this::insertAndQueryNodes);
    }

    @Test
    @FixFor( "MODE-2607" )
    public void shouldUpdateParentAndRemoveChildWithDifferentTransactions1() throws Exception {
        final String parentPath = "/parent";
        final String childPath = "/parent/child";
 
        //create parent and child node with some properties in a tx
        startTransaction();
        Session session = newSession();
        Node parent = session.getRootNode().addNode("parent");
        parent.setProperty("foo", "parent");
        Node child = parent.addNode("child");
        child.setProperty("foo", "child");
        session.save();
        commitTransaction();

        //edit the parent and remove the child in a new tx
        startTransaction();
        session = newSession();
        parent = session.getNode(parentPath);
        parent.setProperty("foo", "bar2");
        session.save();
        
        child = session.getNode(childPath);
        child.remove();
        session.save();
        commitTransaction();

        //check that the editing worked in a new tx
        startTransaction();
        parent = session.getNode(parentPath);
        assertEquals("bar2", parent.getProperty("foo").getString());
        assertNoNode("/parent/child");
        commitTransaction();
    }

    @Test
    @FixFor( "MODE-2610" )
    public void shouldUpdateParentAndRemoveChildWithDifferentTransactions2() throws Exception {
        final String parentPath = "/parent";
        final String childPath = "/parent/child";
       
        startTransaction();
        Node parent = session.getRootNode().addNode("parent");
        parent.setProperty("foo", "parent");
        Node child = parent.addNode("child");
        child.setProperty("foo", "child");
        session.save();
        commitTransaction();

        startTransaction();
        child = session.getNode(childPath);
        parent = session.getNode(parentPath);
        parent.setProperty("foo", "bar2");
        session.save();
        child.remove();
        session.save();
        commitTransaction();

        startTransaction();
        parent = session.getNode(parentPath);
        assertEquals("bar2", parent.getProperty("foo").getString());   
        assertNoNode("/parent/child");
        session.logout();
        commitTransaction();
    }
    
    @FixFor( "MODE-2623" )
    @Test
    public void shouldAllowLockUnlockWithinTransaction() throws Exception {
        final String path = "/test";
        Node parent = session.getRootNode().addNode("test");
        parent.addMixin("mix:lockable");
        session.save();
        
        startTransaction();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        lockMgr.lock(path, true, true, Long.MAX_VALUE, session.getUserID());
        lockMgr.unlock(path);
        commitTransaction();
        
        assertFalse(session.getNode(path).isLocked());
    }
    
    @FixFor("MODE-2627")
    @Test
    public void shouldUpdateACLOnMovedNode() throws Exception {
        AccessControlManager acm = session.getAccessControlManager();
        final String childPath2 = "/parent/child2";
        final String childDestinationNode = "/parent/child/child2";
        JcrAccessControlList acl = new JcrAccessControlList(childDestinationNode);
        Privileges privileges = new Privileges(session);
        Privilege[] privilegeArray = new Privilege[] {
            privileges.forName(Privilege.JCR_READ), 
            privileges.forName(Privilege.JCR_WRITE), 
            privileges.forName(Privilege.JCR_READ_ACCESS_CONTROL)
        };
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"), privilegeArray);
        
        startTransaction();
        Node parent = session.getRootNode().addNode("parent");
        parent.addNode("child");
        parent.addNode("child2");
        session.save();
        commitTransaction();
        
        startTransaction();
        session.getWorkspace().move(childPath2, childDestinationNode);
        session.save();
        commitTransaction();
        
        startTransaction();
        acm.setPolicy(childDestinationNode, acl);
        session.save();
        Node movedNode = session.getNode(childDestinationNode);
        assertEquals(childDestinationNode, movedNode.getPath());
        assertEquals(1, acm.getPolicies(childDestinationNode).length);
        assertEquals(acm.getPolicies(childDestinationNode)[0], acl);
        assertNoNode(childPath2);
        session.logout();
        commitTransaction();
    }
    
    @Test
    @FixFor( "MODE-2642" )
    public void shouldLockNodeWithinTransaction() throws Exception {
        Node node = session.getRootNode().addNode("test");
        node.addMixin("mix:lockable");
        session.save();
        
        startTransaction();
        JcrLockManager lockManager = session.getWorkspace().getLockManager();
        Lock lock = lockManager.lock(node.getPath(), false, false, Long.MAX_VALUE, null);
        assertTrue(lock.isLive());
        assertTrue("Node should be locked", node.isLocked());
        commitTransaction();
        assertTrue(node.isLocked());
    }
    
    @Test
    @FixFor( "MODE-2668" )
    public void shouldRaiseExceptionAndRollbackIfTransactionFails1() throws Exception {
        session.getRootNode().addNode("parent");
        session.save();         
        Transaction tx = startTransaction();
        tx.rollback();
        AbstractJcrNode parent = session.getNode("/parent");
        parent.addNode("child");
        try {
            session.save();
            fail("Expected save operation to fail");
        } catch (RepositoryException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            session.refresh(false);
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    Transaction tx1 = startTransaction();
                    session.getNode("/parent").addNode("child");
                    session.save();
                    tx1.commit();
                    assertEquals(1, parent.getNodes().getSize());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } 
            }, executorService).get();
        } finally {
            transactionManager().suspend();
            executorService.shutdownNow();      
        }
    }
    
    @Test
    @FixFor( "MODE-2668" )
    public void shouldRaiseExceptionAndRollbackIfTransactionFails2() throws Exception {
        Node parent = session.getRootNode().addNode("parent");
        session.save();
        Transaction tx = startTransaction();
        // add one child
        parent.addNode("child1");
        session.save();
        // explicitly rollback
        tx.rollback();
        try {
            parent.addNode("child2");            
            session.save();
            fail("Expected save operation to fail");
        } catch (RepositoryException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            session.refresh(false);
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    Transaction tx1 = startTransaction();
                    parent.addNode("child1");
                    parent.addNode("child2");
                    session.save();
                    tx1.commit();
                    assertEquals(2, parent.getNodes().getSize());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService).get();
        } finally {
            transactionManager().suspend();
            executorService.shutdownNow();        
        }
    }
    
    @Test
    @FixFor("MODE-2670")
    public void shouldRollbackAndCommitTransactionsFromDifferentThreadsForFileProvider() throws Exception {
        // Start the repository using the JBoss Transactions transaction manager ...
        startRepositoryWithConfigurationFrom("config/repo-config-inmemory-txn.json");
        testCommitAndRollbackOffSeparateThreads();
    }  
    
    @Test
    @FixFor("MODE-2670")
    public void shouldRollbackAndCommitTransactionsFromDifferentThreadsForDBProvider() throws Exception {
        FileUtil.delete("target/txn");
        // Start the repository using the JBoss Transactions transaction manager ...
        startRepositoryWithConfigurationFrom("config/repo-config-db-txn.json");
        testCommitAndRollbackOffSeparateThreads();
    }
    
    private void testCommitAndRollbackOffSeparateThreads() throws Exception {
        session.getRootNode().addNode("parent");
        session.save();
        
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            transactionManager().begin();
            Transaction tx = transactionManager().getTransaction();
            assertNotNull(tx);
            session.getNode("/parent").addNode("child");
            session.save();
            executorService.submit(() -> {
                tx.rollback();
                return null;
            }).get(2, TimeUnit.SECONDS);
         
            //nothing should be there
            assertNoNode("/parent/child");
            // and the tx off thread 'main' should be aborted
            Transaction afterRollback = transactionManager().getTransaction();
            assertNotNull(afterRollback);
            assertEquals(Status.STATUS_ROLLEDBACK, afterRollback.getStatus());
            // remove it from the current thread
            transactionManager().suspend();
        
            //now make the same change and commit from a separate thread
            executorService.submit(() -> {
                transactionManager().begin();
                final Transaction tx1 = transactionManager().getTransaction();
                assertNotNull(tx1);
                assertEquals(Status.STATUS_ACTIVE, tx1.getStatus());
                session.getNode("/parent").addNode("child");
                session.save();
                executorService.submit(() -> {
                    tx1.commit();
                    return null;
                }).get(2, TimeUnit.SECONDS);
                // we've committed off a different thread, but the changes should be persisted
                try {
                    assertNode("/parent/child");
                    return null;
                } finally {
                    //remove it from the current thread
                    transactionManager().suspend();                    
                }
            }).get(2, TimeUnit.SECONDS);
            assertNode("/parent/child");
            assertNull(transactionManager().getTransaction());
        } catch (java.util.concurrent.TimeoutException te) {
            fail("Timeout detected; this means threads are not able to complete due to a lock starvation");
        }
        finally {
            executorService.shutdownNow();     
        }
    }
    
    private void insertAndQueryNodes(int i) {
        Session session = null;

        try {
            startTransaction();
            session = repository.login();
            createNode("/", UUID.randomUUID().toString(), session);
            commitTransaction();
            
            
            session = repository.login();
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("SELECT node.* FROM [mix:title] AS node", Query.JCR_SQL2);
            QueryResult result = query.execute();
            assertTrue(result.getNodes().getSize() > 0);
            session.logout();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if (session != null) {
                session.logout();
                session = null;
            }
        }
    }

    private Node createNode(String parentNodePath, String uuid, Session session) throws RepositoryException {

        String nodeLevelOneName = uuid.substring(0, 2);
        String nodeLevelOnePath = parentNodePath + "/" + nodeLevelOneName;

        String nodeLevelTwoName = uuid.substring(2, 4);
        String nodeLevelTwoPath = nodeLevelOnePath + "/" + nodeLevelTwoName;

        String nodeLevelThreeName = uuid.substring(4, 6);
        String nodeLevelThreePath = nodeLevelTwoPath + "/" + nodeLevelThreeName;

        addLevel(parentNodePath, nodeLevelOneName, nodeLevelOnePath, session);
        addLevel(nodeLevelOnePath, nodeLevelTwoName, nodeLevelTwoPath, session);
        addLevel(nodeLevelTwoPath, nodeLevelThreeName, nodeLevelThreePath, session);
        session.save();

        Node parent = session.getNode(parentNodePath);
        Node node = parent.addNode(uuid);
        node.addMixin("mix:title");
        node.setProperty("jcr:title", "test");
        session.save();

        return node;
    }

    private void addLevel(String currentNodeLevelPath, String nextNodeLevelName, String nextNodeLevelPath, Session session) throws RepositoryException {
        if (!session.nodeExists(nextNodeLevelPath)) {
            Node parentNode = session.getNode(currentNodeLevelPath);
            parentNode.addNode(nextNodeLevelName);
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

    protected Transaction startTransaction() throws NotSupportedException, SystemException {
        TransactionManager txnMgr = transactionManager();
        // Change this to true if/when debugging ...
        try {
            txnMgr.setTransactionTimeout(1000);
        } catch (Exception e) {
            // ignore
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

    private void moveDocument(String nodeName) throws Exception {
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
