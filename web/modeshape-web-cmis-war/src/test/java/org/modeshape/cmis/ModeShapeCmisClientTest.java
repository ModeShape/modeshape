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
package org.modeshape.cmis;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.tika.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for CMIS bridge
 *
 * @author kulikov
 */
public class ModeShapeCmisClientTest {

    private Session session;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        // default factory implementation
        SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        parameter.put(SessionParameter.USER, "dnauser");
        parameter.put(SessionParameter.PASSWORD, "password");

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, serviceUrl("ACLService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, serviceUrl("/DiscoveryService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, serviceUrl("MultiFilingService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, serviceUrl("NavigationService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, serviceUrl("ObjectService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, serviceUrl("/PolicyService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, serviceUrl("RelationshipService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, serviceUrl("RepositoryService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, serviceUrl("VersioningService?wsdl"));

        parameter.put(SessionParameter.REPOSITORY_ID, "cmis_repo:default");

        // create session
        session = factory.createSession(parameter, null, new StandardAuthenticationProvider(), null, null);
    }

    @Test
    public void shouldAccessRootFolder() throws Exception {
        Folder root = session.getRootFolder();
        System.out.println("Root: " + root);
    }

    @Test
    public void shouldObtainContentHashProperty() throws Exception {
        //Create test folder
        Map<String, Object> folderProperties = new HashMap<String, Object>();
        folderProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        folderProperties.put(PropertyIds.NAME, "test");
        Folder folder = session.getRootFolder().createFolder(folderProperties);

        //Create the document from the test file
        Map<String, Object> fileProperties = new HashMap<String, Object>();
        fileProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        fileProperties.put(PropertyIds.NAME, "file");
        
        String contents = "Hello";
        ContentStream cos = session.getObjectFactory().createContentStream("file", 
                contents.length(), "text/plain", 
                new ByteArrayInputStream(contents.getBytes()));
        
        // create a major version
        Document file = folder.createDocument(fileProperties, cos, VersioningState.MAJOR);
        file.getProperties();
        String s = file.getProperty(PropertyIds.CONTENT_STREAM_HASH).getValueAsString();
        assertEquals("{sha-1}f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0", s);
    }
    
    @Test
    public void testVersioning() throws Exception {
        //Create test folder
        Map<String, Object> folderProperties = new HashMap<String, Object>();
        folderProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        folderProperties.put(PropertyIds.NAME, "test");
        Folder folder = session.getRootFolder().createFolder(folderProperties);

        //Create the document from the test file
        Map<String, Object> fileProperties = new HashMap<String, Object>();
        fileProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        fileProperties.put(PropertyIds.NAME, "file");
        fileProperties.put(PropertyIds.VERSION_LABEL, "initial");
        
        String[] contents = new String[] {"Hello World!", "Hello World", "Hello"};
        
        ContentStream cos = session.getObjectFactory().createContentStream("file", 
                contents[2].length(), "text/plain", 
                new ByteArrayInputStream(contents[2].getBytes()));
        
        // create a major version
        Document file = folder.createDocument(fileProperties, cos, VersioningState.MAJOR);
        
        //---------------------------------
        ObjectId nextVersion = file.checkOut();
        Document file2 = (Document) session.getObject(nextVersion);
        
        cos = session.getObjectFactory().createContentStream("file", 
                contents[1].length(), "text/plain", 
                new ByteArrayInputStream(contents[1].getBytes()));
        
        file2.checkIn(true, null, cos, "version1");

        //---------------------------------
        nextVersion = file.checkOut();
        Document file3 = (Document) session.getObject(nextVersion);
        
        cos = session.getObjectFactory().createContentStream("file", 
                contents[0].length(), "text/plain", 
                new ByteArrayInputStream(contents[0].getBytes()));
        
        file3.checkIn(true, null, cos, "version2");

        //---------------------------------------
        List<Document> versions = file.getAllVersions();
        assertEquals(3, versions.size());
        
        int i = 0;
        for (Document doc : versions) {
            String verId = doc.getId();
            Document versDoc = (Document)session.getObject(verId);
            String s = IOUtils.toString(versDoc.getContentStream().getStream());
            
            assertEquals(contents[i], s);
            i++;
        }
        
    }

    private String serviceUrl(String serviceMethod) {
        return "http://localhost:8090/modeshape-cmis/services/" + serviceMethod;
    }
}
