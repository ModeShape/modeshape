package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;

@SuppressWarnings( "synthetic-access" )
public class DoCopyTest extends AbstractWebDAVTest {

    @Test
    public void testDoCopyIfReadOnly() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyOfLockNullResource() throws Exception {

        final String parentPath = "/lockedFolder";
        final String path = parentPath.concat("/nullFile");

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, OWNER, true, 1, TEMP_TIMEOUT, !TEMPORARY);

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("/destination"));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/destination"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject so = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");
                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfParentIsLockedWithWrongLockToken() throws Exception {
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, DEST_COLLECTION_PATH, OWNER, true, 1, TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, DEST_COLLECTION_PATH);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfParentIsLockedWithRightLockToken() throws Exception {
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, DEST_COLLECTION_PATH, OWNER, true, 1, TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, DEST_COLLECTION_PATH);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                oneOf(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                destFileSo = initFileStoredObject(RESOURCE_CONTENT);
                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationPathInvalid() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfSourceEqualsDestination() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyFolderIfNoLocks() throws Exception {
        final String[] sourceChildren = new String[] {"sourceFile"};
        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(null));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).createFolder(mockTransaction, DEST_COLLECTION_PATH);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("-1"));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);
                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream stream = resourceRequestStream();
                will(returnValue(stream));
                oneOf(mockStore).setResourceContent(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile", stream, null, null);

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);
                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");
                will(returnValue(destFileSo));
            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfSourceDoesntExist() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject notExistSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(notExistSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/destFolder"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject existingDestSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                oneOf(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("serverName".concat(DEST_FILE_PATH)));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject existingDestSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfOverwriteTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(DEST_FILE_PATH)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfOverwriteFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(DEST_COLLECTION_PATH)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject existingDestSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(existingDestSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
