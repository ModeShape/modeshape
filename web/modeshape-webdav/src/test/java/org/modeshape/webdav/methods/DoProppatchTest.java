package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoProppatchTest extends AbstractWebDAVTest {

    @Test
    public void doProppatchIfReadOnly() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), READ_ONLY);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnNonExistingResource() throws Exception {

        final String path = "/notExists";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !READ_ONLY);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnRequestWithNoContent() throws Exception {

        final String path = "/testFile";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(""));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !READ_ONLY);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnResource() throws Exception {

        final String path = "/testFile";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(""));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(RESOURCE_CONTENT.length));

                oneOf(mockReq).getInputStream();
                will(returnValue(resourceRequestStream()));

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !READ_ONLY);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
