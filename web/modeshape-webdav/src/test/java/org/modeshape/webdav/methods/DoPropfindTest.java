package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoPropfindTest extends AbstractWebDAVTest {

    @Test
    public void doPropFindOnDirectory() throws Exception {
        final String path = "/";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("Depth");
                will(returnValue("infinity"));

                StoredObject rootSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                one(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                one(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getServletPath();
                will(returnValue(path));

                one(mockStore).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[] {"file1", "file2"}));

                StoredObject file1So = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, path + "file1");
                will(returnValue(file1So));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getServletPath();
                will(returnValue(path));

                one(mockStore).getChildrenNames(mockTransaction, path + "file1");
                will(returnValue(new String[] {}));

                StoredObject file2So = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, path + "file2");
                will(returnValue(file2So));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getServletPath();
                will(returnValue(path));

                one(mockStore).getChildrenNames(mockTransaction, path + "file2");
                will(returnValue(new String[] {}));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(), mockMimeTyper);
        doPropfind.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnFile() throws Exception {
        final String path = "/testFile";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject fileSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                one(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                one(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getServletPath();
                will(returnValue("/"));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(), mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnNonExistingResource() throws Exception {
        final String path = "/notExists";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject notExistingSo = null;

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockReq).getRequestURI();
                will(returnValue(path));

                one(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, path);
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(), mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
