package org.jboss.dna.jcr;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Additional DNA tests that check for JCR compliance.
 *
 */
public class DnaTckTest extends AbstractJCRTest {

    Session session;

    protected void tearDown() throws Exception {
        try {
            superuser.getRootNode().getNode(this.nodeName1).remove();
            superuser.save();
        }
        catch (PathNotFoundException ignore) { } 
        
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    private void testRead(Session session) throws Exception {
        Node rootNode = session.getRootNode();
        
        for (NodeIterator iter = rootNode.getNodes(); iter.hasNext(); ) {
            iter.nextNode();
        }
    }
    
    private void testAddNode(Session session) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        root.addNode(nodeName1, testNodeType);
        session.save();
    }
    
    private void testRemoveNode(Session session) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        Node node = root.getNode(nodeName1);
        node.remove();
        session.save();
    }

    private void testSetProperty(Session session) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        root.setProperty(this.propertyName1, "test value");
        session.save();
        
    }
    
    private void testRemoveProperty(Session session) throws Exception {
        Session localAdmin = helper.getRepository().login(helper.getSuperuserCredentials(), session.getWorkspace().getName());
        assertEquals(session.getWorkspace().getName(), superuser.getWorkspace().getName());
        
        Node superRoot = localAdmin.getRootNode();
        Node superNode;
        try {
            superNode = superRoot.getNode(this.nodeName1);
        }
        catch (PathNotFoundException pnfe) {
            superNode = superRoot.addNode(nodeName1, testNodeType);
        }
        superNode.setProperty(this.propertyName1, "test value");

        localAdmin.save();
        localAdmin.logout();
        
        session.refresh(false);
        Node root = session.getRootNode();
        Node node = root.getNode(nodeName1);
        Property property = node.getProperty(this.propertyName1);
        property.remove();
        session.save();
    }
    
    private void testWrite(Session session) throws Exception {
        testAddNode(session);
        testSetProperty(session);
        testRemoveProperty(session);
        testRemoveNode(session);
    }

    /**
     * Tests that read-only sessions can read nodes by loading all of the children of the root node
     * @throws Exception
     */
    public void testShouldAllowReadOnlySessionToRead() throws Exception {
        session = helper.getReadOnlySession();
        testRead(session);
    }

    /**
     * Tests that read-only sessions cannot add nodes, remove nodes, set nodes, or set properties.
     * @throws Exception
     */
    public void testShouldNotAllowReadOnlySessionToWrite() throws Exception {
        session = helper.getReadOnlySession();
        try {
            testAddNode(session);
            fail("Read-only sessions should not be able to add nodes");
        }
        catch (AccessDeniedException expected) {
        }
        try {
            testSetProperty(session);
            fail("Read-only sessions should not be able to set properties");
        }
        catch (AccessDeniedException expected) {
        }
        try {
            testRemoveProperty(session);
            fail("Read-only sessions should not be able to remove properties");
        }
        catch (AccessDeniedException expected) {
        }
        try {
            testRemoveNode(session);
            fail("Read-only sessions should not be able to remove nodes");
        }
        catch (AccessDeniedException expected) {
        }
    }
    
    /**
     * Tests that read-write sessions can read nodes by loading all of the children of the root node
     * @throws Exception
     */
    public void testShouldAllowReadWriteSessionToRead() throws Exception {
        session = helper.getReadWriteSession();
        testRead(session);
    }

    /**
     * Tests that read-write sessions can add nodes, remove nodes, set nodes, and set properties.
     * @throws Exception
     */
    public void testShouldAllowReadWriteSessionToWrite() throws Exception {
        session = helper.getReadWriteSession();
        testWrite(session);
    }

    /**
     * User defaultuser is configured to have readwrite in "otherWorkspace" and readonly in the default
     * workspace.  This test makes sure both work.
     * @throws Exception
     */
    public void testShouldMapRolesToWorkspacesWhenSpecified() throws Exception {
        Credentials creds = new SimpleCredentials("defaultonly", "defaultonly".toCharArray());
        session = helper.getRepository().login(creds);

        testRead(session);
        testWrite(session);
        
        session.logout();
        
        session = helper.getRepository().login(creds, "otherWorkspace");
        testRead(session);
        try {
            testWrite(session);
            fail("User 'defaultuser' should not have write access to 'otherWorkspace'");
        }
        catch (AccessDeniedException expected) {
        }
        session.logout();
    }


    public void testShouldCopyFromAnotherWorkspace() throws Exception {
        session = helper.getSuperuserSession("otherWorkspace");
        String nodetype1 = this.getProperty("nodetype");
        Node node1 = session.getRootNode().addNode(nodeName1, nodetype1);
        node1.addNode(nodeName2, nodetype1);
        session.save();
        session.logout();
        
        superuser.getRootNode().addNode(nodeName4, nodetype1);
        superuser.save();
        
        superuser.getWorkspace().copy("otherWorkspace", "/" + nodeName1, "/" + nodeName4 + "/" + nodeName1);
        
        Node node4 = superuser.getRootNode().getNode(nodeName4);
        Node node4node1 = node4.getNode(nodeName1);
        Node node4node1node2 = node4node1.getNode(nodeName2);
        
        assertNotNull(node4node1node2);
    }
}
