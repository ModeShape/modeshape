package org.modeshape.test.integration.federation;

import javax.jcr.Binary;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrNodeTypeManager;
import org.modeshape.jcr.JcrTools;
import org.modeshape.test.ModeShapeSingleUseTest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Integration test which uses a federation connector with 1 default workspace and several projections over different sources.
 *
 * @author Horia Chiorean
 */
public class FederationMixedSourcesTest extends ModeShapeSingleUseTest {

    private static final String FS_PROJECTION = "fs";
    private static final String JPA_PROJECTION = "jpa";
    private static final String INMEMORY_PROJECTION = "inmemory";

    private Session session;

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session = sessionTo("test-federated", "default");
    }

    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForFederatedMixedSources.xml";
    }

    @FixFor("MODE-1235")
    @Test
    public void shouldSupportReferenceableNodesAcrossSources() throws Exception {
        String referenceableNodeProjection = FS_PROJECTION;
        String targetNodeProjection = JPA_PROJECTION;

        String filePath = "/file_" + String.valueOf(System.currentTimeMillis());
        Node file = session.getRootNode().addNode(referenceableNodeProjection + filePath, NodeType.NT_FILE);
        Node fileContent = file.addNode("jcr:content", NodeType.NT_RESOURCE);
        fileContent.setProperty("jcr:data", session.getValueFactory().createBinary(new ByteArrayInputStream("content".getBytes())));        

        String testNodePath = "/testNode_" + String.valueOf(System.currentTimeMillis());
        session().getRootNode().addNode(targetNodeProjection + testNodePath);
        session.save();

        fileContent = session.getNode("/" + referenceableNodeProjection + filePath + "/jcr:content");
        fileContent.addMixin(NodeType.MIX_REFERENCEABLE);
        Node testNode = session.getNode("/" + targetNodeProjection + testNodePath);
        testNode.setProperty("contentRef", fileContent);
        session.save();

        Property contentRef = testNode.getProperty("contentRef");
        String fileContentUuid = fileContent.getProperty("jcr:uuid").getString();
        assertEquals(fileContentUuid, contentRef.getString());
        assertNotNull(contentRef.getNode());
        assertEquals(fileContentUuid, contentRef.getNode().getIdentifier());
    }

    @Test
    @FixFor("MODE-1424")
    public void shouldAllowUsedNamespacesToBeUnregistered() throws Exception {
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        String namespacePrefix = "rh";
        namespaceRegistry.registerNamespace(namespacePrefix, "http://www.redhat.com");
        session.save();
        assertNotNull(session.getNamespaceURI(namespacePrefix));

        JcrNodeTypeManager nodeTypeManager = (JcrNodeTypeManager)session.getWorkspace().getNodeTypeManager();
        nodeTypeManager.registerNodeTypes(
                getClass().getClassLoader().getResourceAsStream("federated/redhatMixin.cnd"),
                true);
        session.save();

        assertNotNull(nodeTypeManager.getNodeType("rh:product"));

        Node folder = session.getRootNode().addNode(INMEMORY_PROJECTION + "/jbossas", "nt:folder");
        folder.addMixin("rh:product");
        session.save();

        // attempt to delete the namespace first. according to JCR 2.0 specs this
        // operation should succeed
        namespaceRegistry.unregisterNamespace(namespacePrefix);
        try {
            namespaceRegistry.getURI(namespacePrefix);
            fail("Namespace not unregistered");
        } catch (NamespaceException e) {
            //expected
        }
        folder.remove();
        session.save();
    }

    @Test
    @FixFor( "MODE-1585" )
    public void shouldAllowVersioningOperations() throws Exception {
        String fileName = "testfile";

        String filePath = "/jpa/" + fileName;
        JcrTools tools = new JcrTools();
        Node fileNode = tools.findOrCreateNode(session, filePath, "nt:folder", "nt:file");
        Node resourceNode = fileNode.addNode("jcr:content", "nt:resource");
        URL fileUrl = getClass().getClassLoader().getResource(getPathToDefaultConfiguration());
        InputStream is = new FileInputStream(new File(fileUrl.toURI()));
        Binary binaryValue = session.getValueFactory().createBinary(is);
        resourceNode.setProperty("jcr:data", binaryValue);
        resourceNode.addMixin("mix:versionable");
        session.save();
        session.getWorkspace().getVersionManager().checkin(resourceNode.getPath());

        resourceNode = session.getNode(filePath + "/jcr:content");
        session.getWorkspace().getVersionManager().checkout(resourceNode.getPath());
        session.getWorkspace().getVersionManager().checkin(resourceNode.getPath());
    }

}
