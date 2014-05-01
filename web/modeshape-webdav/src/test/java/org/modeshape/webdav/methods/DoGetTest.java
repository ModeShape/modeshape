package org.modeshape.webdav.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Locale;
import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;

@SuppressWarnings( "synthetic-access" )
public class DoGetTest extends AbstractWebDAVTest {

    @Test
    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/index.html"));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, "/index.html");
                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaPageResultsInPage() throws Exception {
        final TestingOutputStream testingOutputStream = new TestingOutputStream();

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified", indexSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");

                StoredObject so = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(so));

                oneOf(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));

                oneOf(mockStore).getResourceContent(mockTransaction, "/index.html");
                will(returnValue(resourceRequestStream()));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);
        assertEquals("<hello/>", testingOutputStream.toString());

        mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRudimentaryChildList() throws Exception {
        final TestingOutputStream testingOutputStream = new TestingOutputStream();
        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();
                StoredObject aaa = initFolderStoredObject();
                StoredObject bbb = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.GERMAN));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF-8");

                oneOf(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));

                oneOf(mockStore).getChildrenNames(mockTransaction, "/foo/");
                will(returnValue(new String[] {"AAA", "BBB"}));

                exactly(2).of(mockReq).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost")));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo//AAA");
                will(returnValue(aaa));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo//BBB");
                will(returnValue(bbb));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);
        assertTrue(testingOutputStream.toString().length() > 0);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/foo/"));

                oneOf(mockRes).encodeRedirectURL("/foo//indexFile");

                oneOf(mockRes).sendRedirect("");
            }
        });

        DoGet doGet = new DoGet(mockStore, "/indexFile", null, new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404() throws Exception {
        final TestingOutputStream testingOutputStream = new TestingOutputStream();

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                StoredObject alternativeSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified", alternativeSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/alternative");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");

                oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                oneOf(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));
                oneOf(mockStore).getResourceContent(mockTransaction, "/alternative");
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, "/alternative", new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", testingOutputStream.toString());

        mockery.assertIsSatisfied();
    }
}
