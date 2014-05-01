package org.modeshape.webdav.methods;

import java.util.Collections;
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("infinity"));

                StoredObject rootSo = initFolderStoredObject();

                oneOf(mockStore).getCustomNamespaces(mockTransaction, path);
                will(returnValue(Collections.emptyMap()));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getCustomProperties(mockTransaction, path);
                will(returnValue(Collections.emptyMap()));

                oneOf(mockStore).getCustomProperties(mockTransaction, path + "file1");
                will(returnValue(Collections.emptyMap()));

                oneOf(mockStore).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[] {"file1", "file2"}));

                StoredObject file1So = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file1");
                will(returnValue(file1So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getChildrenNames(mockTransaction, path + "file1");
                will(returnValue(new String[] {}));

                StoredObject file2So = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file2");
                will(returnValue(file2So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getChildrenNames(mockTransaction, path + "file2");
                will(returnValue(new String[] {}));

                oneOf(mockStore).getCustomProperties(mockTransaction, path + "file2");
                will(returnValue(Collections.emptyMap()));
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject fileSo = initFolderStoredObject();

                oneOf(mockStore).getCustomNamespaces(mockTransaction, path);
                will(returnValue(Collections.emptyMap()));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue("/"));

                oneOf(mockStore).getCustomProperties(mockTransaction, path);
                will(returnValue(Collections.emptyMap()));
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockReq).getRequestURI();
                will(returnValue(path));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, path);
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(), mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
