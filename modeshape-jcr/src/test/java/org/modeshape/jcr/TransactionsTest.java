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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.junit.Test;

public class TransactionsTest extends SingleUseAbstractTest {

    private static final String MULTI_LINE_VALUE = "Line\t1\nLine 2\rLine 3\r\nLine 4";

    protected void initializeData() throws Exception {
        Node root = session.getRootNode();
        Node a = root.addNode("a");
        Node b = a.addNode("b");
        Node c = b.addNode("c");
        a.addMixin("mix:lockable");
        a.setProperty("stringProperty", "value");

        b.addMixin("mix:referenceable");
        b.setProperty("booleanProperty", true);

        c.setProperty("stringProperty", "value");
        c.setProperty("multiLineProperty", MULTI_LINE_VALUE);
        session.save();
    }

    @Test
    public void shouldBeAbleToMoveNodeWithinUserTransaction() throws Exception {
        startTransaction();
        moveDocument("childX");
        commitTransaction();
    }

    @Test
    public void shouldBeAbleToMoveNodeOutsideOfUserTransaction() throws Exception {
        moveDocument("childX");
    }

    @Test
    public void shouldBeAbleToUseSeparateSessionsWithinSingleUserTransaction() throws Exception {
        // We'll use separate threads, but we want to have them both do something specific at a given time,
        // so we'll use a barrier ...
        final CyclicBarrier barrier = new CyclicBarrier(2);

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
                    barrier.await(20, TimeUnit.SECONDS);

                    // Create a second session, which should NOT see the persisted-but-not-committed changes ...
                    session = newSession();
                    Node grandChild2 = session.getNode(path);
                    separateThreadNode.set(grandChild2);

                } catch (Exception err) {
                    separateThreadException.set(err);
                } finally {
                    if (session != null) session.logout();
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

        // Sync up with the other thread ...
        barrier.await();

        // Create a second session, which should see the persisted-but-not-committed changes ...
        Session session2 = newSession();
        Node grandChild2 = session2.getNode(path);
        assertThat(grandChild.isSame(grandChild2), is(true));
        session2.logout();

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

    protected void startTransaction() throws NotSupportedException, SystemException {
        session.getRepository().transactionManager().begin();
    }

    protected void commitTransaction()
        throws SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
        HeuristicRollbackException {
        session.getRepository().transactionManager().commit();
    }

    protected void rollbackTransaction() throws SystemException, SecurityException, IllegalStateException {
        session.getRepository().transactionManager().rollback();
    }

    public void moveDocument( String nodeName ) throws Exception {
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
