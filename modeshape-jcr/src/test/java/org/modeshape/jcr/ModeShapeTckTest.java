package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.JcrTools;

/**
 * Additional ModeShape tests that check for JCR compliance.
 */
public class ModeShapeTckTest extends AbstractJCRTest {

    Session session;
    private Map<String, Node> testAreasByWorkspace = new HashMap<String, Node>();

    public ModeShapeTckTest( String testName ) {
        super();

        this.setName(testName);
        this.isReadOnly = true;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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

        Credentials creds = getHelper().getSuperuserCredentials();
        Repository repository = getHelper().getRepository();
        for (Map.Entry<String, Node> entry : testAreasByWorkspace.entrySet()) {
            Session session = repository.login(creds, entry.getKey());
            try {
                Node node = session.getNode(entry.getValue().getPath());
                node.remove();
                session.save();
            } catch (RepositoryException e) {
                // ignore ...
            } finally {
                session.logout();
            }
        }

        // Force a new repository instance for each test ...
        // ModeShapeRepositoryStub.reloadRepositoryInstance();
    }

    protected Node getTestRoot( Session session ) throws Exception {
        // Create a temporary area for the test ...
        String workspaceName = session.getWorkspace().getName();
        Node node = testAreasByWorkspace.get(workspaceName);
        if (node == null) {
            node = session.getRootNode().addNode("tmp");
            testAreasByWorkspace.put(workspaceName, node);
        } else {
            // There is a node, but check which session it came from ...
            if (node.getSession() != session) {
                // Look it up in the supplied session ...
                node = session.getRootNode().getNode("tmp");
            }
        }
        return node;
    }

    private void testRead( Session session ) throws Exception {
        Node rootNode = session.getRootNode();

        for (NodeIterator iter = rootNode.getNodes(); iter.hasNext();) {
            iter.nextNode();
        }
    }

    private void testAddNode( Session session ) throws Exception {
        session.refresh(false);
        Node root = getTestRoot(session);
        root.addNode(nodeName1, testNodeType);
        session.save();
    }

    private void testRemoveNode( Session session ) throws Exception {
        session.refresh(false);
        Node root = getTestRoot(session);
        Node node = root.getNode(nodeName1);
        node.remove();
        session.save();
    }

    private void testSetProperty( Session session ) throws Exception {
        session.refresh(false);
        Node root = getTestRoot(session);
        root.setProperty(this.propertyName1, "test value");
        session.save();

    }

    private void testRemoveProperty( Session session ) throws Exception {
        Session localAdmin = getHelper().getRepository().login(getHelper().getSuperuserCredentials(),
                                                               session.getWorkspace().getName());

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
        nodeTypes.unregisterNodeTypes(Collections.singleton(nodeTypeName));
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

    protected boolean useDeprecatedApi() {
        return false;
    }

    @SuppressWarnings( "deprecation" )
    protected VersionHistory versionHistory( Node node ) throws RepositoryException {
        if (useDeprecatedApi()) return node.getVersionHistory();
        return session.getWorkspace().getVersionManager().getVersionHistory(node.getPath());
    }

    @SuppressWarnings( "deprecation" )
    protected Version baseVersion( Node node ) throws RepositoryException {
        if (useDeprecatedApi()) return node.getBaseVersion();
        return session.getWorkspace().getVersionManager().getBaseVersion(node.getPath());
    }

    @SuppressWarnings( "deprecation" )
    protected Version checkin( Node node ) throws RepositoryException {
        if (useDeprecatedApi()) return node.checkin();
        return session.getWorkspace().getVersionManager().checkin(node.getPath());
    }

    @SuppressWarnings( "deprecation" )
    protected void checkout( Node node ) throws RepositoryException {
        if (useDeprecatedApi()) {
            node.checkout();
        } else {
            session.getWorkspace().getVersionManager().checkout(node.getPath());
        }
    }

    @SuppressWarnings( "deprecation" )
    protected void restore( Node node,
                            Version version,
                            boolean removeExisting ) throws RepositoryException {
        if (useDeprecatedApi()) {
            node.restore(version, removeExisting);
        } else {
            session.getWorkspace().getVersionManager().restore(version, removeExisting);
        }
    }

    @SuppressWarnings( "deprecation" )
    protected void lock( Node node,
                         boolean isDeep,
                         boolean isSessionScoped ) throws RepositoryException {
        if (useDeprecatedApi()) {
            node.lock(isDeep, isSessionScoped);
        } else {
            session.getWorkspace().getLockManager().lock(node.getPath(), isDeep, isSessionScoped, 1L, "owner");
        }
    }

    protected void printSubgraph( Node node ) throws RepositoryException {
        JcrTools tools = new JcrTools();
        tools.printSubgraph(node);
    }

    protected void printVersionHistory( Node node ) throws RepositoryException {
        VersionHistory history = session.getWorkspace().getVersionManager().getVersionHistory(node.getPath());
        printSubgraph(history);
    }

    /**
     * Tests that read-only sessions can read nodes by loading all of the children of the root node
     * 
     * @throws Exception
     */
    public void testShouldAllowReadOnlySessionToRead() throws Exception {
        session = getHelper().getReadOnlySession();
        testRead(session);
    }

    /**
     * Tests that read-only sessions cannot add nodes, remove nodes, set nodes, or set properties.
     * 
     * @throws Exception
     */
    public void testShouldNotAllowReadOnlySessionToWrite() throws Exception {
        session = getHelper().getReadOnlySession();
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
        session = getHelper().getReadOnlySession();
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
        session = getHelper().getReadWriteSession();
        testRead(session);
    }

    /**
     * Tests that read-write sessions can add nodes, remove nodes, set nodes, and set properties.
     * 
     * @throws Exception
     */
    public void testShouldAllowReadWriteSessionToWrite() throws Exception {
        session = getHelper().getReadWriteSession();
        testWrite(session);
    }

    /**
     * Tests that read-write sessions cannot register namespaces or types
     * 
     * @throws Exception
     */
    public void testShouldNotAllowReadWriteSessionToAdmin() throws Exception {
        session = getHelper().getReadWriteSession();
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
        session = getHelper().getSuperuserSession();
        testRead(session);
    }

    /**
     * Tests that admin sessions can add nodes, remove nodes, set nodes, and set properties.
     * 
     * @throws Exception
     */
    public void testShouldAllowAdminSessionToWrite() throws Exception {
        session = getHelper().getSuperuserSession();
        testWrite(session);
    }

    /**
     * Tests that admin sessions can register namespaces and types
     * 
     * @throws Exception
     */
    public void testShouldAllowAdminSessionToAdmin() throws Exception {
        session = getHelper().getSuperuserSession();
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
        session = getHelper().getRepository().login(creds);

        testRead(session);

        session.logout();

        // If the repo only supports one workspace, stop here
        if ("default".equals(this.workspaceName)) return;

        session = getHelper().getRepository().login(creds, this.workspaceName);
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
        session = getHelper().getRepository().login(creds);

        testRead(session);
        testWrite(session);

        session.logout();

        session = getHelper().getRepository().login(creds, "otherWorkspace");
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
            session = getHelper().getRepository().login(creds);
            fail("User 'noaccess' with no access to the default workspace should not be able to log into that workspace");
        } catch (LoginException le) {
            // Expected
        }

        // If the repo only supports one workspace, stop here
        if ("default".equals(this.workspaceName)) return;

        session = getHelper().getRepository().login(creds, this.workspaceName);

        String[] workspaceNames = session.getWorkspace().getAccessibleWorkspaceNames();

        assertThat(workspaceNames.length, is(1));
        assertThat(workspaceNames[0], is(this.workspaceName));

        session.logout();
    }

