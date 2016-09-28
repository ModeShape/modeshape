/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.connector.cmis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Workspace;

/**
 * Provide integration testing of the CMIS connector with OpenCMIS InMemory Repository.
 * 
 * @author Alexander Voloshyn
 * @version 1.0 2/20/2013
 */
public class CmisConnectorIT extends MultiUseAbstractTest {
    /**
     * Test OpenCMIS InMemory Server URL.
     * <p/>
     * This OpenCMIS InMemory server instance should be started by maven cargo plugin at pre integration stage.
     */
    private static final String CMIS_URL = "http://localhost:8090/";
    private static Logger logger = Logger.getLogger(CmisConnectorIT.class);

    @BeforeClass
    public static void beforeAll() throws Exception {
        // waiting when CMIS repository will be ready
        boolean isReady = false;

        // max time for waiting in milliseconds
        long maxTime = 30000L;

        // actially waiting time in milliseconds
        long waitingTime = 0L;

        // time quant in milliseconds
        long timeQuant = 500L;

        logger.info("Waiting for CMIS repository...");
        do {
            try {
                testDirectChemistryConnect();
                isReady = true;
            } catch (Exception e) {
                Thread.sleep(timeQuant);
                waitingTime += timeQuant;
            }
        } while (!isReady && waitingTime < maxTime);

        // checking status
        if (!isReady) {
            throw new IllegalStateException("CMIS repository did not respond withing " + maxTime + " milliseconds");
        }
        logger.info("CMIS repository has been started successfuly");
        
        RepositoryConfiguration config = RepositoryConfiguration.read("config/repository-1.json");
        startRepository(config);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    public static void testDirectChemistryConnect() {
        // default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, CMIS_URL + "services/ACLService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, CMIS_URL + "services/DiscoveryService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, CMIS_URL + "services/MultiFilingService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, CMIS_URL + "services/NavigationService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, CMIS_URL + "services/ObjectService10?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, CMIS_URL + "services/PolicyService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, CMIS_URL + "services/RelationshipService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, CMIS_URL + "services/RepositoryService10?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, CMIS_URL + "services/VersioningService?wsdl");
        // Default repository id for in memory server is A1
        parameter.put(SessionParameter.REPOSITORY_ID, "A1");

        // create session
        final Session session = factory.createSession(parameter);
        assertTrue("Chemistry session should exists.", session != null);
    }

    @Test
    public void shouldSeeCmisTypesAsJcrTypes() throws Exception {
        NodeTypeManager manager = getSession().getWorkspace().getNodeTypeManager();

        NodeTypeIterator it = manager.getNodeType("nt:file").getDeclaredSubtypes();
        while (it.hasNext()) {
            NodeType nodeType = it.nextNodeType();
            assertTrue(nodeType != null);
        }
    }

    @Test
    public void shouldAccessRootFolder() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertTrue(root != null);
    }

