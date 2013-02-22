/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.connector;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ValueFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Provide integration testing of the CMIS connector with OpenCMIS InMemory
 * Repository.
 *
 * @author Alexander Voloshyn
 * @version 1.0 2/20/2013
 */
public class CmisConnectorIT extends MultiUseAbstractTest {
    /**
     * Test OpenCMIS InMemory Server URL.
     * <p/>
     * This OpenCMIS InMemory server instance should be started by maven cargo
     * plugin at pre integration stage.
     */
    private static final String CMIS_URL = "http://localhost:8090/";

    @BeforeClass
    public static void beforeAll() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("config/repository-1.json");
        startRepository(config);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Test
    public void testDirectChemistryConnect() {
        // default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE,
                BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE,
                CMIS_URL + "services/ACLService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE,
                CMIS_URL + "services/DiscoveryService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE,
                CMIS_URL + "services/MultiFilingService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE,
                CMIS_URL + "services/NavigationService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE,
                CMIS_URL + "services/ObjectService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE,
                CMIS_URL + "services/PolicyService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE,
                CMIS_URL + "services/RelationshipService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE,
                CMIS_URL + "services/RepositoryService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE,
                CMIS_URL + "services/VersioningService?wsdl");
        // Default repository id for in memory server is A1
        parameter.put(SessionParameter.REPOSITORY_ID, "A1");

        // create session
        final Session session = factory.createSession(parameter);
        assertTrue("Chemistry session should exists.", session != null);
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
        Node repoInfo = getSession().getNode("/cmis/repository");
        assertEquals("OpenCMIS InMemory-Server", repoInfo.getProperty("cmis:productName").getString());
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

        Node node3 = getSession().getNode(
                "/cmis/My_Folder-0-0/My_Folder-1-0/My_Folder-2-0");
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
        Property value = file.getProperty("cmis:data");

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

    //-----------------------------------------------------------------------/
    // Folder cmis build-in properties
    //-----------------------------------------------------------------------/
    @Test
    public void shouldAccessObjectIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String objectId = node.getProperty("cmis:objectId").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shouldAccessNamePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getProperty("cmis:name").getString();
        assertEquals("My_Folder-0-0", name);
    }

    @Test
    public void shouldAccessBaseTypeIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getProperty("cmis:baseTypeId").getString();
        assertEquals("cmis:folder", name);
    }

    @Test
    public void shouldAccessObjectTypeIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getProperty("cmis:objectTypeId").getString();
        assertEquals("cmis:folder", name);
    }

    @Test
    public void shouldAccessCreatedByPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String name = node.getProperty("cmis:createdBy").getString();
        assertEquals("unknown", name);
    }

    @Test
    public void shouldAccessCreationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        Calendar date = node.getProperty("cmis:creationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shouldAccessModificationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        Calendar date = node.getProperty("cmis:lastModificationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shouldAccessPathPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String path = node.getProperty("cmis:path").getString();
        assertEquals("/My_Folder-0-0", path);
    }

    @Test
    public void shouldAccessParentIdPropertyForFolder() throws Exception {
        Node root = getSession().getNode("/cmis");
        String rootId = root.getProperty("cmis:objectId").getString();

        Node node = getSession().getNode("/cmis/My_Folder-0-0");
        String parentId = node.getProperty("cmis:parentId").getString();
        assertEquals(rootId, parentId);
    }

    //-----------------------------------------------------------------------/
    // Document cmis build-in properties
    //-----------------------------------------------------------------------/
    @Test
    public void shouldAccessObjectIdPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String objectId = node.getProperty("cmis:objectId").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shouldAccessNamePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("cmis:name").getString();
        assertEquals("My_Document-1-0", name);
    }

    @Test
    public void shouldAccessBaseTypeIdPropertyDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("cmis:baseTypeId").getString();
        assertEquals("cmis:document", name);
    }

    @Test
    public void shouldAccessObjectTypeIdPropertyDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("cmis:objectTypeId").getString();
        assertEquals("ComplexType", name);
    }

    @Test
    public void shouldAccessCreatedByPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("cmis:createdBy").getString();
        assertEquals("unknown", name);
    }

    @Test
    public void shouldAccessLastModifiedByPropertyForDocument()
            throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        String name = node.getProperty("cmis:lastModifiedBy").getString();
        assertEquals("unknown", name);
    }

    @Test
    public void shouldAccessCreationDatePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        Calendar date = node.getProperty("cmis:creationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shouldAccessModificationDatePropertyForDocument()
            throws Exception {
        Node node = getSession().getNode("/cmis/My_Folder-0-0/My_Document-1-0");
        Calendar date = node.getProperty("cmis:lastModificationDate").getDate();
        assertTrue(date != null);
    }

    //    @Test
    public void shouldCreateFolder() throws Exception {
        Node root = getSession().getNode("/cmis/src");

        Node node = root.addNode("test", "cmis:folder");
        node.setProperty("cmis:name", "test-name");

        root = getSession().getNode("/cmis/src");
        Node node1 = root.addNode("test-1", "cmis:document");

        byte[] content = "Hello World".getBytes();
        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        bin.reset();

        ValueFactory valueFactory = getSession().getValueFactory();
        node1.setProperty("cmis:data", valueFactory.createBinary(bin));
        node1.setProperty("cmis:mimeType", "text/plain");

//        Node node2 = node.addNode("org", "cmis:folder");
        getSession().save();
        assertTrue(node != null);

    }

    //    @Test
    public void shouldCreateDocument() throws Exception {
        System.out.println("Creating document test case:");
        Node root = getSession().getNode("/cmis/src");
        Node node = root.addNode("readme", "cmis:document");
        System.out.println("Got node");
        node.setProperty("cmis:name", "test-name");
        getSession().save();
        assertTrue(node != null);
    }

    @Test
    public void shouldModifyDocument() throws Exception {
        Node pom = getSession().getNode("/cmis/My_Folder-0-1");
        pom.setProperty("cmis:name", "My_Folder-0-1_NEW_NAME");

        getSession().save();
    }
}
