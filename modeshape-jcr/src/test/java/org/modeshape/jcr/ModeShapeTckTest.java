package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Collections;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;

/**
 * Additional ModeShape tests that check for JCR compliance.
 */
public class ModeShapeTckTest extends AbstractJCRTest {

    Session session;

    public ModeShapeTckTest( String testName ) {
        super();

        this.setName(testName);
        this.isReadOnly = true;
    }

    public static Test readOnlySuite() {
        TestSuite suite = new TestSuite("ModeShape JCR API tests");

        suite.addTest(new ModeShapeTckTest("testShouldAllowAdminSessionToRead"));
        suite.addTest(new ModeShapeTckTest("testShouldAllowReadOnlySessionToRead"));
        suite.addTest(new ModeShapeTckTest("testShouldAllowReadWriteSessionToRead"));
        suite.addTest(new ModeShapeTckTest("testShouldNotSeeWorkspacesWithoutReadPermission"));
        suite.addTest(new ModeShapeTckTest("testShouldMapReadRolesToWorkspacesWhenSpecified"));

        return suite;
    }

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

    private void testRegisterNamespace( Session session ) throws Exception {
        String unusedPrefix = session.getUserID();
        session.getWorkspace().getNamespaceRegistry().registerNamespace(unusedPrefix, unusedPrefix);
        session.getWorkspace().getNamespaceRegistry().unregisterNamespace(unusedPrefix);
    }

    private void testRegisterType( Session session ) throws Exception {
        JcrNodeTypeManager nodeTypes = (JcrNodeTypeManager)session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate newType = nodeTypes.createNodeTypeTemplate();
        String nodeTypeName = session.getUserID() + "Type";
        newType.setName(nodeTypeName);
        nodeTypes.registerNodeType(newType, false);
        nodeTypes.unregisterNodeType(Collections.singleton(nodeTypeName));
    }

    private void testWrite( Session session ) throws Exception {
        testAddNode(session);
        testSetProperty(session);
        testRemoveProperty(session);
        testRemoveNode(session);
    }

