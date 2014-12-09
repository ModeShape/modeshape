package org.modeshape.test.integration;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.UUID;

/**
 * A stateless EJB that uses container managed transactions
 *
 * @author Richard Lucas
 */
@Stateless
public class StatelessCMTBean {

    @Inject
    Session session;

    /**
     * Creates a new node under the root node
     *
     * @return the newly created node
     * @throws javax.jcr.RepositoryException
     */
    public Node createNode() throws RepositoryException {
        String uuid = UUID.randomUUID().toString();
        Node root = session.getRootNode();
        Node node = root.addNode(uuid);
        node.setProperty("testProperty", "test");
        session.save();
        System.out.println("Created node " + node.getPath() + " with session " + session);
        return node;
    }

    /**
     * Updates the node at the given node path. Sets the 'testProperty' to test2. The update is performed asynchronously in a separate thread.
     *
     * @param nodePath
     *         the path of node being updated
     * @throws javax.jcr.RepositoryException
     */
    @Asynchronous
    public void update(String nodePath) throws RepositoryException {
        System.out.println("Updating node ...");

        Node node = session.getNode(nodePath);
        node.setProperty("testProperty", "test2");
        node.getSession().save();
        System.out.println(node);
        System.out.println("Updated node new testPropertyValue = " + node.getProperty("testProperty").getString());

    }
}
