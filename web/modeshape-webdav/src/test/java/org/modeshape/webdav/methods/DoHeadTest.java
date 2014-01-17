package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoHeadTest extends AbstractWebDAVTest {

    @Test
    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaPageResultsInPage() throws Exception {

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
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

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

        DoHead doHead = new DoHead(mockStore, "/indexFile", null, new ResourceLocks(), mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
