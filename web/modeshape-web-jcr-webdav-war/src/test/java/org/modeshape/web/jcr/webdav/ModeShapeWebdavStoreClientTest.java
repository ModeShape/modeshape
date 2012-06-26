package org.modeshape.web.jcr.webdav;


import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Unit test for the {@link ModeShapeWebdavStore} implementation, tested using a real web-dav compliant client.
 * The test doesn't cover all the WebDav possible scenarios, but should be enough to smoke out issues.
 *
 * @author Horia Chiorean
 */
public class ModeShapeWebdavStoreClientTest {

    /**
     * The name of the repository is dictated by the "repository-config.json" configuration file loaded by the
     * "org.modeshape.jcr.JCR_URL" parameter in the "web.xml" file.
     */
    private static final String REPOSITORY_NAME = "webdav_repo";
    private static final String SERVER_CONTEXT = "http://localhost:8090/webdav";

    private Sardine sardine;

    @Before
    public void beforeEach() throws Exception {
        sardine = initializeWebDavClient();
    }

    protected Sardine initializeWebDavClient() throws SardineException {
        // Configured in pom
        return SardineFactory.begin("dnauser", "password");
    }

    @Test
    public void shouldNotConnectWithInvalidCredentials() throws Exception {
        sardine = SardineFactory.begin();
        assertFalse(sardine.exists(getServerUrl(null)));
    }

    @Test
    public void shouldConnectToRepository() throws Exception {
        String uri = getServerUrl(null);
        assertTrue(sardine.exists(uri));
        assertNotNull(sardine.getResources(uri));
    }

    @Test
    public void shouldConnectToRepositoryAndWorkspace() throws Exception {
        String uri = getServerUrl(getDefaultWorkspaceName());
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
        String uri = getServerUrl("missingWS");
        assertFalse(sardine.exists(uri));
    }

    @Test
    public void shouldCreateFolder() throws Exception {
        String folderName = "testFolder" + UUID.randomUUID().toString();
        String uri = getServerUrl(getDefaultWorkspaceName()) + "/" + folderName;
        sardine.createDirectory(uri);
        assertTrue(sardine.exists(uri));
        DavResource folder = getResourceAtURI(uri);
        assertEquals(0l, folder.getContentLength().longValue());
    }

    private DavResource getResourceAtURI( String uri ) throws SardineException {
        List<DavResource> resourcesList = sardine.getResources(uri);
        assertEquals(1, resourcesList.size());
        return resourcesList.get(0);
    }

    @Test
    public void shouldCreateFile() throws Exception {
        String folderUri = getServerUrl(getDefaultWorkspaceName()) + "/testDirectory" + UUID.randomUUID().toString();
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

    private String getServerUrl(String workspaceName) {
        String serverContext = getServerContext();
        assertNotNull(serverContext);
        String repositoryName = getRepositoryName();
        assertNotNull(repositoryName);

        String baseUrl = serverContext + "/" + repositoryName;
        return workspaceName != null ? baseUrl + "/" + workspaceName : baseUrl;
    }

    protected String getDefaultWorkspaceName() {
        return "default";
    }

    protected String getServerContext() {
        return SERVER_CONTEXT;
    }

    protected String getRepositoryName() {
        return REPOSITORY_NAME;
    }
}