    public void testShouldCopyFromAnotherWorkspace() throws Exception {
        session = getHelper().getSuperuserSession("otherWorkspace");
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
    public void IGNOREtestShouldNotCloneIfItWouldViolateTypeSemantics() throws Exception {
        // TODO: Copy and clone
        session = getHelper().getSuperuserSession("otherWorkspace");
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

    private void ensureExactlyOneVersionRoot( VersionManager vm,
                                              String absPath ) throws Exception {
        boolean foundRoot = false;
        VersionHistory vh = vm.getVersionHistory(absPath);
        VersionIterator vi = vh.getAllVersions();

        while (vi.hasNext()) {
            Version v = vi.nextVersion();

            if ("jcr:rootVersion".equals(v.getName())) {
                if (foundRoot) {
                    fail("Found multiple root versions of versionable node at " + absPath);
                }
                foundRoot = true;
            }
        }

        if (!foundRoot) fail("No root version found for versionable node at " + absPath);

    }

    @FixFor( "MODE-1017" )
    public void testShouldNotHaveTwoRootVersions() throws Exception {
        Session session = superuser;
        ValueFactory vf = session.getValueFactory();

        Node root = session.getRootNode();
        Node file = root.addNode("createfile.mode", "nt:file");

        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));
        session.save();

        file.addMixin("mix:versionable");

        // Per Section 15.1:
        // "Under both simple and full versioning, on persist of a new versionable node N that neither corresponds
        // nor shares with an existing node:
        // - The jcr:isCheckedOut property of N is set to true and
        // - A new VersionHistory (H) is created for N. H contains one Version, the root version (V0)
        // (see §3.13.5.2 Root Version)."
        //
        // This means that the version history should not be created until save is performed. This makes sense,
        // because otherwise the version history would be persisted for a newly-created node, even though that node
        // is not yet persisted. Tests with the reference implementation (see sandbox) verified this behavior.
        try {
            session.getWorkspace().getVersionManager().getVersionHistory(file.getPath());
            fail("It should not be possible to obtain the version history for a transient node.");
        } catch (UnsupportedRepositoryOperationException e) {
            // this exception is expected
        }

        session.save();
        ensureExactlyOneVersionRoot(session.getWorkspace().getVersionManager(), file.getPath());
        session.refresh(false);
        ensureExactlyOneVersionRoot(session.getWorkspace().getVersionManager(), file.getPath());
    }

    public void testShouldFailToGetVersionHistoryOfTransientNode() throws Exception {
        Session session = superuser;
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node root = session.getRootNode();
        Node file = root.addNode("createfile2.mode", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));

        // Do not save the node ...
        // session.save();

        // Now add the mixin transiently ...
        file.addMixin("mix:versionable");

        try {
            vm.getVersionHistory(file.getPath());
            fail("It should not be possible to obtain the version history for a transient node.");
        } catch (InvalidItemStateException e) {
            // expected
        }

