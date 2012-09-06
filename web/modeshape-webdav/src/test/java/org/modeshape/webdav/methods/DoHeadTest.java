package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;

public class DoHeadTest extends AbstractWebDAVTest {

    @Test
    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
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

        DoHead doHead = new DoHead(mockStore, "/indexFile", null, new ResourceLocks(), mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
