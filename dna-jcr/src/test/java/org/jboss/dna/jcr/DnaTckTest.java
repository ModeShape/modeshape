package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.ConstraintViolationException;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Additional DNA tests that check for JCR compliance.
 */
public class DnaTckTest extends AbstractJCRTest {

    Session session;

    @Override
    protected void tearDown() throws Exception {
        try {
            superuser.getRootNode().getNode(this.nodeName1).remove();
            superuser.save();
        } catch (PathNotFoundException ignore) {
        }

        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    private void testRead( Session session ) throws Exception {
        Node rootNode = session.getRootNode();

        for (NodeIterator iter = rootNode.getNodes(); iter.hasNext();) {
            iter.nextNode();
        }
    }

    private void testAddNode( Session session ) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        root.addNode(nodeName1, testNodeType);
        session.save();
    }

    private void testRemoveNode( Session session ) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        Node node = root.getNode(nodeName1);
        node.remove();
        session.save();
    }

    private void testSetProperty( Session session ) throws Exception {
        session.refresh(false);
        Node root = session.getRootNode();
        root.setProperty(this.propertyName1, "test value");
        session.save();

    }

    private void testRemoveProperty( Session session ) throws Exception {
        Session localAdmin = helper.getRepository().login(helper.getSuperuserCredentials(), session.getWorkspace().getName());
        assertEquals(session.getWorkspace().getName(), superuser.getWorkspace().getName());

        Node superRoot = localAdmin.getRootNode();
        Node superNode;
        try {
            superNode = superRoot.getNode(this.nodeName1);
        } catch (PathNotFoundException pnfe) {
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

    private void testWrite( Session session ) throws Exception {
        testAddNode(session);
        testSetProperty(session);
        testRemoveProperty(session);
        testRemoveNode(session);
    }

    /**
     * Tests that read-only sessions can read nodes by loading all of the children of the root node
     * 
     * @throws Exception
     */
    public void testShouldAllowReadOnlySessionToRead() throws Exception {
        session = helper.getReadOnlySession();
        testRead(session);
    }

    /**
     * Tests that read-only sessions cannot add nodes, remove nodes, set nodes, or set properties.
     * 
     * @throws Exception
     */
    public void testShouldNotAllowReadOnlySessionToWrite() throws Exception {
        session = helper.getReadOnlySession();
        try {
            testAddNode(session);
            fail("Read-only sessions should not be able to add nodes");
        } catch (AccessDeniedException expected) {
        }
        try {
            testSetProperty(session);
            fail("Read-only sessions should not be able to set properties");
        } catch (AccessDeniedException expected) {
        }
        try {
            testRemoveProperty(session);
            fail("Read-only sessions should not be able to remove properties");
        } catch (AccessDeniedException expected) {
        }
        try {
            testRemoveNode(session);
            fail("Read-only sessions should not be able to remove nodes");
        } catch (AccessDeniedException expected) {
        }
    }

    /**
     * Tests that read-write sessions can read nodes by loading all of the children of the root node
     * 
     * @throws Exception
     */
    public void testShouldAllowReadWriteSessionToRead() throws Exception {
        session = helper.getReadWriteSession();
        testRead(session);
    }

    /**
     * Tests that read-write sessions can add nodes, remove nodes, set nodes, and set properties.
     * 
     * @throws Exception
     */
    public void testShouldAllowReadWriteSessionToWrite() throws Exception {
        session = helper.getReadWriteSession();
        testWrite(session);
    }

    /**
     * User defaultuser is configured to have readwrite in "otherWorkspace" and readonly in the default workspace. This test makes
     * sure both work.
     * 
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
        } catch (AccessDeniedException expected) {
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

    /**
     * A clone operation with removeExisting = true should fail if it would require removing an existing node that is a mandatory
     * child node of some other parent (and not replacing it as part of the clone operation).
     * 
     * @throws Exception if an error occurs
     */
    public void testShouldNotCloneIfItWouldViolateTypeSemantics() throws Exception {
        session = helper.getSuperuserSession("otherWorkspace");
        assertThat(session.getWorkspace().getName(), is("otherWorkspace"));

        String nodetype1 = this.getProperty("nodetype");
        Node node1 = session.getRootNode().addNode("cloneSource", nodetype1);
        // This node is not a mandatory child of nodetype1 (dnatest:referenceableUnstructured)
        node1.addNode("dnatest:mandatoryChild", nodetype1);

        session.save();
        session.logout();

        // /node4 in the default workspace is type dna:referenceableUnstructured
        superuser.getRootNode().addNode("cloneTarget", nodetype1);

        // /node3 in the default workspace is type dna:referenceableUnstructured
        superuser.getRootNode().addNode(nodeName3, nodetype1);
        superuser.save();

        // Throw the cloned items under node4
        superuser.getWorkspace().clone("otherWorkspace", "/cloneSource", "/cloneTarget/cloneSource", false);

        superuser.refresh(false);
        Node node3 = (Node)superuser.getItem("/node3");
        assertThat(node3.getNodes().getSize(), is(0L));

        Node node4node1 = (Node)superuser.getItem("/cloneTarget/cloneSource");
        assertThat(node4node1.getNodes().getSize(), is(1L));

        // Now clone from the same source under node3 and remove the existing records
        superuser.getWorkspace().clone("otherWorkspace", "/cloneSource", "/" + nodeName3 + "/cloneSource", true);
        superuser.refresh(false);

        Node node3node1 = (Node)superuser.getItem("/node3/cloneSource");
        assertThat(node3node1.getNodes().getSize(), is(1L));

        // Check that the nodes were indeed removed
        Node node4 = (Node)superuser.getItem("/cloneTarget");

        assertThat(node4.getNodes().getSize(), is(0L));

        superuser.getRootNode().addNode("nodeWithMandatoryChild", "dnatest:nodeWithMandatoryChild");
        try {
            superuser.save();
            fail("A node with type dnatest:nodeWithMandatoryChild should not be savable until the child is added");
        } catch (ConstraintViolationException cve) {
            // Expected
        }

        superuser.move("/node3/cloneSource/dnatest:mandatoryChild", "/nodeWithMandatoryChild/dnatest:mandatoryChild");
        superuser.save();
        superuser.refresh(false);

        // Now clone from the same source under node3 and remove the existing records
        try {
            superuser.getWorkspace().clone("otherWorkspace", "/cloneSource", "/" + nodeName3 + "/cloneSource", true);
            fail("Should not be able to use clone to remove the mandatory child node at /nodeWithMandatoryChild/dnatest:mandatoryChild");
        } catch (ConstraintViolationException cve) {
            // expected
        }

    }

}
