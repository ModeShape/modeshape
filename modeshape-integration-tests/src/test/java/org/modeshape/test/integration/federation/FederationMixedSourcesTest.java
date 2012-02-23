package org.modeshape.test.integration.federation;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.test.ModeShapeSingleUseTest;
import java.io.ByteArrayInputStream;

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

}
