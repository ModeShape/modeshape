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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("Destination");
                will(returnValue("/destination"));

                one(mockReq).getServerName();
                will(returnValue("myServer"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue("/destination"));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject so = initLockNullStoredObject();

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                one(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");
                one(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("myServer"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                one(mockRes).setStatus(WebdavStatus.SC_LOCKED);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("myServer"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                one(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                destFileSo = initFileStoredObject(RESOURCE_CONTENT);
                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(null));

                one(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(null));

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).createFolder(mockTransaction, DEST_COLLECTION_PATH);

                one(mockReq).getHeader("Depth");
                will(returnValue("-1"));

                one(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);
                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream stream = resourceRequestStream();
                will(returnValue(stream));
                one(mockStore).setResourceContent(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile", stream, null, null);

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);
                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject notExistSo = null;

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(notExistSo));

                one(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue("/folder/destFolder"));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject existingDestSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                one(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockReq).getHeader("Destination");
                will(returnValue("serverName".concat(DEST_FILE_PATH)));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject existingDestSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(existingDestSo));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                one(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(DEST_FILE_PATH)));

                one(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                StoredObject sourceSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceSo));

                one(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(DEST_COLLECTION_PATH)));

                one(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject existingDestSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(existingDestSo));

                one(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
