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

package org.modeshape.test.integration;

import java.util.LinkedList;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.transaction.UserTransaction;
import org.jboss.logging.Logger;
import org.modeshape.jcr.api.JcrTools;

/**
 * A stateless EJB that accesses a Repository using multiple bean-managed transactions (BMT) and creates a JCR Session a variety
 * of ways.
 */
@Stateless
@TransactionManagement( TransactionManagementType.BEAN )
public class StatelessBeanManagedTransactionBean {

    private static Logger log = Logger.getLogger(StatelessBeanManagedTransactionBean.class);

    @Resource( mappedName = "java:/jcr/artifacts" )
    private Repository repository;

    @Resource
    private UserTransaction utxn;

    /** Stateless utility */
    private final JcrTools tools = new JcrTools();

    /**
     * Create a 3-level tree of nodes under the node at the specified path.
     * 
     * @param path the path of the node under which the tree should be created; this node will be created if missing
     * @param numNodesAtEachLevel
     * @param print flag indicating whether the status should be logged as INFO-level messages
     * @throws Exception
     */
    public void createNodes( String path,
                             int numNodesAtEachLevel,
                             boolean print ) throws Exception {
        if (path == null || path.trim().length() == 0) path = "/";
        utxn.begin();
        try {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            try {

                // Find or create the node at the specified path ...
                Node topNode = tools.findOrCreateNode(session, path, NodeType.NT_UNSTRUCTURED);

                for (int i = 1; i <= numNodesAtEachLevel; i++) {
                    Node node = topNode.addNode("node" + i, NodeType.NT_UNSTRUCTURED);
                    node.setProperty("prop", "Property value for " + node.getIdentifier());
                    if (print) log.info("Populating " + node.getPath());
                    for (int j = 1; j <= numNodesAtEachLevel; j++) {
                        Node child = node.addNode("child" + j, NodeType.NT_UNSTRUCTURED);
                        child.setProperty("prop", "Property value for " + child.getIdentifier());
                        if (print) log.info("Populating " + child.getPath());
                        for (int k = 1; k <= numNodesAtEachLevel; k++) {
                            Node grandChild = child.addNode("grandchild" + k, NodeType.NT_UNSTRUCTURED);
                            grandChild.setProperty("prop", "Property value for " + grandChild.getIdentifier());
                        }
                    }
                    if (i != numNodesAtEachLevel) {
                        if (print) log.info("Saving session");
                        session.save();
                        if (print) log.info("Committing transaction");
                        utxn.commit();
                        utxn.begin();
                    }
                }

                if (print) log.info("Saving session");
                session.save();

            } finally {
                session.logout();
            }
        } finally {
            if (print) log.info("Committing transaction");
            utxn.commit();
        }

        if (print) log.info("CreateNodesTest completed");
    }

    /**
     * Verify a 3-level tree of nodes under the node at the specified path.
     * 
     * @param path the path of the node under which the tree should be created; this node will be created if missing
     * @param numNodesAtEachLevel
     * @param print flag indicating whether the status should be logged as INFO-level messages
     * @throws Exception
     */
    public void verifyNodesInTransaction( String path,
                                          int numNodesAtEachLevel,
                                          boolean print ) throws Exception {
        if (path == null || path.trim().length() == 0) path = "/";
        utxn.begin();
        try {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            try {

                // Find or create the node at the specified path ...
                Node topNode = session.getNode(path);

                for (int i = 1; i <= numNodesAtEachLevel; i++) {
                    Node node = topNode.getNode("node" + i);
                    if (print) log.info("Verifying " + node.getPath());
                    node.getProperty("prop").getString().equals("Property value for " + node.getIdentifier());
                    for (int j = 1; j <= numNodesAtEachLevel; j++) {
                        Node child = node.getNode("child" + j);
                        if (print) log.info("Verifying " + child.getPath());
                        child.getProperty("prop").getString().equals("Property value for " + node.getIdentifier());
                        for (int k = 1; k <= numNodesAtEachLevel; k++) {
                            Node grandChild = child.getNode("grandchild" + k);
                            if (print) log.info("Verifying " + grandChild.getPath());
                            grandChild.getProperty("prop").getString().equals("Property value for " + node.getIdentifier());
                        }
                    }
                }
            } finally {
                if (session != null) session.logout();
            }
        } finally {
            utxn.commit();
        }
    }

    public void cleanup( String path,
                         boolean removeChildrenOnly ) throws Exception {
        utxn.begin();
        try {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            try {
                Node topNode = session.getNode(path);

                if (removeChildrenOnly) {
                    // Record (in reverse order) all the children from the top-level node ...
                    LinkedList<Node> children = new LinkedList<Node>();
                    NodeIterator iter = topNode.getNodes("node*");
                    while (iter.hasNext()) {
                        Node child = iter.nextNode();
                        children.addFirst(child);
                    }

                    // Now remove all of the children (in reverse order) ...
                    for (Node child : children) {
                        child.remove();
                    }
                } else {
                    topNode.remove();
                }

                session.save();
            } finally {
                session.logout();
            }
        } finally {
            utxn.commit();
        }
    }
}
