package org.modeshape.web.jcr.webdav;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.util.IoUtil;
import org.modeshape.web.jcr.ModeShapeJcrDeployer;
import org.modeshape.web.jcr.RepositoryFactory;
import org.modeshape.web.jcr.spi.FactoryRepositoryProvider;
import org.modeshape.web.jcr.webdav.ModeShapeWebdavStore.JcrSessionTransaction;

public class ModeShapeWebdavStoreTest {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private IWebdavStore store;
    private JcrSessionTransaction tx;
    private ServletContextListener contextListener = new ModeShapeJcrDeployer();
    private Session session;
    private String repositoryName = "mode:repository";
    private String workspaceName = "default";

    @Mock
    private Principal principal;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ServletContext context;
    @Mock
    private ServletContextEvent event;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(principal.getName()).thenReturn("testuser");
        when(request.getUserPrincipal()).thenReturn(principal);
        when(request.isUserInRole("readwrite")).thenReturn(true);
        when(context.getInitParameter(RepositoryFactory.PROVIDER_KEY)).thenReturn(FactoryRepositoryProvider.class.getName());
        when(context.getInitParameter(FactoryRepositoryProvider.JCR_URL)).thenReturn("file:///configRepository.xml");
        when(context.getInitParameter(SingleRepositoryRequestResolver.INIT_REPOSITORY_NAME)).thenReturn("mode:repository");
        when(context.getInitParameter(SingleRepositoryRequestResolver.INIT_WORKSPACE_NAME)).thenReturn("default");
        when(event.getServletContext()).thenReturn(context);

        RequestResolver uriResolver = new SingleRepositoryRequestResolver();
        uriResolver.initialize(context);
        store = new ModeShapeWebdavStore(uriResolver);

        contextListener.contextInitialized(event);

        ModeShapeWebdavStore.setRequest(request);

        session = RepositoryFactory.getSession(request, repositoryName, workspaceName);

        tx = (JcrSessionTransaction)store.begin(principal);

        assertThat(tx.owns(session), is(false));
    }

    @After
    public void afterEach() throws Exception {
        session.logout();
        ModeShapeWebdavStore.setRequest(null);
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
        List<String> sessChildren = new LinkedList<String>();
        Node node = (Node)session.getItem(path);
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
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

        store.createFolder(tx, "/newFolder");

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

        store.createResource(tx, "/newFile");

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

        Node fileNode = session.getRootNode().addNode("newFile", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "mode:resource");
        contentNode.setProperty("jcr:data", TEST_STRING);
        contentNode.setProperty("jcr:mimeType", "text/plain");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.save();

        when(request.getPathInfo()).thenReturn("/newFile");
        assertThat((int)store.getResourceLength(tx, "/newFile"), is(TEST_STRING.length()));

        InputStream is = store.getResourceContent(tx, "/newFile");
        String webdavContent = IoUtil.read(is);
        assertThat(webdavContent, is(TEST_STRING));
    }

    @Test
    public void shouldRemoveFile() throws Exception {
        final String TEST_STRING = "This is my miraculous test string!";

        Node fileNode = session.getRootNode().addNode("newFile", "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "mode:resource");
        contentNode.setProperty("jcr:data", TEST_STRING);
        contentNode.setProperty("jcr:mimeType", "text/plain");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.save();

        when(request.getPathInfo()).thenReturn("/newFile");
        store.removeObject(tx, "/newFile");
        store.commit(tx);

        session.refresh(false);
        assertThat(session.getRootNode().hasNode("newFile"), is(false));
    }

    @Test
    public void shouldSimulateAddingFileThroughWebdav() throws Exception {
        final String TEST_STRING = "This is my miraculous test string!";
        final String TEST_URI = "/TestFile.rtf";

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

}