    private void testAdmin( Session session ) throws Exception {
        testRegisterNamespace(session);
        testRegisterType(session);
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
     * Tests that read-only sessions cannot register namespaces or types
     * 
     * @throws Exception
     */
    public void testShouldNotAllowReadOnlySessionToAdmin() throws Exception {
        session = helper.getReadOnlySession();
        try {
            testRegisterNamespace(session);
            fail("Read-only sessions should not be able to register namespaces");
        } catch (AccessDeniedException expected) {
        }
        try {
            testRegisterType(session);
            fail("Read-only sessions should not be able to register types");
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
     * Tests that read-write sessions cannot register namespaces or types
     * 
     * @throws Exception
     */
    public void testShouldNotAllowReadWriteSessionToAdmin() throws Exception {
        session = helper.getReadWriteSession();
        try {
            testRegisterNamespace(session);
            fail("Read-write sessions should not be able to register namespaces");
        } catch (AccessDeniedException expected) {
        }
        try {
            testRegisterType(session);
            fail("Read-write sessions should not be able to register types");
        } catch (AccessDeniedException expected) {
        }
    }

    /**
     * Tests that admin sessions can read nodes by loading all of the children of the root node
     * 
     * @throws Exception
     */
    public void testShouldAllowAdminSessionToRead() throws Exception {
        session = helper.getSuperuserSession();
        testRead(session);
    }

    /**
     * Tests that admin sessions can add nodes, remove nodes, set nodes, and set properties.
     * 
     * @throws Exception
     */
    public void testShouldAllowAdminSessionToWrite() throws Exception {
        session = helper.getSuperuserSession();
        testWrite(session);
    }

    /**
     * Tests that admin sessions can register namespaces and types
     * 
     * @throws Exception
     */
    public void testShouldAllowAdminSessionToAdmin() throws Exception {
        session = helper.getSuperuserSession();
        testAdmin(session);
    }

    /**
     * User defaultuser is configured to have readwrite in "otherWorkspace" and readonly in the default workspace. This test makes
     * sure both work.
     * 
     * @throws Exception
     */
    public void testShouldMapReadRolesToWorkspacesWhenSpecified() throws Exception {
        Credentials creds = new SimpleCredentials("defaultonly", "defaultonly".toCharArray());
        session = helper.getRepository().login(creds);

        testRead(session);

        session.logout();

        // If the repo only supports one workspace, stop here
        if ("default".equals(this.workspaceName)) return;

        session = helper.getRepository().login(creds, this.workspaceName);
        testRead(session);
        try {
            testWrite(session);
            fail("User 'defaultuser' should not have write access to '" + this.workspaceName + "'");
        } catch (AccessDeniedException expected) {
        }
        session.logout();
    }

    /**
     * User defaultuser is configured to have readwrite in "otherWorkspace" and readonly in the default workspace. This test makes
     * sure both work.
     * 
     * @throws Exception
     */
    public void testShouldMapWriteRolesToWorkspacesWhenSpecified() throws Exception {
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

    /**
     * Users should not be able to see workspaces to which they don't at least have read access. User 'noaccess' has no access to
     * the default workspace.
     * 
     * @throws Exception
     */
    public void testShouldNotSeeWorkspacesWithoutReadPermission() throws Exception {
        Credentials creds = new SimpleCredentials("noaccess", "noaccess".toCharArray());

        try {
            session = helper.getRepository().login(creds);
            fail("User 'noaccess' with no access to the default workspace should not be able to log into that workspace");
        } catch (LoginException le) {
            // Expected
        }

        // If the repo only supports one workspace, stop here
        if ("default".equals(this.workspaceName)) return;

        session = helper.getRepository().login(creds, this.workspaceName);

        String[] workspaceNames = session.getWorkspace().getAccessibleWorkspaceNames();

        assertThat(workspaceNames.length, is(1));
        assertThat(workspaceNames[0], is(this.workspaceName));

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
        // This node is not a mandatory child of nodetype1 (modetest:referenceableUnstructured)
        node1.addNode("modetest:mandatoryChild", nodetype1);

        session.save();
        session.logout();

        // /cloneTarget in the default workspace is type mode:referenceableUnstructured
        superuser.getRootNode().addNode("cloneTarget", nodetype1);

        // /node3 in the default workspace is type mode:referenceableUnstructured
        superuser.getRootNode().addNode(nodeName3, nodetype1);
        superuser.save();

        // Throw the cloned items under cloneTarget
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

        superuser.getRootNode().addNode("nodeWithMandatoryChild", "modetest:nodeWithMandatoryChild");
        try {
            superuser.save();
            fail("A node with type modetest:nodeWithMandatoryChild should not be savable until the child is added");
        } catch (ConstraintViolationException cve) {
            // Expected
        }

        superuser.move("/node3/cloneSource/modetest:mandatoryChild", "/nodeWithMandatoryChild/modetest:mandatoryChild");
        superuser.save();
        superuser.refresh(false);

        // Now clone from the same source under node3 and remove the existing records
        try {
            superuser.getWorkspace().clone("otherWorkspace", "/cloneSource", "/" + nodeName3 + "/cloneSource", true);
            fail("Should not be able to use clone to remove the mandatory child node at /nodeWithMandatoryChild/modetest:mandatoryChild");
        } catch (ConstraintViolationException cve) {
            // expected
        }

    }

    public void testAdminUserCanBreakOthersLocks() throws Exception {
        String lockNodeName = "lockTestNode";
        session = helper.getReadWriteSession();
        Node root = session.getRootNode();
        Node lockNode = root.addNode(lockNodeName);
        lockNode.addMixin("mix:lockable");
        session.save();

        lockNode.lock(false, false);
        assertThat(lockNode.isLocked(), is(true));

        Session superuser = helper.getSuperuserSession();
        root = superuser.getRootNode();
        lockNode = root.getNode(lockNodeName);

        assertThat(lockNode.isLocked(), is(true));
        lockNode.unlock();
        assertThat(lockNode.isLocked(), is(false));
        superuser.logout();

    }

    public void testShouldCreateProperVersionHistoryWhenSavingVersionedNode() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/test", "nt:unstructured");
        node.addMixin("mix:versionable");
        session.save();

        assertThat(node.hasProperty("jcr:isCheckedOut"), is(true));
        assertThat(node.getProperty("jcr:isCheckedOut").getBoolean(), is(true));

        assertThat(node.hasProperty("jcr:versionHistory"), is(true));
        Node history = node.getProperty("jcr:versionHistory").getNode();
        assertThat(history, is(notNullValue()));

        assertThat(node.hasProperty("jcr:baseVersion"), is(true));
        Node version = node.getProperty("jcr:baseVersion").getNode();
        assertThat(version, is(notNullValue()));

        assertThat(version.getParent(), is(history));

        assertThat(node.hasProperty("jcr:uuid"), is(true));
        assertThat(node.getProperty("jcr:uuid").getString(), is(history.getProperty("jcr:versionableUuid").getString()));

        assertThat(node.getVersionHistory().getUUID(), is(history.getUUID()));
        assertThat(node.getVersionHistory().getPath(), is(history.getPath()));

        assertThat(node.getBaseVersion().getUUID(), is(version.getUUID()));
        assertThat(node.getBaseVersion().getPath(), is(version.getPath()));
    }

    public void testShouldCreateProperStructureForPropertiesOnTheFirstCheckInOfANode() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.getRootNode().save();

        node.addMixin("mix:versionable");
        node.save();

        node.setProperty("abortProp", "abortPropValue");
        node.setProperty("copyProp", "copyPropValue");
        node.setProperty("ignoreProp", "ignorePropValue");
        node.setProperty("versionProp", "versionPropValue");

        node.save();

        try {
            node.checkin();
            fail("Should not be able to checkin a node with a property that has an OnParentVersionAction of ABORT");
        } catch (VersionException ve) {
            assertThat(node.getProperty("jcr:isCheckedOut").getBoolean(), is(true));
        }

        node.setProperty("abortProp", (String)null);
        node.save();

        node.checkin();
        assertThat(node.getProperty("jcr:isCheckedOut").getBoolean(), is(false));

        Version version = node.getBaseVersion();
        assertThat(version, is(notNullValue()));
        assertThat(version.getProperty("jcr:frozenNode/copyProp").getString(), is("copyPropValue"));
        assertThat(version.getProperty("jcr:frozenNode/versionProp").getString(), is("versionPropValue"));

        try {
            version.getProperty("jcr:frozenNode/ignoreProp");
            fail("Frozen version should not record a property that has an OnParentVersionAction of IGNORE");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        node.checkout();

        node.setProperty("abortProp", "abortPropValueNew");
        node.setProperty("copyProp", "copyPropValueNew");
        node.setProperty("ignoreProp", "ignorePropValueNew");
        node.setProperty("versionProp", "versionPropValueNew");

        version = node.getBaseVersion();
        assertThat(version, is(notNullValue()));
        assertThat(version.getProperty("jcr:frozenNode/copyProp").getString(), is("copyPropValue"));
        assertThat(version.getProperty("jcr:frozenNode/versionProp").getString(), is("versionPropValue"));

        try {
            version.getProperty("ignoreProp");
            fail("Frozen version should not record a property that has an OnParentVersionAction of IGNORE");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        node.save();

    }

    public void testShouldCreateProperHistoryForNodeWithCopySemantics() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.getRootNode().save();

        /*
         * Create /checkinTest/copyNode/AbortNode with copyNode being versionable.  This should be able 
         * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
         */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");

        Node abortNode = copyNode.addNode("abortNode", "modetest:versionTest");
        abortNode.setProperty("ignoreProp", "ignorePropValue");
        abortNode.setProperty("copyProp", "copyPropValue");

        /*
         * Create /checkinTest/copyNode/versionNode with versionNode being versionable as well.  This should
         * create a copy of versionNode in the version history, due to copyNode (the root of the checkin) having
         * COPY semantics for the OnParentVersionAction.
         */

        Node versionNode = copyNode.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        node.save();

        Version version = copyNode.checkin();

        assertThat(version.getProperty("jcr:frozenNode/versionNode/jcr:primaryType").getString(), is("nt:frozenNode"));
        assertThat(version.getProperty("jcr:frozenNode/versionNode/jcr:frozenPrimaryType").getString(),
                   is("modetest:versionTest"));
        assertThat(version.getProperty("jcr:frozenNode/abortNode/copyProp").getString(), is("copyPropValue"));
        try {
            version.getProperty("jcr:frozenNode/abortNode/ignoreProp");
            fail("Property with OnParentVersionAction of IGNORE should not have been copied");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
    }

    public void testShouldIgnoreAbortSemanticsOfChildNode() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.save();

        /*
         * Create /checkinTest/versionNode/abortNode with versionNode being versionable.  This should not fail
         * when versionNode is checked in, as the OnParentVersionAction semantics come from the checked-in node.
         */

        Node versionNode = node.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        Node abortNode = versionNode.addNode("abortNode", "modetest:versionTest");
        abortNode.setProperty("ignoreProp", "ignorePropValue");
        abortNode.setProperty("copyProp", "copyPropValue");

        node.save();

        Version version = versionNode.checkin();

        assertThat(version.getProperty("jcr:frozenNode/abortNode/jcr:primaryType").getString(), is("nt:frozenNode"));
        assertThat(version.getProperty("jcr:frozenNode/abortNode/jcr:frozenPrimaryType").getString(), is("modetest:versionTest"));
        assertThat(version.getProperty("jcr:frozenNode/abortNode/copyProp").getString(), is("copyPropValue"));
        try {
            version.getProperty("jcr:frozenNode/abortNode/ignoreProp");
            fail("Property with OnParentVersionAction of IGNORE should not have been copied");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
    }

    public void testShouldCreateProperHistoryForVersionableChildOfNodeWithVersionSemantics() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.save();

        /*
         * Create /checkinTest/versionNode/copyNode with versionNode and copyNode being versionable.  This should
         * create a child of type nt:childVersionedNode under the frozen node.
         */

        Node versionNode = node.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        Node copyNode = versionNode.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("copyProp", "copyPropValue");

        node.save();

        Version version = versionNode.checkin();

        assertThat(version.getProperty("jcr:frozenNode/copyNode/jcr:primaryType").getString(), is("nt:versionedChild"));
        try {
            version.getProperty("jcr:frozenNode/copyNode/copyProp");
            fail("Property should not be copied to versionable child of versioned node");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        try {
            version.getProperty("jcr:frozenNode/copyNode/ignoreProp");
            fail("Property should not be copied to versionable child of versioned node");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        String childUuid = version.getProperty("jcr:frozenNode/copyNode/jcr:childVersionHistory").getString();
        Node childNode = session.getNodeByUUID(childUuid);

        Node rootNode = childNode.getNode("jcr:rootVersion");

        assertThat(rootNode.getProperty("jcr:frozenNode/jcr:frozenPrimaryType").getString(), is("modetest:versionTest"));
    }

    public void testShouldRestorePropertiesOnVersionableNode() throws Exception {
        session = helper.getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.getRootNode().save();

        /*
         * Create /checkinTest/copyNode with copyNode being versionable.  This should be able 
         * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
         */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        node.save();

        Version version = copyNode.checkin();

        /*
         * Make some changes
         */
        copyNode.checkout();
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        copyNode.save();
        copyNode.checkin();

        copyNode.restore(version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

    }

    public void testShouldAllowDeletingNodesWhenLargePropertyIsPresent() throws Exception {
        // q.v. MODE-693
        session = helper.getReadWriteSession();

        Node root = session.getRootNode();

        final int SIZE = 2048;
        byte[] largeArray = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            largeArray[i] = (byte)'x';
        }

        Node projectNode = root.addNode("mode693", "nt:unstructured");

        Node fileNode = projectNode.addNode("fileNode", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream(largeArray));
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        contentNode.setProperty("jcr:mimeType", "application/octet-stream");

        Node otherNode = projectNode.addNode("otherNode", "nt:unstructured");
        session.save();

        String pathToNode = projectNode.getName() + "/" + otherNode.getName();

        if (!root.hasNode(pathToNode)) {
            throw new RepositoryException("Cannot delete node at path=" + pathToNode + " since no node exists at this path");
        }
        Node nodeToDelete = root.getNode(pathToNode);
        nodeToDelete.remove();
        session.save();

        // Make sure that only one node was deleted
        assertThat(root.hasNode(projectNode.getName()), is(true));
        assertThat(projectNode.hasNode(fileNode.getName()), is(true));
        assertThat(fileNode.hasNode(contentNode.getName()), is(true));
        assertThat(root.hasNode(pathToNode), is(false));
    }
}
