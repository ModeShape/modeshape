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
package org.modeshape.web.jcr.rest.client.json;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.nodetype.NodeType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.web.jcr.rest.client.IJcrConstants;
import org.modeshape.web.jcr.rest.client.IRestClient;
import org.modeshape.web.jcr.rest.client.Status;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>JsonRestClientTest</code> class is a test class for the {@link JsonRestClient JSON REST client} object.
 * <p>
 * One container: mvn -P cargo-1 clean install assembly:assembly
 * <p>
 * Two containers: mvn -P cargo-1,cargo-2 clean install assembly:assembly
 */
public final class JsonRestClientTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    // user and password configured in pom
    private static final String PSWD = "password";
    private static final String USER = "dnauser";
    private static final String URL = "http://localhost:8090/resources";

    private static final String REPOSITORY_NAME = "repo";
    private static final String WORKSPACE_NAME = "default";

    private static final String WORKSPACE_PATH = "/myproject/myfolder/";
    private static final String FILE_PATH = WORKSPACE_PATH + "document.txt";
    private static final String BINARY_FILE_PATH = WORKSPACE_PATH + "picture.jpg";
    private static final String DDL_FILE_PATH = WORKSPACE_PATH + "oracle_test_create.ddl";
    private static final String BOOKS_MODEL_PATH = WORKSPACE_PATH + "Books_Oracle.xmi";
    private static final String BOOKS_MODEL_PATH2 = WORKSPACE_PATH + "Books_Oracle2.xmi";

    private static final String WORKSPACE_UNUSUALPATH = "/myproject/My.Test - Folder/";
    private static final String FILE_UNUSUALPATH = WORKSPACE_UNUSUALPATH + "Test File_.a-().txt";

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private IRestClient restClient;
    private Server server;
    private Repository repository1;
    private Workspace workspace1;

    private File textfile = null;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Before
    public void beforeEach() throws Exception {
        this.restClient = new JsonRestClient();
        // Create and validate the server ...
        this.server = new Server(URL, USER, PSWD);
        try {
            this.server = this.restClient.validate(server);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
        // Now create the repository and workspace objects
        this.repository1 = new Repository(REPOSITORY_NAME, server);
        this.workspace1 = new Workspace(WORKSPACE_NAME, repository1);
    }

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldGetRepositories() throws Exception {
        Collection<Repository> repositories = this.restClient.getRepositories(server);
        assertThat(repositories.size(), equalTo(1));
        Repository repo = repositories.iterator().next();
        assertThat(repo, is(repository1));
        if (!repo.getMetadata().isEmpty()) {
            Map<String, Object> metadata = repo.getMetadata();
            assertThat(metadata.get("jcr.specification.name"), is((Object)"Content Repository for Java Technology API"));
            assertThat(metadata.get("jcr.specification.version"), is((Object)"2.0"));
            assertThat(metadata.get("jcr.repository.name"), is((Object)"ModeShape"));
            assertThat(metadata.get("jcr.repository.vendor.url"), is((Object)"http://www.modeshape.org"));
            assertThat(metadata.get("jcr.repository.version").toString().startsWith("4."), is(true));
            assertThat(metadata.get("option.versioning.supported"), is((Object)"true"));
        }
    }

    // Test is not currently working as a unit test, cause it throws an exception when using the default cargo setup
    // but does work when pointed at a local jbossas server
    // TODO: determine how to add/setup the local cargo server with cnd files
    @Ignore
    @Test
    public void shouldGetNodeTypes() throws Exception {
        Map<String, NodeType> nodeTypes = this.restClient.getNodeTypes(repository1);

        // this is currently the number returned from the default jbossas installation
        // assertThat(results.size(), is(2));

        for (Iterator<NodeType> it = nodeTypes.values().iterator(); it.hasNext();) {
            NodeType nt = it.next();
            System.out.println("NODETYPE: " + nt.getName());
            System.out.println("   declared supertypes:             " + nt.getSupertypes());
            System.out.println("   declared property definitions:   " + nt.getDeclaredPropertyDefinitions());
            System.out.println("   declared child node definitions: " + nt.getDeclaredPropertyDefinitions());
        }
    }

    @Test
    public void shouldGetWorkspaces() throws Exception {
        Collection<Workspace> workspaces = this.restClient.getWorkspaces(repository1);
        assertThat(workspaces.size(), is(1));
        assertThat(workspaces.iterator().next(), is(workspace1));
    }

    @Test
    public void shouldNotUnpublishNonexistentFile() throws Exception {
        File file = new File("bogusfile");
        Status status = this.restClient.unpublish(workspace1, WORKSPACE_PATH, file);

        printExceptionIfStatusIsError(status);

        assertThat(status.getMessage(), status.isInfo(), is(true));
    }

    private void printExceptionIfStatusIsError( Status status ) {
        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }
    }

    @Test
    public void shouldPublishBinaryResource() throws Exception {
        URL binaryFile = getClass().getResource(BINARY_FILE_PATH);
        assertThat(binaryFile, is(notNullValue()));

        // publish
        File file = new File(binaryFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_PATH, file);

        printExceptionIfStatusIsError(status);

        assertThat(status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(workspace1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(workspace1, WORKSPACE_PATH, file);
        assertThat(actual, is(expected));

        shouldUnpublishFile(file);
    }

    /**
     * this is not made a test because its called by other tests to publish the text resource for thier test, therefore this test
     * on its own is not needed.
     * 
     * @throws Exception
     */
    private void shouldPublishTextResource() throws Exception {
        URL textFile = getClass().getResource(FILE_PATH);
        assertThat(textFile, is(notNullValue()));

        // publish
        textfile = new File(textFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_PATH, textfile);

        printExceptionIfStatusIsError(status);

        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, textfile), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(workspace1, WORKSPACE_PATH, textfile).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(workspace1, WORKSPACE_PATH, textfile);
        if (!expected.equals(actual)) {
            System.err.println("\nvan2expected: \n" + expected);
            System.err.println("\nvan2actual: \n" + actual);
        }
        assertThat(actual, is(expected));

    }

    @FixFor( "MODE-919" )
    @Test
    public void shouldPublishDdlResource() throws Exception {
        URL ddlFile = getClass().getResource(DDL_FILE_PATH);
        assertThat(ddlFile, is(notNullValue()));

        // publish
        File file = new File(ddlFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_PATH, file);

        printExceptionIfStatusIsError(status);

        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(workspace1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(workspace1, WORKSPACE_PATH, file);
        if (!expected.equals(actual)) {
            System.err.println("van1expected: \n" + expected);
            System.err.println("\nvan1actual: \n" + actual);
        }
        assertThat(actual, is(expected));

        shouldUnpublishFile(file);
    }

    @Test
    public void shouldPublishResourcesHavingNonLettersNonNumbersInName() throws Exception {
        URL textFile = getClass().getResource(FILE_UNUSUALPATH);
        assertThat(textFile, is(notNullValue()));

        // publish
        File file = new File(textFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_UNUSUALPATH, file);

        printExceptionIfStatusIsError(status);

        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_UNUSUALPATH, file), is(true));

        shouldUnpublishFile(file);

    }

    @Test
    public void shouldUnpublish() throws Exception {
        // first publish
        shouldPublishTextResource();

        Status status = this.restClient.unpublish(workspace1, WORKSPACE_PATH, textfile);

        printExceptionIfStatusIsError(status);

        assertThat(status.isOk(), is(true));

        // confirm it does not exist in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, textfile), is(false));
    }

    protected void shouldUnpublishFile( File file ) throws Exception {
        Status status = this.restClient.unpublish(workspace1, WORKSPACE_PATH, file);

        printExceptionIfStatusIsError(status);

        // confirm it does not exist in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, file), is(false));
    }

    @Test
    public void shouldQuery() throws Exception {
        // first publish
        shouldPublishTextResource();

        List<QueryRow> results = this.restClient.query(workspace1, IJcrConstants.XPATH, "/" + FILE_PATH);

        assertThat(results.size(), is(1));
        QueryRow row = results.get(0);

        assertThat(row.getColumnNames().size(), is(3));
        assertThat(row.getColumnNames().contains("jcr:score"), is(true));
        assertThat(row.getColumnNames().contains("jcr:path"), is(true));
        assertThat(row.getColumnNames().contains("jcr:primaryType"), is(true));

        assertThat(row.getColumnType("jcr:score"), is("DOUBLE"));
        assertThat(row.getColumnType("jcr:path"), is("STRING"));
        assertThat(row.getColumnType("jcr:primaryType"), is("STRING"));

        assertThat((String)row.getValue("jcr:path"), is("/myproject/myfolder/document.txt"));
        assertThat((String)row.getValue("jcr:primaryType"), is("nt:file"));

        shouldUnpublishFile(textfile);

    }

    @FixFor( "MODE-1130" )
    @Test
    public void shouldPublishNonVersionableResourceMultipleTimes() throws Exception {
        URL modelFile = getClass().getResource(BOOKS_MODEL_PATH);
        assertThat(modelFile, is(notNullValue()));

        // publish first time
        File file = new File(modelFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_PATH, file, false);

        // make sure no error when publishing the first time
        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(workspace1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(workspace1, WORKSPACE_PATH, file);
        assertThat(actual, is(expected));

        // publish second time
        status = this.restClient.publish(workspace1, WORKSPACE_PATH, file, false);

        // make sure no error when publishing the second time
        assertThat(status.getMessage(), status.isOk(), is(true));

        shouldUnpublishFile(file);
    }

    @FixFor( "MODE-1130" )
    @Test
    public void shouldPublishMVersionableResourceMultipleTimes() throws Exception {
        URL modelFile = getClass().getResource(BOOKS_MODEL_PATH2);
        assertThat(modelFile, is(notNullValue()));

        // publish first time
        File file = new File(modelFile.toURI());
        Status status = this.restClient.publish(workspace1, WORKSPACE_PATH, file, true);

        // make sure no error when publishing the first time
        if (!status.isOk()) {
            System.out.println(status + "\n");
            status.getException().printStackTrace();
        }
        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(workspace1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(workspace1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(workspace1, WORKSPACE_PATH, file);
        assertThat(actual, is(expected));

        // publish second time
        status = this.restClient.publish(workspace1, WORKSPACE_PATH, file, true);

        // make sure no error when publishing the second time
        if (!status.isOk()) {
            System.out.println(status + "\n");
            status.getException().printStackTrace();
        }
        assertThat(status.getMessage(), status.isOk(), is(true));

        shouldUnpublishFile(file);
        // TODO still need to verify that the 2 versions exist in repository
    }

    @FixFor( "MODE-1131" )
    @Test
    public void shouldQueryAndNotFailWhenNullValueAppearsInResultSet() throws Exception {
        // first publish
        shouldPublishTextResource();

        String path = WORKSPACE_PATH.substring(0, WORKSPACE_PATH.lastIndexOf("/"));
        String query = "SELECT [jcr:primaryType], [jcr:path], [jcr:title] FROM [nt:folder] WHERE [jcr:path] = '" + path + "'";
        List<QueryRow> results = this.restClient.query(workspace1, IJcrConstants.JCR_SQL2, query);

        assertThat(results.size(), is(1));

        QueryRow row = results.get(0);

        assertThat(row.getColumnNames().size(), is(3));
        assertThat(row.getColumnNames().contains("jcr:path"), is(true));
        assertThat(row.getColumnNames().contains("jcr:primaryType"), is(true));
        assertThat(row.getColumnNames().contains("jcr:title"), is(true));

        assertThat(row.getColumnType("jcr:path"), is("STRING"));
        assertThat(row.getColumnType("jcr:primaryType"), is("STRING"));
        assertThat(row.getColumnType("jcr:title"), is("STRING"));

        assertThat((String)row.getValue("jcr:path"), is(path));
        assertThat((String)row.getValue("jcr:primaryType"), is("nt:folder"));
        assertThat(row.getValue("jcr:title"), is(nullValue()));

        shouldUnpublishFile(textfile);
    }

    @Test
    @FixFor( "MODE-1619" )
    public void fileExistenceChecksShouldWork() throws Exception {
        String fileName = "restClient_testFile_" + UUID.randomUUID().toString();
        File file = new File("target", fileName);
        file.createNewFile();
        Status status = restClient.publish(workspace1, "/uploads/", file);
        assertTrue(status.isOk());
        assertTrue(restClient.fileExists(file, workspace1, "/uploads/"));

        status = restClient.unpublish(workspace1, "/uploads", file);
        assertTrue(status.isOk());
        assertFalse(restClient.fileExists(file, workspace1, "/uploads/"));

        assertTrue(file.delete());
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldCreatePublishAreaBelowRoot() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis();
        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title", "description");

        path = "/folderB_" + System.currentTimeMillis() + "/";
        response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title", "description");
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldAllowNullTitleAndDescription() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis();
        Status response = restClient.markAsPublishArea(workspace1, path, null, null);
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, null, null);
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldCreateIntermediatePathElementsWhenCreatingPublishArea() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis() + "/b/c";
        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title", "description");
    }

    @Test
    @FixFor( "MODE-1919" )
    public void markingPublishAreaShouldBeIdempotent() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis();
        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title", "description");

        restClient.markAsPublishArea(workspace1, path, "title1", "description1");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title1", "description1");
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldAllowChangingPublishedFolderToPublishArea() throws Exception {
        File file = new File("target", "restClient_testFile_" + UUID.randomUUID().toString());
        file.createNewFile();

        String path = "/folderA_" + System.currentTimeMillis() + "/";
        Status status = restClient.publish(workspace1, path, file);
        assertEquals(Status.OK_STATUS, status);

        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        assertFolderMarkedAsPublishArea(path, "title", "description");
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldAllowToUnmarkAsPublishArea() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis();
        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);
        response = restClient.unmarkAsPublishArea(workspace1, path);
        assertEquals(Status.OK_STATUS, response);
        assertFolderUnmarkedAsPublishArea(path);
    }

    @Test
    @FixFor( "MODE-1919" )
    public void unmarkAsPublishAreaShouldBeIdempotent() throws Exception {
        String path = "/folderA_" + System.currentTimeMillis();
        Status response = restClient.markAsPublishArea(workspace1, path, "title", "description");
        assertEquals(Status.OK_STATUS, response);

        response = restClient.unmarkAsPublishArea(workspace1, path);
        assertEquals(Status.OK_STATUS, response);
        assertFolderUnmarkedAsPublishArea(path);

        response = restClient.unmarkAsPublishArea(workspace1, path);
        assertEquals(Status.OK_STATUS, response);
        assertFolderUnmarkedAsPublishArea(path);
    }

    @Test
    @FixFor( "MODE-1919" )
    public void shouldAllowUnmarkingNonPublishArea() throws Exception {
        File file = new File("target", "restClient_testFile_" + UUID.randomUUID().toString());
        file.createNewFile();

        String path = "/folderA_" + System.currentTimeMillis() + "/";
        Status status = restClient.publish(workspace1, path, file);
        assertEquals(Status.OK_STATUS, status);

        Status response = restClient.unmarkAsPublishArea(workspace1, path);
        assertEquals(Status.OK_STATUS, response);
    }

    private void assertFolderMarkedAsPublishArea( String path,
                                                  String title,
                                                  String description ) throws Exception {
        String query = "SELECT [jcr:mixinTypes], [jcr:title], [jcr:description] FROM [nt:folder] WHERE [jcr:path] = '" + path
                       + "'";
        List<QueryRow> results = this.restClient.query(workspace1, IJcrConstants.JCR_SQL2, query);
        assertThat(results.size(), is(1));

        QueryRow row = results.get(0);
        assertEquals(IJcrConstants.PUBLISH_AREA_TYPE, row.getValue(IJcrConstants.MIXIN_TYPES_PROPERTY).toString());
        if (title != null) {
            assertEquals(title, row.getValue(IJcrConstants.PUBLISH_AREA_TITLE).toString());
        } else {
            assertNull(row.getValue(IJcrConstants.PUBLISH_AREA_TITLE));
        }

        if (description != null) {
            assertEquals(description, row.getValue(IJcrConstants.PUBLISH_AREA_DESCRIPTION).toString());
        } else {
            assertNull(row.getValue(IJcrConstants.PUBLISH_AREA_DESCRIPTION));
        }
    }

    private void assertFolderUnmarkedAsPublishArea( String path ) throws Exception {
        String query = "SELECT [jcr:mixinTypes] FROM [nt:folder] WHERE [jcr:path] = '" + path + "'";
        List<QueryRow> results = this.restClient.query(workspace1, IJcrConstants.JCR_SQL2, query);
        assertThat(results.size(), is(1));

        QueryRow row = results.get(0);
        assertNull(row.getValue(IJcrConstants.MIXIN_TYPES_PROPERTY));
    }

}
