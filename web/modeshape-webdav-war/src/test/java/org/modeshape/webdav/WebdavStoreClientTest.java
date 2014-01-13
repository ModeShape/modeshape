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

package org.modeshape.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;

/**
 * Integration test for the {@link org.modeshape.webdav.IWebdavStore} default implementation, tested using a real web-dav
 * compliant client. This should be updated whenever the default webdav support is updated (e.g. when fixing compliance bugs).
 * <p>
 * Ideally this would contain at least the test cases that <a href="http://www.webdav.org/neon/litmus">Litmus</a> has.
 * </p>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class WebdavStoreClientTest {

    /**
     * The servlet context name comes from the pom.xml
     */
    private static final String SERVER_CONTEXT = "http://localhost:8090/webdav";

    protected Sardine sardine;

    @Before
    public void beforeEach() throws Exception {
        sardine = initializeWebDavClient();
    }

    protected Sardine initializeWebDavClient() throws SardineException {
        return SardineFactory.begin();
    }

    @Test
    public void shouldConnectWithValidCredentials() throws Exception {
        String uri = resourceUri(null);
        assertTrue(sardine.exists(uri));
        try {
            assertNotNull(sardine.getResources(uri));
        } catch (SardineException e) {
            // there seems to be a bug in Sardine146 (which is the only one available via Maven atm)
            assertEquals(302, e.getStatusCode());
        }
    }

    @Test
    public void shouldCreateFolder() throws Exception {
        String folderName = testFolder();
        String uri = resourceUri(folderName);
        sardine.createDirectory(uri);
        assertTrue(sardine.exists(uri));
        DavResource folder = getResourceAtURI(uri);
        assertEquals(0l, folder.getContentLength().longValue());
    }

    private String testFolder() {
        return "testFolder" + UUID.randomUUID().toString();
    }

    protected DavResource getResourceAtURI( String uri ) throws SardineException {
        List<DavResource> resourcesList = sardine.getResources(uri);
        assertEquals(1, resourcesList.size());
        return resourcesList.get(0);
    }

    @Test
    public void shouldCreateFile() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("textfile.txt");
        assertNotNull(fileStream);

        String fileUri = folderUri + "/testFile" + UUID.randomUUID().toString();
        sardine.put(fileUri, fileStream);

        assertTrue(sardine.exists(fileUri));
        DavResource file = getResourceAtURI(fileUri);
        byte[] fileBytes = IoUtil.readBytes(getClass().getClassLoader().getResourceAsStream("textfile.txt"));
        assertEquals(fileBytes.length, file.getContentLength().longValue());
    }

    protected String resourceUri( String resourceName ) {
        return !StringUtil.isBlank(resourceName) ? getServerContext() + "/" + resourceName : getServerContext();
    }

    protected String getServerContext() {
        return SERVER_CONTEXT;
    }

    @Test
    public void shouldSetCustomPropertiesOnFolder() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        assertCustomPropertiesOnResource(folderUri);
    }

    @Test
    public void shouldIgnoreCustomPropertiesOnFolderIfNotPresent() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);

        sardine.setCustomProps(folderUri, null, null);
        sardine.setCustomProps(folderUri, null, Arrays.asList("missing_prop"));
    }

    @Test
    public void shouldSetCustomPropertiesOnFile() throws Exception {
        String folderUri = resourceUri(testFolder());
        sardine.createDirectory(folderUri);
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("textfile.txt");
        String fileUri = folderUri + "/testFile" + UUID.randomUUID().toString();
        sardine.put(fileUri, fileStream);

        assertCustomPropertiesOnResource(fileUri);
    }

    private void assertCustomPropertiesOnResource( String resourceUri ) throws SardineException {
        Map<String, String> propertiesToAdd = new HashMap<String, String>();
        propertiesToAdd.put("prop1", "value1");
        propertiesToAdd.put("prop2", "value2");
        sardine.setCustomProps(resourceUri, propertiesToAdd, null);

        DavResource resource = sardine.getResources(resourceUri).get(0);
        Map<String, String> customProps = resource.getCustomProps();
        assertEquals("value1", customProps.get("prop1"));
        assertEquals("value2", customProps.get("prop2"));

        sardine.setCustomProps(resourceUri, null, Arrays.asList("prop2", "prop3"));
        resource = sardine.getResources(resourceUri).get(0);
        customProps = resource.getCustomProps();
        assertEquals("value1", customProps.get("prop1"));
        assertFalse(customProps.containsKey("prop2"));
    }
}