        session.save();
        ensureExactlyOneVersionRoot(vm, file.getPath());
    }

    public void testShouldFailToGetVersionHistoryOfPersistentNonVersionableNodeThatWasTransientlyMadeVersionable()
        throws Exception {
        Session session = superuser;
        VersionManager vm = session.getWorkspace().getVersionManager();

        Node root = session.getRootNode();
        Node file = root.addNode("createfile3.mode", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));

        // Save the node ...
        session.save();

        // Now add the mixin transiently ...
        file.addMixin("mix:versionable");

        try {
            vm.getVersionHistory(file.getPath());
            fail("It should not be possible to obtain the version history for a persistent node that was newly-made versionable.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }

        session.save();
        ensureExactlyOneVersionRoot(vm, file.getPath());

    }

    public void testShouldNotHaveVersionHistoryForTransientVersionableNode() throws Exception {
        Session session = superuser;
        ValueFactory vf = session.getValueFactory();

        Node root = session.getRootNode();
        Node file = root.addNode("createfile4.mode", "nt:file");

        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));

        file.addMixin("mix:versionable");

        // Per Section 15.1:
        // "Under both simple and full versioning, on persist of a new versionable node N that neither corresponds
        // nor shares with an existing node:
        // - The jcr:isCheckedOut property of N is set to true and
        // - A new VersionHistory (H) is created for N. H contains one Version, the root version (V0)
        // (see §3.13.5.2 Root Version)."
        //
        // This means that the version history should not be created until save is performed. This makes sense,
        // because otherwise the version history would be persisted for a newly-created node, even though that node
        // is not yet persisted. Tests with the reference implementation (see sandbox) verified this behavior.
        try {
            session.getWorkspace().getVersionManager().getVersionHistory(file.getPath());
            fail("It should not be possible to obtain the version history for a transient node.");
        } catch (RepositoryException e) {
            // some repository exception is expected; the ref impl throws a RepositoryException
        }

        session.save();
        ensureExactlyOneVersionRoot(session.getWorkspace().getVersionManager(), "/createfile.mode");
        session.refresh(false);
        ensureExactlyOneVersionRoot(session.getWorkspace().getVersionManager(), "/createfile.mode");
    }

    @FixFor( "MODE-1089" )
    public void testShouldNotFailGettingVersionHistoryForNodeMadeVersionableSinceLastSave() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        VersionManager vm = session1.getWorkspace().getVersionManager();

        // Create node structure
        Node root = session1.getRootNode();
        Node area = root.addNode("tmp2", "nt:unstructured");

        Node outer = area.addNode("outerFolder");
        Node inner = outer.addNode("innerFolder");
        Node file = inner.addNode("testFile.dat");
        file.setProperty("jcr:mimeType", "text/plain");
        file.setProperty("jcr:data", "Original content");
        session1.save();

        file.addMixin("mix:versionable");
        // session.save();

        isVersionable(vm, file); // here's the problem
        session1.save();

        Version v1 = vm.checkin(file.getPath());
        assertThat(v1, is(notNullValue()));
        // System.out.println("Created version: " + v1);
        // ubgraph(root.getNode("jcr:system/jcr:versionStorage"));
    }

    @SuppressWarnings( "deprecation" )
    public void testShouldCreateProperVersionHistoryWhenSavingVersionedNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = session.getRootNode().addNode("test", "nt:unstructured");
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

        assertThat(versionHistory(node).getUUID(), is(history.getUUID()));
        assertThat(versionHistory(node).getIdentifier(), is(history.getIdentifier()));
        assertThat(versionHistory(node).getPath(), is(history.getPath()));

        assertThat(baseVersion(node).getUUID(), is(version.getUUID()));
        assertThat(baseVersion(node).getIdentifier(), is(version.getIdentifier()));
        assertThat(baseVersion(node).getPath(), is(version.getPath()));
    }

    public void testShouldCreateProperStructureForPropertiesOnTheFirstCheckInOfANode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = session.getRootNode().addNode("/checkInTest", "modetest:versionTest");
        session.save();

        node.addMixin("mix:versionable");
        session.save();

        node.setProperty("abortProp", "abortPropValue");
        node.setProperty("copyProp", "copyPropValue");
        node.setProperty("ignoreProp", "ignorePropValue");
        node.setProperty("versionProp", "versionPropValue");

        session.save();

        try {
            checkin(node);
            fail("Should not be able to checkin a node with a property that has an OnParentVersionAction of ABORT");
        } catch (VersionException ve) {
            assertThat(node.getProperty("jcr:isCheckedOut").getBoolean(), is(true));
        }

        node.setProperty("abortProp", (String)null);
        session.save();

        checkin(node);
        assertThat(node.getProperty("jcr:isCheckedOut").getBoolean(), is(false));

        Version version = baseVersion(node);
        assertThat(version, is(notNullValue()));
        assertThat(version.getProperty("jcr:frozenNode/copyProp").getString(), is("copyPropValue"));
        assertThat(version.getProperty("jcr:frozenNode/versionProp").getString(), is("versionPropValue"));

        try {
            version.getProperty("jcr:frozenNode/ignoreProp");
            fail("Frozen version should not record a property that has an OnParentVersionAction of IGNORE");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        checkout(node);

        node.setProperty("abortProp", "abortPropValueNew");
        node.setProperty("copyProp", "copyPropValueNew");
        node.setProperty("ignoreProp", "ignorePropValueNew");
        node.setProperty("versionProp", "versionPropValueNew");

        version = baseVersion(node);
        assertThat(version, is(notNullValue()));
        assertThat(version.getProperty("jcr:frozenNode/copyProp").getString(), is("copyPropValue"));
        assertThat(version.getProperty("jcr:frozenNode/versionProp").getString(), is("versionPropValue"));

        try {
            version.getProperty("ignoreProp");
            fail("Frozen version should not record a property that has an OnParentVersionAction of IGNORE");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        session.save();

    }

    public void testShouldCreateProperHistoryForNodeWithCopySemantics() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode/copyNode/AbortNode with the first copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");

        Node midLevel = copyNode.addNode("copyNode", "modetest:versionTest");
        midLevel.setProperty("ignoreProp", "ignorePropValue");
        midLevel.setProperty("copyProp", "copyPropValue");

        Node abortNode = midLevel.addNode("abortNode", "modetest:versionTest");
        abortNode.setProperty("ignoreProp", "ignorePropValue");
        abortNode.setProperty("copyProp", "copyPropValue");

        /*
        * Create /checkinTest/copyNode/versionNode with versionNode being versionable as well. This should
        * create a copy of versionNode in the version history, due to copyNode (the root of the checkin) having
        * COPY semantics for the OnParentVersionAction.
        */

        Node versionNode = copyNode.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        session.save();

        Version version = checkin(copyNode);
        Version version2 = checkin(versionNode);

        assertThat(version.getProperty("jcr:frozenNode/versionNode/jcr:primaryType").getString(), is("nt:versionedChild"));
        assertThat(version.getProperty("jcr:frozenNode/copyNode/abortNode/copyProp").getString(), is("copyPropValue"));
        try {
            version.getProperty("jcr:frozenNode/abortNode/ignoreProp");
            fail("Property with OnParentVersionAction of IGNORE should not have been copied");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        assertThat(version2.getProperty("jcr:frozenNode/jcr:primaryType").getString(), is("nt:frozenNode"));
        assertThat(version2.getProperty("jcr:frozenNode/jcr:frozenPrimaryType").getString(), is("modetest:versionTest"));
        assertThat(version2.getProperty("jcr:frozenNode/jcr:frozenUuid").getString(), is(versionNode.getIdentifier()));
    }

    public void testShouldThrowExceptionWhenVersioningChildNodeWithOnParentVersionSemanticsOfAbort() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/versionNode/abortNode with versionNode being versionable. This should fail
        * when versionNode is checked in, as the OnParentVersionAction semantics come from the OPV of the child.
        */

        Node versionNode = node.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        Node abortNode = versionNode.addNode("abortNode", "modetest:versionTest");
        abortNode.setProperty("ignoreProp", "ignorePropValue");
        abortNode.setProperty("copyProp", "copyPropValue");

        session.save();

        try {
            checkin(versionNode);
            fail("Child node with OnParentVersionAction of ABORT should have resulted in exception.");
        } catch (VersionException ve) {
            // Expected
        }
    }

    public void testShouldCreateProperHistoryForVersionableChildOfNodeWithVersionSemantics() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/versionNode/copyNode with versionNode and copyNode being versionable. This should
        * create a child of type nt:childVersionedNode under the frozen node.
        */

        Node versionNode = node.addNode("versionNode", "modetest:versionTest");
        versionNode.addMixin("mix:versionable");

        Node copyNode = versionNode.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("copyProp", "copyPropValue");

        session.save();

        Version version = checkin(versionNode);

        assertThat(version.getProperty("jcr:frozenNode/copyNode/jcr:primaryType").getString(), is("nt:frozenNode"));
        assertThat(version.getProperty("jcr:frozenNode/copyNode/jcr:frozenPrimaryType").getString(), is("modetest:versionTest"));
        try {
            version.getProperty("jcr:frozenNode/copyNode/copyProp");
        } catch (PathNotFoundException pnfe) {
            fail("Property should be copied to versionable child of versioned node, because copyNode was copied (see Section 3.13.9 Item 5 of JSR-283)");
        }

        try {
            version.getProperty("jcr:frozenNode/copyNode/ignoreProp");
        } catch (PathNotFoundException pnfe) {
            fail("Ignored property should be copied to versionable child of versioned node when in a COPY subgraph");
        }
    }

    public void testShouldRestorePropertiesOnVersionableNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode with copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        session.save();

        Version version = checkin(copyNode);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        restore(copyNode, version, false);

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

    @FixFor( "MODE-1228" )
    public void testShouldRestoreWithNoReplaceTheNonReferenceableCopiedChildNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode with copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:title");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:title"));

        Version version = checkin(copyNode);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        // printSubgraph(copyNode);
        // printVersionHistory(copyNode);

        restore(copyNode, version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:title"));
    }

    @FixFor( "MODE-1228" )
    public void testShouldRestoreWithReplaceTheNonReferenceableCopiedChildNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode with copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:title");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:title"));

        Version version = checkin(copyNode);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        restore(copyNode, version, true);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:title"));
    }

    @FixFor( "MODE-1228" )
    public void testShouldRestoreWithNoReplaceTheReferenceableCopiedChildNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode with copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:referenceable");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
        String belowCopyNodeId = belowCopyNode.getIdentifier();

        Version version = checkin(copyNode);

        // printSubgraph(version);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        // printVersionHistory(copyNode);

        restore(copyNode, version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they do NOT replace the
        // referenceable nodes (with the same identifier) in the workspace. See Section 15.7.6 for details.
        // Therefore, the properties should match the updated values...

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        String belowCopyNode2Id = belowCopyNode.getIdentifier();
        assertThat(belowCopyNodeId, is(belowCopyNode2Id));
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
    }

    @FixFor( "MODE-1228" )
    public void testShouldRestoreWithReplaceTheReferenceableCopiedChildNode() throws Exception {
        session = getHelper().getReadWriteSession();
        Node node = getTestRoot(session).addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
        * Create /checkinTest/copyNode with copyNode being versionable. This should be able
        * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
        */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:referenceable");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
        String belowCopyNodeId = belowCopyNode.getIdentifier();

        Version version = checkin(copyNode);

        // printSubgraph(version);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        // printVersionHistory(copyNode);

        restore(copyNode, version, true);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        String belowCopyNode2Id = belowCopyNode.getIdentifier();
        assertThat(belowCopyNodeId, is(belowCopyNode2Id));
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
    }

    @FixFor( "MODE-693" )
    public void testShouldAllowDeletingNodesWhenLargePropertyIsPresent() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = session.getRootNode();

        final int SIZE = 2048;
        byte[] largeArray = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            largeArray[i] = (byte)'x';
        }

        Node projectNode = root.addNode("mode693", "nt:unstructured");

        Node fileNode = projectNode.addNode("fileNode", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
        Binary binaryValue = session.getValueFactory().createBinary(new ByteArrayInputStream(largeArray));
        contentNode.setProperty("jcr:data", binaryValue);
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

    @FixFor( "MODE-704" )
    public void testShouldReturnSilentlyWhenCheckingOutACheckedOutNode() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = session.getRootNode();

        Node testNode = root.addNode("checkedOutNodeTest", "nt:unstructured");
        session.save();

        // Add the mixin, but don't save it
        testNode.addMixin("mix:versionable");
        checkout(testNode);

        session.save();

        // Now check that it still returns silently on a saved node that was never checked in.
        checkout(testNode);
    }

    @FixFor( "MODE-709" )
    public void testShouldCreateVersionStorageForWhenVersionableNodesCopied() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);

        Node parentNode = root.addNode("versionableNodeForCopy", "nt:unstructured");
        parentNode.addMixin("mix:versionable");

        Node childNode = parentNode.addNode("versionableChild", "nt:unstructured");
        childNode.addMixin("mix:versionable");

        Node targetNode = root.addNode("destForCopy", "nt:unstructured");

        session.save();

        String newParentPath = targetNode.getPath() + "/" + parentNode.getName();
        session.getWorkspace().copy(parentNode.getPath(), newParentPath);

        parentNode = (Node)session.getItem(newParentPath);
        childNode = parentNode.getNode("versionableChild");

        checkout(parentNode);
        checkin(parentNode);

        checkout(childNode);
        checkin(childNode);

    }

    @FixFor( "MODE-720" )
    @SuppressWarnings( "unchecked" )
    public void testShouldBeAbleToReferToUnsavedReferenceNode() throws Exception {
        session = getHelper().getSuperuserSession();

        JcrNodeTypeManager nodeTypes = (JcrNodeTypeManager)session.getWorkspace().getNodeTypeManager();

        /*
        * Register a one-off node type with a reference property that has a constraint on it
        */
        NodeTypeTemplate ntt = nodeTypes.createNodeTypeTemplate();
        ntt.setName("modetest:constrainedPropType");

        PropertyDefinitionTemplate pdt = nodeTypes.createPropertyDefinitionTemplate();
        pdt.setName("modetest:constrainedProp");
        pdt.setRequiredType(PropertyType.REFERENCE);
        pdt.setValueConstraints(new String[] {"nt:unstructured"});
        ntt.getPropertyDefinitionTemplates().add(pdt);

        nodeTypes.registerNodeType(ntt, false);

        /*
        * Add a node that would satisfy the constraint
        */
        Node root = session.getRootNode();

        Node parentNode = root.addNode("constrainedNodeTest", "nt:unstructured");
        Node targetNode = parentNode.addNode("target", "nt:unstructured");
        targetNode.addMixin("mix:referenceable");

        /*
        * Now add a node with the one-off type.
        */
        Node referringNode = parentNode.addNode("referer", "modetest:constrainedPropType");
        referringNode.setProperty("modetest:constrainedProp", targetNode);

        session.save();
    }

    @FixFor( "MODE-1092" )
    @SuppressWarnings( "unchecked" )
    public void testShouldBeAbleToSetWeakReferences() throws Exception {

        session = getHelper().getSuperuserSession();

        JcrNodeTypeManager nodeTypes = (JcrNodeTypeManager)session.getWorkspace().getNodeTypeManager();

        /*
        * Register a one-off node type with a reference property that has a constraint on it
        */
        NodeTypeTemplate ntt = nodeTypes.createNodeTypeTemplate();
        ntt.setName("modetest:typeWithWeakReference");

        PropertyDefinitionTemplate pdt = nodeTypes.createPropertyDefinitionTemplate();
        pdt.setName("modetest:weakRefProp");
        pdt.setRequiredType(PropertyType.WEAKREFERENCE);
        pdt.setValueConstraints(new String[] {"nt:unstructured"});
        ntt.getPropertyDefinitionTemplates().add(pdt);

        nodeTypes.registerNodeType(ntt, false);

        /*
        * Add a node that would satisfy the constraint
        */
        Node root = session.getRootNode();

        Node parentNode = root.addNode("weakReferenceTest", "nt:unstructured");
        Node targetNode = parentNode.addNode("target", "nt:unstructured");
        targetNode.addMixin("mix:referenceable");

        /*
        * Now add a node with the one-off type.
        */
        Node referringNode = parentNode.addNode("referer", "modetest:typeWithWeakReference");
        referringNode.setProperty("modetest:weakRefProp", targetNode);

        session.save();

        // Now fetch the referring node again, and verify that the reference is still a weak reference
        Node referringNode2 = session.getNode("/weakReferenceTest/referer");
        Property property = referringNode2.getProperty("modetest:weakRefProp");
        assertThat(property.getType(), is(PropertyType.WEAKREFERENCE));
        Node referredNode = property.getNode();
        assertThat(referredNode, is(targetNode));
    }

    @FixFor( "MODE-701" )
    public void testShouldBeAbleToImportAutocreatedChildNodeWithoutDuplication() throws Exception {
        session = getHelper().getSuperuserSession();

        /*
        * Add a node that would satisfy the constraint
        */
        Node root = getTestRoot(session);

        Node parentNode = root.addNode("autocreatedChildRoot", "nt:unstructured");
        session.save();

        Node targetNode = parentNode.addNode("nodeWithAutocreatedChild", "modetest:nodeWithAutocreatedChild");
        assertThat(targetNode.getNode("modetest:autocreatedChild"), is(notNullValue()));
        // Don't save this yet
        session.refresh(false);

        InputStream in = getClass().getResourceAsStream("/io/autocreated-node-test.xml");
        session.importXML(root.getPath() + "/autocreatedChildRoot", in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    public void testShouldAllowCheckoutAfterMove() throws Exception {
        // q.v., MODE-???

        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node sourceNode = root.addNode("versionableSource", "nt:unstructured");
        sourceNode.addMixin("mix:versionable");

        Node targetNode = root.addNode("versionableTarget", "nt:unstructured");
        session.save();

        String sourceName = sourceNode.getName();
        session.move(sourceNode.getPath(), targetNode.getPath() + "/" + sourceName);
        sourceNode = targetNode.getNode(sourceName);
        checkout(sourceNode);
    }

    @SuppressWarnings( "deprecation" )
    public void testAdminUserCanBreakOthersLocksUsingDeprecatedNodeLockAndUnlockMethods() throws Exception {
        String lockNodeName = "lockTestNode";
        session = getHelper().getReadWriteSession();
        Node root = getTestRoot(session);
        Node lockNode = root.addNode(lockNodeName);
        lockNode.addMixin("mix:lockable");
        session.save();

        lockNode.lock(false, false);
        assertThat(lockNode.isLocked(), is(true));

        Session superuser = getHelper().getSuperuserSession();
        root = getTestRoot(superuser);
        lockNode = root.getNode(lockNodeName);

        assertThat(lockNode.isLocked(), is(true));
        lockNode.unlock();
        assertThat(lockNode.isLocked(), is(false));
        superuser.logout();

    }

    public void testAdminUserCanBreakOthersLocks() throws Exception {
        String lockNodeName = "lockTestNode2";
        session = getHelper().getReadWriteSession();
        Node root = getTestRoot(session);
        Node lockNode = root.addNode(lockNodeName);
        lockNode.addMixin("mix:lockable");
        session.save();

        session.getWorkspace().getLockManager().lock(lockNode.getPath(), false, false, 1L, "me");
        assertThat(lockNode.isLocked(), is(true));

        Session superuser = getHelper().getSuperuserSession();
        root = getTestRoot(superuser);
        lockNode = root.getNode(lockNodeName);

        assertThat(lockNode.isLocked(), is(true));
        session.getWorkspace().getLockManager().unlock(lockNode.getPath());
        assertThat(lockNode.isLocked(), is(false));
        superuser.logout();

    }

    public void testShouldNotAllowLockedNodeToBeRemoved() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("lockedParent");
        parentNode.addMixin("mix:lockable");

        Node targetNode = parentNode.addNode("lockedTarget");
        session.save();

        lock(parentNode, true, true);

        Session session2 = getHelper().getReadWriteSession();
        Node targetNode2 = (Node)session2.getItem(root.getPath() + "/lockedParent/lockedTarget");

        try {
            targetNode2.remove();
            fail("Locked nodes should not be able to be removed");
        } catch (LockException le) {
            // Success
        }

        targetNode.remove();
        session.save();
    }

    public void testShouldNotAllowPropertyOfLockedNodeToBeRemoved() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("lockedPropParent");
        parentNode.addMixin("mix:lockable");

        Node targetNode = parentNode.addNode("lockedTarget");
        targetNode.setProperty("foo", "bar");
        session.save();

        lock(parentNode, true, true);

        Session session2 = getHelper().getReadWriteSession();
        Property targetProp2 = (Property)session2.getItem(root.getPath() + "/lockedPropParent/lockedTarget/foo");

        try {
            targetProp2.remove();
            fail("Properties of locked nodes should not be able to be removed");
        } catch (LockException le) {
            // Success
        }

        targetNode.getProperty("foo").remove();
        session.save();
    }

    public void testShouldNotAllowCheckedInNodeToBeRemoved() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("checkedInParent");
        parentNode.addMixin("mix:versionable");

        Node targetNode = parentNode.addNode("checkedInTarget");
        session.save();

        checkin(parentNode);

        try {
            targetNode.remove();
            fail("Checked in nodes should not be able to be removed");
        } catch (VersionException ve) {
            // Success
        }

        checkout(parentNode);
        targetNode.remove();
        session.save();
    }

    public void testShouldNotAllowPropertyOfCheckedInNodeToBeRemoved() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("checkedInPropParent");
        parentNode.addMixin("mix:versionable");

        Node targetNode = parentNode.addNode("checkedInTarget");
        Property targetProp = targetNode.setProperty("foo", "bar");
        session.save();

        checkin(parentNode);

        try {
            targetProp.remove();
            fail("Properties of checked in nodes should not be able to be removed");
        } catch (VersionException ve) {
            // Success
        }

        checkout(parentNode);
        targetProp.remove();
        session.save();
    }

    public void testGetPathOnRemovedNodeShouldThrowException() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("invalidItemStateTest");
        session.save();

        parentNode.remove();

        try {
            parentNode.getPath();
            fail("getPath on removed node should throw InvalidItemStateException per section 7.1.3.3 of 1.0.1 spec");
        } catch (InvalidItemStateException iise) {
            // Success
        }

    }

    @FixFor( "MODE-792" )
    public void testCheckingOutAnAlreadyCheckedOutNodeShouldHaveNoEffect() throws Exception {
        session = getHelper().getReadWriteSession();
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("checkedOutNodeNopTest");
        parentNode.addMixin("mix:versionable");

        session.save();
        versionManager.checkin(parentNode.getPath());

        versionManager.checkout(parentNode.getPath());

        parentNode.setProperty("foo", "bar");
        versionManager.checkout(parentNode.getPath());

        assertEquals(parentNode.getProperty("foo").getString(), "bar");

    }

    @FixFor( "MODE-793" )
    public void testPropertyCardinalityShouldPropagateToFrozenNode() throws Exception {
        session = getHelper().getReadWriteSession();
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node root = getTestRoot(session);
        Node parentNode = root.addNode("checkedOutNodeNopTest");
        parentNode.addMixin("mix:versionable");
        parentNode.setProperty("foo", new String[] {"bar", "baz"});
        session.save();

        assertEquals(true, parentNode.getProperty("foo").getDefinition().isMultiple());

        Version version = versionManager.checkin(parentNode.getPath());

        Node frozenNode = version.getFrozenNode();

        assertEquals(true, frozenNode.getProperty("foo").getDefinition().isMultiple());
    }

    @FixFor( "MODE-799" )
    public void testNodeWithoutETagMixinShouldNotHaveETagProperty() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        root.addNode("someNewNode");
        session.save();

        assertThat(root.getNode("someNewNode").hasProperty("jcr:etag"), is(false));
    }

    @FixFor( "MODE-799" )
    public void testAutomaticCreationUponSaveOfETagPropertyWhenETagMixinIsAddedToNodeWithoutBinaryProperties() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node newNode = root.addNode("someNewNode4");
        newNode.addMixin("mix:etag");
        session.save();

        String etagValue = root.getNode("someNewNode4").getProperty("jcr:etag").getString();
        assertThat(etagValue, is(""));
    }

    @FixFor( "MODE-799" )
    public void testAutomaticCreationUponSaveOfETagPropertyWhenETagMixinIsAddedToNodeWithExistingBinaryProperties()
        throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node newNode = root.addNode("someNewNode2");
        Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream("This is the value".getBytes()));
        newNode.setProperty("binaryProperty", binary);
        session.save();

        root.getNode("someNewNode2").addMixin("mix:etag");
        session.save();

        String etagValue = root.getNode("someNewNode2").getProperty("jcr:etag").getString();
        assertThat(etagValue.trim().length() != 0, is(true));
    }

    @FixFor( "MODE-799" )
    public void testAutomaticCreationUponSaveOfETagPropertyWhenNodeWithETagMixinHasNewBinaryProperty() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node newNode = root.addNode("someNewNode3");
        newNode.addMixin("mix:etag");
        session.save();

        Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream("This is the value".getBytes()));
        root.getNode("someNewNode3").setProperty("binaryProperty", binary);
        session.save();

        String etagValue = root.getNode("someNewNode3").getProperty("jcr:etag").getString();
        assertThat(etagValue.trim().length() != 0, is(true));
    }

    @FixFor( "MODE-796" )
    public void testNodeReferenceRemainsValidAfterSave() throws Exception {
        session = getHelper().getReadWriteSession();

        Node root = getTestRoot(session);
        Node nodeA = root.addNode("nodeA", "nt:unstructured");
        nodeA.setProperty("foo", "bar A");
        Node nodeB = root.addNode("nodeB", "nt:unstructured");
        nodeB.setProperty("foo", "bar B");
        session.save();

        // Verify that the node references still have a path ...
        assertThat(nodeA.getPath(), is(root.getPath() + "/nodeA"));
        assertThat(nodeB.getPath(), is(root.getPath() + "/nodeB"));

        // Move 'nodeA' under 'nodeB' ...
        session.move(nodeA.getPath(), root.getPath() + "/nodeB/nodeA");
        assertThat(nodeA.getPath(), is(root.getPath() + "/nodeB/nodeA"));
        assertThat(nodeB.getPath(), is(root.getPath() + "/nodeB"));
        session.save();
        assertThat(nodeA.getPath(), is(root.getPath() + "/nodeB/nodeA"));
        assertThat(nodeB.getPath(), is(root.getPath() + "/nodeB"));
    }

    @FixFor( "MODE-956" )
    public void testShouldBeAbleToSetNonexistingPropertyToNull() throws Exception {
        Node rootNode = getTestRoot(superuser);

        Node child = rootNode.addNode("child", "nt:unstructured");
        rootNode.getSession().save();

        child.setProperty("foo", (Calendar)null);
    }

    @FixFor( "MODE-1005" )
    public void testShouldThrowRepositoryExceptionForRelativePathsInSessionGetNode() throws Exception {
        try {
            Node root = getTestRoot(superuser);
            root.addNode("nodeForRelativePathTest", "nt:unstructured");

            superuser.getNode("nodeForRelativePathTest");
            fail("Should throw RepositoryException when attempting to call Session.getNode(String) with a relative path");
        } catch (RepositoryException re) {
            // Expected
        }
    }

    @FixFor( "MODE-1005" )
    public void testShouldThrowRepositoryExceptionForRelativePathsInSessionGetProperty() throws Exception {
        try {
            Node root = getTestRoot(superuser);
            root.addNode("propertyNodeForRelativePathTest", "nt:unstructured");

            superuser.getProperty("propertyNodeForRelativePathTest/jcr:primaryType");
            fail("Should throw RepositoryException when attempting to call Session.getProperty(String) with a relative path");
        } catch (RepositoryException re) {
            // Expected
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockPreventsOtherSessionsFromChangingPropertiesOnLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();
        // subgraph(root1);

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toChange = session2.getNode(node2.getPath());
        try {
            toChange.addMixin("mix:versionable");
            fail("Expected to see LockException");
        } catch (LockException e) {
            // expected
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockAllowsOtherSessionsToDeleteLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toDelete = session2.getNode(node2.getPath());
        try {
            // This is possible because removing node2 is an alteration of node1, upon which there is no lock
            toDelete.remove();
            session2.save();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockAllowsSameSessionToDeleteLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        try {
            node3.remove();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockPreventsOtherSessionsFromDeletingChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toDelete = session2.getNode(node3.getPath());
        try {
            toDelete.remove();
            fail("Expected to see LockException");
        } catch (LockException e) {
            // expected
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockAllowsSameSessionToDeleteChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        try {
            node3.remove();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockAllowOtherSessionsToChangePropertiesOfChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toChange = session2.getNode(node3.getPath());
        try {
            toChange.addMixin("mix:referenceable");
            session2.save();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyShallowLockAllowsSameSessionToChangeChildProperties() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:unstructured");
        Node node1 = tmp.addNode("node1", "nt:unstructured");
        Node node2 = tmp.addNode("node1/node2", "nt:unstructured");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:unstructured");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), false, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        Node toChange = node2;
        try {
            toChange.setProperty("newProp", "newValue");
            session1.save();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyAddingMixinAndSavingRequiredBeforeLockingNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        try {
            node2.addMixin("mix:lockable");
            lm1.lock(node2.getPath(), false, false, 10000, "Locked");
            session1.save();
        } catch (InvalidItemStateException e) {
            // expected??, since the lock manager is owned by the workspace
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockPreventsOtherSessionsFromChangingPropertiesOnLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toChange = session2.getNode(node2.getPath());
        try {
            toChange.addMixin("mix:versionable");
            fail("Expected to see LockException");
        } catch (LockException e) {
            // expected
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockAllowsOtherSessionsToDeleteLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toDelete = session2.getNode(node2.getPath());
        try {
            // This is possible because removing node2 is an alteration of node1, upon which there is no lock
            toDelete.remove();
            session2.save();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockAllowsSameSessionToDeleteLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        try {
            node3.remove();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockPreventsOtherSessionsFromDeletingChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toDelete = session2.getNode(node3.getPath());
        try {
            toDelete.remove();
            fail("Expected to see LockException");
        } catch (LockException e) {
            // expected
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockAllowsSameSessionToDeleteChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 10000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        try {
            node3.remove();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockPreventsOtherSessionsFromChangePropertiesOfChildOfLockedNode() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:folder");
        Node node1 = tmp.addNode("node1", "nt:folder");
        Node node2 = tmp.addNode("node1/node2", "nt:folder");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:folder");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        session2.refresh(true);
        Node toChange = session2.getNode(node3.getPath());
        try {
            toChange.addMixin("mix:referenceable");
            session2.save();
            fail("Expected to see LockException");
        } catch (LockException e) {
            // expected
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
            session2.logout();
        }
    }

    @FixFor( "MODE-1040" )
    @SuppressWarnings( "unused" )
    public void testShouldVerifyDeepLockAllowsSameSessionToChangeChildProperties() throws Exception {
        Session session1 = getHelper().getSuperuserSession();
        LockManager lm1 = session1.getWorkspace().getLockManager();

        // Create node structure
        Node root1 = getTestRoot(session1);
        Node tmp = root1.addNode("tmp", "nt:unstructured");
        Node node1 = tmp.addNode("node1", "nt:unstructured");
        Node node2 = tmp.addNode("node1/node2", "nt:unstructured");
        Node node3 = tmp.addNode("node1/node2/node3", "nt:unstructured");
        session1.save();

        // Create an open-scoped shallow lock on node2
        node2.addMixin("mix:lockable");
        session1.save();
        lm1.lock(node2.getPath(), true, false, 100000, "Locked");
        session1.save();

        // Attempt to a child node of node2
        Node toChange = node2;
        try {
            toChange.setProperty("newProp", "newValue");
            session1.save();
        } finally {
            tmp.remove();
            session1.save();
            session1.logout();
        }
    }

    boolean isVersionable( VersionManager vm,
                           Node node ) throws RepositoryException {
        try {
            vm.getVersionHistory(node.getPath());
            return true;
        } catch (UnsupportedRepositoryOperationException e) {
            return false;
        }
    }

    protected void checkinRecursively( Node node,
                                       VersionManager vm ) throws RepositoryException {
        String path = node.getPath();
        if (node.isNodeType("mix:versionable") && vm.isCheckedOut(path)) {
            try {
                vm.checkin(path);
            } catch (VersionException e) {
                // Something on this node can't be checked in, so remove it ..
                node.remove();
                return;
            }
        }
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            checkinRecursively(iter.nextNode(), vm);
        }
    }

    @FixFor( "MODE-1234" )
    public void IGNOREtestShouldFindCheckedOutNodesByQuerying() throws Exception {
        // TODO: Query
        Session session1 = getHelper().getSuperuserSession();
        VersionManager vm = session1.getWorkspace().getVersionManager();

        // Check in all nodes that are still checked out ...
        Node root = session1.getRootNode();
        checkinRecursively(root, vm);

        // Create node structure
        Node area = root.addNode("tmp2", "nt:unstructured");

        Node outer = area.addNode("outerFolder");
        Node inner = outer.addNode("innerFolder");
        Node file = inner.addNode("testFile.dat");
        file.setProperty("jcr:mimeType", "text/plain");
        file.setProperty("jcr:data", "Original content");
        session1.save();

        file.addMixin("mix:versionable");
        // session.save();

        isVersionable(vm, file); // here's the problem
        session1.save();

        Version v1 = vm.checkin(file.getPath());
        assertThat(v1, is(notNullValue()));

        // Register a listener so that we only have to wait until the change is made ...
        CountDownListener listener = registerCountDownListener(session1, file.getPath(), 1);

        // Query for versionables ...
        queryForCheckedOutVersionables(session1, 0, false);

        // Now check out a node ...
        vm.checkout(file.getPath());

        // and wait a bit ...
        listener.await(2, TimeUnit.SECONDS);
        listener.unregister();

        // And re-query ...
        queryForCheckedOutVersionables(session1, 1, false);

        // Register a listener so that we only have to wait until the change is made ...
        listener = registerCountDownListener(session1, file.getPath(), 1);

        // Check it back in ...
        vm.checkin(file.getPath());

        // and wait a bit ...
        listener.await(2, TimeUnit.SECONDS);
        listener.unregister();

        // And re-query ...
        queryForCheckedOutVersionables(session1, 0, false);
    }

    protected int queryForCheckedOutVersionables( Session session,
                                                  int expected,
                                                  boolean print ) throws RepositoryException {
        String queryStr = "SELECT * FROM [nt:unstructured] AS node JOIN [mix:versionable] AS versionable "
                          + "ON ISSAMENODE(node,versionable) WHERE versionable.[jcr:isCheckedOut] = true";
        QueryResult result = query(session, queryStr);
        int actual = (int)result.getRows().getSize();
        if (print) System.out.println("Result = \n" + result);
        if (actual != expected) {
            queryStr = "SELECT * FROM [nt:unstructured] AS node JOIN [mix:versionable] AS versionable "
                       + "ON ISSAMENODE(node,versionable)";
            result = query(session, queryStr);
            System.out.println(result);
        }
        assertThat(actual, is(expected));
        return actual;
    }

    protected QueryResult query( Session session,
                                 String queryStr ) throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        QueryResult result = query.execute();
        return result;
    }

    protected CountDownListener registerCountDownListener( Session session,
                                                           String path,
                                                           int counts ) throws RepositoryException {
        CountDownListener listener = new CountDownListener(session, counts);
        session.getWorkspace()
               .getObservationManager()
               .addEventListener(listener, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, path, true, null, null, false);
        return listener;
    }

    protected static class CountDownListener implements EventListener {
        final CountDownLatch latch;
        final Session session;

        public CountDownListener( Session session,
                                  int counts ) {
            this.session = session;
            this.latch = new CountDownLatch(counts);
        }

        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    latch.countDown();
                } catch (Throwable e) {
                    latch.countDown();
                    fail(e.getMessage());
                }
            }
        }

        public void await( int time,
                           TimeUnit unit ) {
            try {
                latch.await(time, unit);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }

        public void unregister() throws RepositoryException {
            this.session.getWorkspace().getObservationManager().removeEventListener(this);
        }
    }

    @SuppressWarnings( "unchecked" )
    @FixFor( "MODE-1050" )
    public void testShouldSuccessfullyRegisterChildAndParentSameTypes() throws RepositoryException {
        Session session = getHelper().getSuperuserSession();

        session.getWorkspace().getNamespaceRegistry().registerNamespace("mx", "urn:test");
        final NodeTypeManager nodeTypeMgr = session.getWorkspace().getNodeTypeManager();

        final NodeTypeTemplate nodeType = nodeTypeMgr.createNodeTypeTemplate();
        nodeType.setName("mx:type");
        nodeType.setDeclaredSuperTypeNames(new String[] {"nt:folder"});

        final NodeDefinitionTemplate subTypes = nodeTypeMgr.createNodeDefinitionTemplate();
        subTypes.setRequiredPrimaryTypeNames(new String[] {"mx:type"});
        nodeType.getNodeDefinitionTemplates().add(subTypes);

        nodeTypeMgr.registerNodeType(nodeType, false);
    }
}
