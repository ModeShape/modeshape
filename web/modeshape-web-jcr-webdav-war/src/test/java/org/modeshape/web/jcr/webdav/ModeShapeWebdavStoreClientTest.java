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

        String folderUri = resourceUri("testDirectory" + UUID.randomUUID().toString());
        sardine.createDirectory(folderUri);
        String fileUri = folderUri + "/testFile" + UUID.randomUUID().toString();

        sardine.put(fileUri, new ByteArrayInputStream(binaryData), "application/octet-stream");
        assertTrue(sardine.exists(fileUri));
        DavResource file = getResourceAtURI(fileUri);

        assertEquals(binarySize, file.getContentLength().intValue());
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