    @Test
    public void testRootFolderName() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertEquals("cmis", root.getName());
    }

    @Test
    public void shouldAccessRepositoryInfo() throws Exception {
        Node repoInfo = getSession().getNode("/cmis/repositoryInfo");
        // Different Chemistry versions return different things ...
        assertTrue(repoInfo.getProperty("cmis:productName").getString().contains("OpenCMIS"));
        assertTrue(repoInfo.getProperty("cmis:productName").getString().contains("InMemory"));
        assertEquals("Apache Chemistry", repoInfo.getProperty("cmis:vendorName").getString());
        assertTrue(repoInfo.getProperty("cmis:productVersion").getString() != null);
    }

    @Test
    public void shouldAccessFolderByPath() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertTrue(root != null);

        Node node1 = getSession().getNode("/cmis/My_Folder-0-0");
        assertTrue(node1 != null);

        Node node2 = getSession().getNode("/cmis/My_Folder-0-0/My_Folder-1-0");
        assertTrue(node2 != null);

        Node node3 = getSession().getNode("/cmis/My_Folder-0-0/My_Folder-1-0/My_Folder-2-0");
        assertTrue(node3 != null);
    }

    @Test
    public void shouldAccessDocumentPath() throws Exception {
        Node file = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        assertTrue(file != null);
    }

    @Test
    public void shouldAccessBinaryContent() throws Exception {
        Node file = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        Node cnt = file.getNode("jcr:content");

        Property value = cnt.getProperty("jcr:data");

        Binary bv = value.getValue().getBinary();
        InputStream is = bv.getStream();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b = 0;
        while (b != -1) {
            b = is.read();
            if (b != -1) {
                bout.write(b);
            }
        }

        byte[] content = bout.toByteArray();
        String s = new String(content, 0, content.length);

        assertFalse("Content shouldn't be empty.", s.trim().isEmpty());
    }

    // -----------------------------------------------------------------------/
    // Folder cmis build-in properties
    // -----------------------------------------------------------------------/
    @Test
    public void shouldAccessObjectIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String objectId = node.getProperty("jcr:uuid").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shouldAccessNamePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getName();
        assertEquals("My_Folder-0-0", name);
    }

    @Test
    public void shouldAccessCreatedByPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getProperty("jcr:createdBy").getString();
        assertEquals("unknown", name);
    }

    @Test
    public void shouldAccessCreationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        Calendar date = node.getProperty("jcr:created").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shouldAccessModificationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        Calendar date = node.getProperty("jcr:lastModified").getDate();
        assertTrue(date != null);
    }

    // -----------------------------------------------------------------------/
    // Document cmis build-in properties
    // -----------------------------------------------------------------------/
    @Test
    public void shouldAccessObjectIdPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String objectId = node.getProperty("jcr:uuid").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shouldAccessCreatedByPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("jcr:createdBy").getString();
        assertEquals("unknown", name);
    }

     @Test
    public void shouldAccessCreationDatePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        Calendar date = node.getProperty("jcr:created").getDate();
        assertTrue(date != null);
    }

     @Test
    public void shouldCreateFolderAndDocument() throws Exception {
        Node root = getSession().getNode("/cmis");
        String name = "test" + System.currentTimeMillis();
        Node node = root.addNode(name, "nt:folder");
        assertTrue(name.equals(node.getName()));
        // node.setProperty("name", "test-name");

        root = getSession().getNode("/cmis/" + name);
        Node node1 = root.addNode("test-1", "nt:file");
        // System.out.println("Test: creating binary content");
        byte[] content = "Hello World".getBytes();
        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        bin.reset();

        // System.out.println("Test: creating content node");
        Node contentNode = node1.addNode("jcr:content", "nt:resource");
        Binary binary = session.getValueFactory().createBinary(bin);
        contentNode.setProperty("jcr:data", binary);
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

        getSession().save();

    }

    @Test
    public void shouldModifyDocument() throws Exception {
        Node file = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        PropertyIterator it = file.getProperties();
        while (it.hasNext()) {
            Object val = it.nextProperty();
            printMessage("property=>" + val);
        }
        file.setProperty("StringProp", "modeshape");
        getSession().save();
    }

    @Test
    public void shouldBeAbleToMoveExternalNodes() throws Exception {
        assertNotNull(session.getNode("/cmis/My_Folder-0-0/My_Document-1-0"));
        ((Workspace)session.getWorkspace()).move("/cmis/My_Folder-0-0/My_Document-1-0", "/cmis/My_Folder-0-1/My_Document-1-X");
        Node file = session.getNode("/cmis/My_Folder-0-1/My_Document-1-X");
        assertNotNull(file);
        assertNotNull(session.getNode("/cmis/My_Folder-0-0"));
        ((Workspace)session.getWorkspace()).move("/cmis/My_Folder-0-0", "/cmis/My_Folder-0-X");
        Node folder = session.getNode("/cmis/My_Folder-0-X");
        assertNotNull(folder);
        assertEquals("nt:folder", folder.getPrimaryNodeType().getName());
        //undo the moves so that the original folder and document are unchaged (they are used by the other tests as well)
        ((Workspace) session.getWorkspace()).move("/cmis/My_Folder-0-1/My_Document-1-X", "/cmis/My_Folder-0-X/My_Document-1-0");
        ((Workspace) session.getWorkspace()).move("/cmis/My_Folder-0-X", "/cmis/My_Folder-0-0");
    }
    
    @Test
    public void shouldContainAccessList() throws Exception {
        AccessControlManager acm = session.getAccessControlManager();
        AccessControlPolicy[] policies = acm.getPolicies("/cmis/My_Folder-0-0");
        assertEquals(1, policies.length);
    }

}
