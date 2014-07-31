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
package org.modeshape.web.jcr.webdav;

import com.googlecode.sardine.DavResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.StringUtil;
import org.modeshape.webdav.WebdavStoreClientTest;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Unit test for the {@link ModeShapeWebdavStore} implementation, tested using a real web-dav compliant client. This test should
 * contains any relevant WebDAV tests from the point of the view of the JCR - WebDAV integration. Any WebDAV spec issues, should
 * be tested via {@link WebdavStoreClientTest}.
 * 
 * @author Horia Chiorean
 */
public class ModeShapeWebdavStoreClientTest extends WebdavStoreClientTest {

    /**
     * The name of the repository is dictated by the "repository-config.json" configuration file loaded by the
     * "org.modeshape.jcr.JCR_URL" parameter in the "web.xml" file.
     */
    private static final String REPOSITORY_NAME = "webdav_repo";
    private static final String SERVER_CONTEXT = "http://localhost:8090/webdav-jcr";

    @Override
    protected Sardine initializeWebDavClient() throws SardineException {
        // Configured in pom
        return SardineFactory.begin("dnauser", "password");
    }

    @Test
    public void shouldConnectToRepository() throws Exception {
        String uri = getJcrServerUrl(null);
        assertTrue(sardine.exists(uri));
        assertNotNull(sardine.getResources(uri));
    }

    @Test
    public void shouldConnectToRepositoryAndWorkspace() throws Exception {
        String uri = getJcrServerUrl(getDefaultWorkspaceName());
        assertTrue(sardine.exists(uri));
        assertNotNull(sardine.getResources(uri));
    }

    @Test
    public void shouldNotFindInvalidRepository() throws Exception {
        String uri = getServerContext() + "/missing_repo";
        assertFalse(sardine.exists(uri));
    }

    @Test
    public void shouldNotFindInvalidWorkspace() throws Exception {
        String uri = getJcrServerUrl("missingWS");
        assertFalse(sardine.exists(uri));
    }

    @Test
    @FixFor( "MODE-1542" )
    public void shouldCreateLargeFile() throws Exception {
        int binarySize = 100 * (1 << 20); //100 MB
        byte[] binaryData = new byte[binarySize];
        new Random().nextBytes(binaryData);

        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);
        String fileUri = testFile(folderUri);

        sardine.put(fileUri, new ByteArrayInputStream(binaryData), "application/octet-stream");
        assertTrue(sardine.exists(fileUri));
        DavResource file = getResourceAtURI(fileUri);

        assertEquals(binarySize, file.getContentLength().intValue());
    }

    private String testFile( String folderUri ) {
        return folderUri + "/testFile" + UUID.randomUUID().toString();
    }

    @Test
    @FixFor( "MODE-984" )
    public void shouldRetrieveFolderCustomProperties() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        DavResource resource = sardine.getResources(folderUri).get(0);
        Map<String, String> customProperties = resource.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertEquals("nt:folder", customProperties.get("primaryType"));
        assertNotNull(customProperties.get("created"));
        assertNotNull(customProperties.get("createdBy"));
    }

    @Test
    @FixFor( "MODE-984" )
    public void shouldRetrieveFileCustomProperties() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        byte[] binaryData = new byte[1024];
        new Random().nextBytes(binaryData);
        String fileUri = testFile(folderUri);
        sardine.put(fileUri, new ByteArrayInputStream(binaryData), "application/octet-stream");

        DavResource resource = sardine.getResources(fileUri).get(0);
        Map<String, String> customProperties = resource.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertEquals("nt:file", customProperties.get("primaryType"));
        assertNotNull(customProperties.get("created"));
        assertNotNull(customProperties.get("createdBy"));
    }

    @Override
    @FixFor( "MODE-984" )
    public void shouldSetCustomPropertiesOnFile() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        byte[] binaryData = new byte[1024];
        new Random().nextBytes(binaryData);
        String fileUri = testFile(folderUri);
        sardine.put(fileUri, new ByteArrayInputStream(binaryData), "application/octet-stream");

        Map<String, String> customProps = new HashMap<String, String>();
        customProps.put("myProp", "myValue");
        sardine.setCustomProps(fileUri, customProps, null);

        DavResource resource = sardine.getResources(fileUri).get(0);
        Map<String, String> customProperties = resource.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertFalse(customProperties.containsKey("myProp"));
    }

    @Override
    public void shouldSetCustomPropertiesOnFolder() throws Exception {
        //custom properties cannot be set on nt:folder
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        Map<String, String> customProps = new HashMap<String, String>();
        customProps.put("myProp", "myValue");
        sardine.setCustomProps(folderUri, customProps, null);
        DavResource resource = sardine.getResources(folderUri).get(0);
        Map<String, String> customProperties = resource.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertFalse(customProperties.containsKey("myProp"));
    }

    @Test
    @FixFor( "MODE-2010" )
    public void shouldIgnoreMultiValuedProperties() throws Exception {
        //created via initial content
        String nodeUri = resourceUri("node");
        DavResource resource = sardine.getResources(nodeUri).get(0);
        Map<String, String> customProperties = resource.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertEquals("value", customProperties.get("single-value-prop"));
    }

    @Test
    @FixFor( "MODE-2243" )
    public void shouldEscapeIllegalCharsInXMLValues() throws Exception {
        //created via initial content
        String folderUri = resourceUri("folder");
        DavResource folder = sardine.getResources(folderUri).get(0);
        Map<String, String> customProperties = folder.getCustomProps();
        assertTrue(!customProperties.isEmpty());
        assertEquals("nt:folder", customProperties.get("primaryType"));
        //nt:folder has a created mixin which will auto-set the next 2 props
        assertNotNull(customProperties.get("created"));
        assertEquals("<modeshape-worker>", customProperties.get("createdBy"));
    }

    private String testFolder() {
        return "testDirectory" + UUID.randomUUID().toString();
    }

    protected String getDefaultWorkspaceName() {
        return "default";
    }

    @Override
    protected String getServerContext() {
        return SERVER_CONTEXT;
    }

    protected String getRepositoryName() {
        return REPOSITORY_NAME;
    }

    @Override
    protected String resourceUri( String resourceName ) {
        String rootUrl = getJcrServerUrl(getDefaultWorkspaceName());
        return !StringUtil.isBlank(resourceName) ? rootUrl + "/" + resourceName : rootUrl;
    }

    private String getJcrServerUrl( String workspaceName ) {
        String serverContext = getServerContext();
        assertNotNull(serverContext);
        String repositoryName = getRepositoryName();
        assertNotNull(repositoryName);

        String baseUrl = serverContext + "/" + repositoryName;
        return workspaceName != null ? baseUrl + "/" + workspaceName : baseUrl;
    }
}
