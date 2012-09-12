package org.modeshape.webdav;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

@SuppressWarnings( "synthetic-access" )
public class WebdavServletTest extends AbstractWebDAVTest {

    private ServletConfig servletConfig;
    private ServletContext servletContext;

    private MockServletConfig mockServletConfig;
    private MockServletContext mockServletContext;
    private MockHttpSession mockHttpSession;
    private MockPrincipal mockPrincipal;

    private String dftIndexFile = "/index.html";
    private String insteadOf404 = "/insteadOf404";

    @Before
    public void setUp() throws Exception {
        setupMocks();

        servletConfig = mockery.mock(ServletConfig.class);
        servletContext = mockery.mock(ServletContext.class);

        mockServletConfig = new MockServletConfig(mockServletContext);
        mockHttpSession = new MockHttpSession(mockServletContext);
        mockServletContext = new MockServletContext();

        mockPrincipal = new MockPrincipal("Admin", new String[] {"Admin", "Manager"});
    }

    @Test
    public void testInit() throws Exception {

        mockery.checking(new Expectations() {});

        WebDavServletBean servlet = new WebdavServlet();
        servlet.init(mockStore, dftIndexFile, insteadOf404, 1, true);

        mockery.assertIsSatisfied();
    }

    // Test successes in eclipse, but fails in "mvn test"
    // first three expectations aren't successful with "mvn test"
    @Test
    public void testInitGenericServlet() throws Exception {

        mockery.checking(new Expectations() {
            {
                allowing(servletConfig).getServletContext();
                will(returnValue(mockServletContext));

                allowing(servletConfig).getServletName();
                will(returnValue("webdav-servlet"));

                allowing(servletContext).log("webdav-servlet: init");

                one(servletConfig).getInitParameter("ResourceHandlerImplementation");
                will(returnValue(""));

                one(servletConfig).getInitParameter("rootpath");
                will(returnValue("./target/tmpTestData/"));

                exactly(2).of(servletConfig).getInitParameter("lazyFolderCreationOnPut");
                will(returnValue("1"));

                one(servletConfig).getInitParameter("default-index-file");
                will(returnValue("index.html"));

                one(servletConfig).getInitParameter("instead-of-404");
                will(returnValue(""));

                exactly(2).of(servletConfig).getInitParameter("no-content-length-headers");
                will(returnValue("0"));
            }
        });

        WebDavServletBean servlet = new WebdavServlet();

        servlet.init(servletConfig);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testService() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(mockServletContext);
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        mockServletConfig.addInitParameter("ResourceHandlerImplementation", "");
        mockServletConfig.addInitParameter("rootpath", "./target/tmpTestData");
        mockServletConfig.addInitParameter("lazyFolderCreationOnPut", "1");
        mockServletConfig.addInitParameter("default-index-file", dftIndexFile);
        mockServletConfig.addInitParameter("instead-of-404", insteadOf404);
        mockServletConfig.addInitParameter("no-content-length-headers", "0");

        // StringTokenizer headers = new StringTokenizer(
        // "Host Depth Content-Type Content-Length");
        mockRequest.setMethod("PUT");
        mockRequest.setAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE, null);
        mockRequest.setPathInfo("/aPath/toAFile");
        mockRequest.setRequestURI("/aPath/toAFile");
        mockRequest.addHeader("Host", "www.foo.bar");
        mockRequest.addHeader("Depth", "0");
        mockRequest.addHeader("Content-Type", "text/xml");
        mockRequest.addHeader("Content-Length", "1234");
        mockRequest.addHeader("User-Agent", "...some Client with WebDAVFS...");

        mockRequest.setSession(mockHttpSession);
        mockPrincipal = new MockPrincipal("Admin", new String[] {"Admin", "Manager"});
        mockRequest.setUserPrincipal(mockPrincipal);
        mockRequest.addUserRole("Admin");
        mockRequest.addUserRole("Manager");

        mockRequest.setContent(RESOURCE_CONTENT);

        mockery.checking(new Expectations() {
            {
            }
        });

        WebDavServletBean servlet = new WebdavServlet();
        servlet.init(mockServletConfig);
        servlet.service(mockRequest, mockResponse);
        mockery.assertIsSatisfied();
    }
}
