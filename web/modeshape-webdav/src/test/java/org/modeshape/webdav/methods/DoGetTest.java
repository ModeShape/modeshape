package org.modeshape.webdav.methods;

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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockReq).getRequestURI();
                will(returnValue("/index.html"));

                one(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, "/index.html");
                one(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockRes).setDateHeader("last-modified", indexSo.getLastModified().getTime());

                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

                one(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                one(mockRes).setContentType("text/foo");

                StoredObject so = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(so));

                one(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));

                one(mockStore).getResourceContent(mockTransaction, "/index.html");
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();
                StoredObject aaa = initFolderStoredObject();
                StoredObject bbb = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getLocale();
                will(returnValue(Locale.GERMAN));

                one(mockRes).setContentType("text/html");
                one(mockRes).setCharacterEncoding("UTF8");

                one(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));

                one(mockStore).getChildrenNames(mockTransaction, "/foo/");
                will(returnValue(new String[] {"AAA", "BBB"}));

                one(mockStore).getStoredObject(mockTransaction, "/foo//AAA");
                will(returnValue(aaa));

                one(mockStore).getStoredObject(mockTransaction, "/foo//BBB");
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getRequestURI();
                will(returnValue("/foo/"));

                one(mockRes).encodeRedirectURL("/foo//indexFile");

                one(mockRes).sendRedirect("");
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                StoredObject alternativeSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockRes).setDateHeader("last-modified", alternativeSo.getLastModified().getTime());

                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

                one(mockMimeTyper).getMimeType(mockTransaction, "/alternative");
                will(returnValue("text/foo"));

                one(mockRes).setContentType("text/foo");

                one(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                one(mockRes).getOutputStream();
                will(returnValue(testingOutputStream));
                one(mockStore).getResourceContent(mockTransaction, "/alternative");
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                one(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, "/alternative", new ResourceLocks(), mockMimeTyper, 0);
        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", testingOutputStream.toString());

        mockery.assertIsSatisfied();
    }
}
