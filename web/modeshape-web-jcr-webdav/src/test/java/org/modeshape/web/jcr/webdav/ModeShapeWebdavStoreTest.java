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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.RepositoryFactory;
import org.modeshape.web.jcr.ModeShapeJcrDeployer;
import org.modeshape.web.jcr.RepositoryManager;
import org.modeshape.web.jcr.webdav.ModeShapeWebdavStore.JcrSessionTransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class ModeShapeWebdavStoreTest {

    private static final String TEST_ROOT = "testRoot";
    private static final String TEST_ROOT_PATH = "/" + TEST_ROOT;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String REPOSITORY_NAME = "repo";
    private static final String WORKSPACE_NAME = "default";

    private static ServletContextListener contextListener = new ModeShapeJcrDeployer();
    @Mock
    private static ServletContextEvent event;

    private IWebdavStore store;
    private JcrSessionTransaction tx;
    private Session session;

    @Mock
    private Principal principal;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ServletContext context;

    private Node testRoot;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(principal.getName()).thenReturn("testuser");
        when(request.getUserPrincipal()).thenReturn(principal);
        when(request.isUserInRole("readwrite")).thenReturn(true);
        when(context.getInitParameter(RepositoryFactory.URL)).thenReturn("file:///repository-config.json");
        when(context.getInitParameter(SingleRepositoryRequestResolver.INIT_REPOSITORY_NAME)).thenReturn(REPOSITORY_NAME);
        when(context.getInitParameter(SingleRepositoryRequestResolver.INIT_WORKSPACE_NAME)).thenReturn(WORKSPACE_NAME);
        when(event.getServletContext()).thenReturn(context);

        RequestResolver uriResolver = new SingleRepositoryRequestResolver();
        uriResolver.initialize(context);

        ContentMapper contentMapper = new DefaultContentMapper();
        contentMapper.initialize(context);
        store = new ModeShapeWebdavStore(uriResolver, contentMapper);

        contextListener.contextInitialized(event);

        ModeShapeWebdavStore.setRequest(request);

        session = RepositoryManager.getSession(request, REPOSITORY_NAME, WORKSPACE_NAME);
        testRoot = session.getRootNode().addNode(TEST_ROOT);
        session.save();

        tx = (JcrSessionTransaction)store.begin(principal);

        assertThat(tx.owns(session), is(false));
    }

    @After
    public void afterEach() throws Exception {
        ModeShapeWebdavStore.setRequest(null);

        testRoot.remove();
        session.save();
        session.logout();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        contextListener.contextDestroyed(event);
    }

    @Test
    public void shouldBeginTransaction() throws Exception {
        assertThat(tx, is(notNullValue()));
    }

    @Test
    public void shouldCommitTransaction() throws Exception {
        store.commit(tx);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCommitNullTransaction() throws Exception {
        store.commit(null);
    }

    private void compareChildrenWithSession( String[] children,
                                             String path ) throws Exception {
        List<String> sessChildren = new LinkedList<>();
        Node node = (Node)session.getItem(path);
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            sessChildren.add(iter.nextNode().getName());
        }

        assertThat(children, is(sessChildren.toArray(EMPTY_STRING_ARRAY)));

    }

    @Test
    public void shouldListFilesAtRoot() throws Exception {
        String[] children = store.getChildrenNames(tx, "/");
        compareChildrenWithSession(children, "/");
    }

    @Test
    public void shouldAddFolderAtRoot() throws Exception {
        String[] children = store.getChildrenNames(tx, "/");

        store.createFolder(tx, TEST_ROOT_PATH + "/newFolder");

        // Make sure that the node isn't visible yet
        compareChildrenWithSession(children, "/");

        store.commit(tx);

        session.refresh(false);

        // Make sure that the node is visible after the commit
        compareChildrenWithSession(store.getChildrenNames(tx, "/"), "/");
    }

    @Test
    public void shouldAddFileAtRoot() throws Exception {
        String[] children = store.getChildrenNames(tx, "/");

        store.createResource(tx, TEST_ROOT_PATH + "/newFile");

        // Make sure that the node isn't visible yet
        compareChildrenWithSession(children, "/");

        store.commit(tx);

        session.refresh(false);

        // Make sure that the node is visible after the commit
        compareChildrenWithSession(store.getChildrenNames(tx, "/"), "/");
    }

    @Test
    public void shouldReadFileLengthAndContent() throws Exception {
        final String TEST_STRING = "This is my miraculous test string!";

        Node fileNode = testRoot.addNode("newFile", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "mode:resource");
        contentNode.setProperty("jcr:data", TEST_STRING);
        contentNode.setProperty("jcr:mimeType", "text/plain");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.save();

        when(request.getPathInfo()).thenReturn(TEST_ROOT_PATH + "/newFile");
        assertThat((int)store.getResourceLength(tx, TEST_ROOT_PATH + "/newFile"), is(TEST_STRING.length()));

        InputStream is = store.getResourceContent(tx, TEST_ROOT_PATH + "/newFile");
        String webdavContent = IoUtil.read(is);
        assertThat(webdavContent, is(TEST_STRING));
    }

    @Test
    public void shouldRemoveFile() throws Exception {
        final String TEST_STRING = "This is my miraculous test string!";

        Node fileNode = testRoot.addNode("newFile", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "mode:resource");
        contentNode.setProperty("jcr:data", TEST_STRING);
        contentNode.setProperty("jcr:mimeType", "text/plain");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.save();

        when(request.getPathInfo()).thenReturn(TEST_ROOT_PATH + "/newFile");
        store.removeObject(tx, TEST_ROOT_PATH + "/newFile");
        store.commit(tx);

        session.refresh(false);
        assertThat(testRoot.hasNode("newFile"), is(false));
    }

    @Test
    public void shouldSimulateAddingFileThroughWebdav() throws Exception {
        final String TEST_STRING = "This is my miraculous test string!";
        final String TEST_URI = TEST_ROOT_PATH + "/TestFile.rtf";

        when(request.getPathInfo()).thenReturn("");
        store.getStoredObject(tx, "");

        when(request.getPathInfo()).thenReturn(TEST_URI);
        store.getStoredObject(tx, TEST_URI);

        when(request.getPathInfo()).thenReturn("/"); // will ask for parent during resolution ...
        store.createResource(tx, TEST_URI);

        when(request.getPathInfo()).thenReturn(TEST_URI);
        long length = store.setResourceContent(tx,
                                               TEST_URI,
                                               new ByteArrayInputStream(TEST_STRING.getBytes()),
                                               "text/plain",
                                               "UTF-8");

        assertThat((int)length, is(TEST_STRING.getBytes().length));

        StoredObject ob = store.getStoredObject(tx, TEST_URI);

        assertThat(ob, is(notNullValue()));
    }

    @Test
    @FixFor( "MODE-2589" )
    public void shouldSimulateAddingAndCopyingZeroLengthFileThroughWebdav() throws Exception {
        final String testString = "";
        final String sourceUri = TEST_ROOT_PATH + "/TestFile.rtf";
        final String destinationUri = TEST_ROOT_PATH + "/CopyOfTestFile.rtf";

        when(request.getPathInfo()).thenReturn("");
        store.getStoredObject(tx, "");

        when(request.getPathInfo()).thenReturn(sourceUri);
        store.getStoredObject(tx, sourceUri);

        when(request.getPathInfo()).thenReturn("/"); // will ask for parent during resolution ...
        store.createResource(tx, sourceUri);

        when(request.getPathInfo()).thenReturn(sourceUri);
        long length = store.setResourceContent(tx, sourceUri, new ByteArrayInputStream(testString.getBytes()), "text/plain",
                                               "UTF-8");

        assertThat((int)length, is(testString.getBytes().length));

        StoredObject ob = store.getStoredObject(tx, sourceUri);
        assertThat(ob, is(notNullValue()));

        when(request.getPathInfo()).thenReturn("/"); // will ask for parent during resolution ...
        store.createResource(tx, destinationUri);

        when(request.getPathInfo()).thenReturn(destinationUri);
        length = store.setResourceContent(tx, destinationUri, store.getResourceContent(tx, sourceUri), "text/plain", "UTF-8");

        assertThat((int)length, is(testString.getBytes().length));
        ob = store.getStoredObject(tx, destinationUri);
        assertThat(ob, is(notNullValue()));
    }
}
