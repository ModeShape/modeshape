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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class CmisConnectorTest extends AbstractConnectorTest {

    @Before
    public void before() throws Exception {
    }

    @Test
    public void shouldAccessRootFolder() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertTrue(root != null);
    }

    @Test
    public void testRootFolderNodeType() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertEquals("cmis:folder",root.getPrimaryNodeType().getName());
    }

    @Test
    public void testRootFolderName() throws Exception {
        Node root = getSession().getNode("/cmis");
        assertEquals("cmis", root.getName());
    }

    @Test
    public void shouldAccessRepositoryInfo() throws Exception {
        Node repoInfo = getSession().getNode("/cmis/repository");
        assertEquals("Dummy cmis repository", repoInfo.getProperty("cmis:productName").getString());
        assertEquals("Modeshape", repoInfo.getProperty("cmis:vendorName").getString());
        assertEquals("1.0", repoInfo.getProperty("cmis:productVersion").getString());
    }


    @Test
    public void shouldAccessFolderByPath()  throws Exception {
        Node root = getSession().getNode("/cmis");
        assertTrue(root != null);

        Node node1 = getSession().getNode("/cmis/src");
        assertTrue(node1 != null);

        Node node2 = getSession().getNode("/cmis/src/main");
        assertTrue(node2 != null);

        Node node3 = getSession().getNode("/cmis/src/main/java");
        assertTrue(node3 != null);
    }


    @Test
    public void shouldAccessDocumentPath()  throws Exception {
        Node file = getSession().getNode("/cmis/src/pom.xml");
        assertTrue(file != null);
    }

    @Test
    public void shouldAccessBinaryContent() throws Exception {
        Node file = getSession().getNode("/cmis/src/pom.xml");
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

        assertEquals("Hello world", s);
    }

    //-----------------------------------------------------------------------/
    // Folder cmis build-in properties
    //-----------------------------------------------------------------------/
    @Test
    public void shoudlAccessObjectIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String objectId = node.getProperty("cmis:objectId").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shoudlAccessNamePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String name = node.getProperty("cmis:name").getString();
        assertEquals("src", name);
    }

    @Test
    public void shoudlAccessBaseTypeIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String name = node.getProperty("cmis:baseTypeId").getString();
        assertEquals("cmis:folder", name);
    }

    @Test
    public void shoudlAccessObjectTypeIdPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String name = node.getProperty("cmis:objectTypeId").getString();
        assertEquals("cmis:folder", name);
    }

    @Test
    public void shoudlAccessCreatedByPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String name = node.getProperty("cmis:createdBy").getString();
        assertEquals("modeshape", name);
    }

    @Test
    public void shoudlAccessLastModifiedByPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String name = node.getProperty("cmis:lastModifiedBy").getString();
        assertEquals("modeshape", name);
    }

    @Test
    public void shoudlAccessCreationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        Calendar date = node.getProperty("cmis:creationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shoudlAccessModificationDatePropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        Calendar date = node.getProperty("cmis:lastModificationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shoudlAccessPathPropertyForFolder() throws Exception {
        Node node = getSession().getNode("/cmis/src");
        String path = node.getProperty("cmis:path").getString();
        assertEquals("/src", path);
    }

    @Test
    public void shoudlAccessParentIdPropertyForFolder() throws Exception {
        Node root = getSession().getNode("/cmis");
        String rootId = root.getProperty("cmis:objectId").getString();

        Node node = getSession().getNode("/cmis/src");
        String parentId = node.getProperty("cmis:parentId").getString();
        assertEquals(rootId, parentId);
    }

    //-----------------------------------------------------------------------/
    // Document cmis build-in properties
    //-----------------------------------------------------------------------/
    @Test
    public void shoudlAccessObjectIdPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String objectId = node.getProperty("cmis:objectId").getString();
        assertTrue(objectId != null);
    }

    @Test
    public void shoudlAccessNamePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String name = node.getProperty("cmis:name").getString();
        assertEquals("pom.xml", name);
    }

    @Test
    public void shoudlAccessBaseTypeIdPropertyDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String name = node.getProperty("cmis:baseTypeId").getString();
        assertEquals("cmis:document", name);
    }

    @Test
    public void shoudlAccessObjectTypeIdPropertyDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String name = node.getProperty("cmis:objectTypeId").getString();
        assertEquals("cmis:document", name);
    }

    @Test
    public void shoudlAccessCreatedByPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String name = node.getProperty("cmis:createdBy").getString();
        assertEquals("modeshape", name);
    }

    @Test
    public void shoudlAccessLastModifiedByPropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        String name = node.getProperty("cmis:lastModifiedBy").getString();
        assertEquals("modeshape", name);
    }

    @Test
    public void shoudlAccessCreationDatePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        Calendar date = node.getProperty("cmis:creationDate").getDate();
        assertTrue(date != null);
    }

    @Test
    public void shoudlAccessModificationDatePropertyForDocument() throws Exception {
        Node node = getSession().getNode("/cmis/src/pom.xml");
        Calendar date = node.getProperty("cmis:lastModificationDate").getDate();
        assertTrue(date != null);
    }

    @Test
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

        Node node2 = node.addNode("org", "cmis:folder");
        getSession().save();
        assertTrue(node != null);

    }

    @Test
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
        Node pom = getSession().getNode("/cmis");
        pom.setProperty("cmis:name", "cmis--");

        getSession().save();
    }
}
