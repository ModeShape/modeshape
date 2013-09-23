package org.modeshape.jcr;

import java.util.Random;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionManager;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Panov
 * @date 9/23/13
 * @time 4:40 PM
 */

public class GetBaseVersionAfterNodeRemovalTest extends SingleUseAbstractTest {
    private VersionManager versionManager;

    @Before
    public void initializeVersionManager() throws Exception {
        versionManager = session.getWorkspace().getVersionManager();
    }

    @Test
    public void testName() throws Exception {
        Node node = createVersionedNode();

        createVersion(node);

        String identifier = node.getIdentifier();

        removeNode(node);

        AbstractJcrNode foundNode = session.getNodeByIdentifier(identifier);
        //exception happens here
        foundNode.getBaseVersion();
    }

    private Node createVersionedNode() throws RepositoryException {
        Node node = session.getRootNode().addNode("outerFolder");
        node.setProperty("jcr:mimeType", "text/plain");
        node.setProperty("jcr:data", "Original content");
        session.save();
        node.addMixin("mix:versionable");
        session.save();
        return node;
    }

    private void createVersion(Node node) throws RepositoryException {
        versionManager.checkout(node.getPath());
        node.setProperty("jcr:data", "Original content " + new Random().nextInt());
        session.save();
        versionManager.checkin(node.getPath());
    }

    private void removeNode(Node node) throws RepositoryException {
        node.remove();
        session.save();
    }
}
