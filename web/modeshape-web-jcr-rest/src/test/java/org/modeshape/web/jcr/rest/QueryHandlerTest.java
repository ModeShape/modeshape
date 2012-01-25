package org.modeshape.web.jcr.rest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.URLEncoder;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.web.jcr.ModeShapeJcrDeployer;
import org.modeshape.web.jcr.RepositoryFactory;
import org.modeshape.web.jcr.spi.FactoryRepositoryProvider;

@Ignore
// TODO: Query
public class QueryHandlerTest {

    private final String VALID_JCR_URL = "file:src/test/resources/repo-config.json";
    private final String REPOSITORY_NAME = "Test Repository";
    private final String WORKSPACE_NAME = "default";
    private final String NODE_NAME = "testNode";

    protected QueryHandler handler;
    protected ModeShapeJcrDeployer deployer;
    @Mock
    private ServletContext context;
    @Mock
    private ServletContextEvent event;
    @Mock
    private HttpServletRequest request;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(context.getInitParameter(FactoryRepositoryProvider.JCR_URL)).thenReturn(VALID_JCR_URL);
        when(context.getInitParameter(RepositoryFactory.PROVIDER_KEY)).thenReturn(FactoryRepositoryProvider.class.getName());
        when(event.getServletContext()).thenReturn(context);

        deployer = new ModeShapeJcrDeployer();
        deployer.contextInitialized(event);

        handler = new QueryHandler();

        Session session = getSession();

        Node root = session.getRootNode();

        for (int i = 0; i < 10; i++) {
            Node newNode = root.addNode(NODE_NAME);
            newNode.setProperty("foo", i);
        }
        session.save();

    }

    @After
    public void afterEach() throws Exception {
        deployer.contextDestroyed(event);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldReturnAllResultsWhenNoOffsetOrLimitProvided() throws Exception {

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, -1, -1, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(10));

        for (int i = 0; i < 10; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            if (i == 0) {
                assertThat(nodePath, is("/" + NODE_NAME));
            } else {
                assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + 1) + "]"));
            }
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldLimitResultsWhenLimitIsProvided() throws Exception {
        final int LIMIT = 3;

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, -1, LIMIT, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");
        assertThat(results.length(), is(LIMIT));

        for (int i = 0; i < LIMIT; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            if (i == 0) {
                assertThat(nodePath, is("/" + NODE_NAME));
            } else {
                assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + 1) + "]"));
            }
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldLimitResultsFromProvidedOffset() throws Exception {
        final int OFFSET = 5;
        final int LIMIT = -1;

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, OFFSET, LIMIT, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(5));

        for (int i = 0; i < 5; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + OFFSET + 1) + "]"));
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldFilterResultsFromProvidedOffsetAndLimit() throws Exception {
        final int OFFSET = 5;
        final int LIMIT = 3;

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, OFFSET, LIMIT, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(LIMIT));

        for (int i = 0; i < LIMIT; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + OFFSET + 1) + "]"));
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldReturnNoResultsForHighOffset() throws Exception {
        final int OFFSET = 10;
        final int LIMIT = -1;

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, OFFSET, LIMIT, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(0));
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldReturnAllResultsForHighLimit() throws Exception {

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, -1, 100, null);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(10));

        for (int i = 0; i < 10; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            if (i == 0) {
                assertThat(nodePath, is("/" + NODE_NAME));
            } else {
                assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + 1) + "]"));
            }
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBindQueryParametersAsVariables() throws Exception {
        URI absoluteUri = uri("query?var1=value1&var2=value2&var3=value3&limit=100");
        UriInfo uriInfo = new UriInfoImpl(absoluteUri, absoluteUri, absoluteUri.getRawPath(), absoluteUri.getRawQuery(),
                                          PathSegmentImpl.parseSegments(absoluteUri.getRawPath()));

        String statement = "//element(" + NODE_NAME + ") order by @foo";
        String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.XPATH, statement, -1, 100, uriInfo);

        JSONObject queryResult = new JSONObject(response);
        assertThat(queryResult.get("rows"), is(notNullValue()));

        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(10));

        for (int i = 0; i < 10; i++) {
            JSONObject object = (JSONObject)results.get(i);
            String nodePath = (String)object.get("jcr:path");

            if (i == 0) {
                assertThat(nodePath, is("/" + NODE_NAME));
            } else {
                assertThat(nodePath, is("/" + NODE_NAME + "[" + (i + 1) + "]"));
            }
        }
    }

    @Test
    public void shouldQueryAndNotFailWhenNullValueAppearsInResultSet() throws Exception {
        Session session = getSession();
        String nodeName = "publishAreaX";
        String nodePath = "/" + nodeName;
        try {
            Node publishAreaX = session.getRootNode().addNode(nodeName, "nt:unstructured");
            publishAreaX.addMixin("mode:publishArea");
            session.save();

            String statement = "SELECT [jcr:primaryType], [jcr:path], [jcr:title] FROM [mode:publishArea]";
            String response = handler.postItem(request, REPOSITORY_NAME, WORKSPACE_NAME, Query.JCR_SQL2, statement, -1, 100, null);

            JSONObject queryResult = new JSONObject(response);
            assertThat(queryResult.get("rows"), is(notNullValue()));

            JSONArray results = (JSONArray)queryResult.get("rows");

            assertThat(results.length(), is(1));

            JSONObject object = (JSONObject)results.get(0);
            assertThat((String)object.get("jcr:path"), is(nodePath));
            assertThat(object.has("jcr:title"), is(false));
        } finally {
            try {
                session.getNode("/publishAreaX").remove();
            } catch (RepositoryException e) {
                // it's okay ...
            }
        }
    }

    private URI uri( String relativePathExcludingRepositoryAndWorkspace ) throws Exception {
        String uri = "http://www.example.com/resources/" + URLEncoder.encode(REPOSITORY_NAME, "UTF-8") + "/"
                     + URLEncoder.encode(WORKSPACE_NAME, "UTF-8") + "/" + relativePathExcludingRepositoryAndWorkspace;
        return new URI(uri);
    }

    private Session getSession() throws RepositoryException {
        return RepositoryFactory.getSession(request, REPOSITORY_NAME, WORKSPACE_NAME);
    }

}
