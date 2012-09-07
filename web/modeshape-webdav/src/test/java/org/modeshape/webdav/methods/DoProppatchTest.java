package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;

public class DoProppatchTest extends AbstractWebDAVTest {

    @Test
    public void doProppatchIfReadOnly() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject notExistingSo = null;

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                one(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                one(mockReq).getHeader("If");
                will(returnValue(""));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getContentLength();
                will(returnValue(0));

                one(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                one(mockReq).getHeader("If");
                will(returnValue(""));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getContentLength();
                will(returnValue(RESOURCE_CONTENT.length));

                one(mockReq).getInputStream();
                will(returnValue(resourceRequestStream()));

                one(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                one(mockReq).getContextPath();
                will(returnValue(""));
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !READ_ONLY);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
