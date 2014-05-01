package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoDeleteTest extends AbstractWebDAVTest {

    @Test
    public void testDeleteIfReadOnlyIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, READ_ONLY);
        doDelete.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectNotExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(fileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject folderSo = initFolderStoredObject();
                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(folderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] {"subFolder", "sourceFile"}));

                StoredObject fileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);

                StoredObject subFolderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH + "/subFolder");
                will(returnValue(subFolderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH + "/subFolder");
                will(returnValue(new String[] {"fileInSubFolder"}));

                StoredObject fileInSubFolderSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH + "/subFolder/fileInSubFolder");
                will(returnValue(fileInSubFolderSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH + "/subFolder/fileInSubFolder");
                oneOf(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH + "/subFolder");
                oneOf(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectNotExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolder() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithWrongLockToken() throws Exception {

        final String lockedFolderPath = "/lockedFolder";
        final String fileInLockedFolderPath = lockedFolderPath + "/fileInLockedFolder";

        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, lockedFolderPath, OWNER, true, -1, TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockedFolderPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(fileInLockedFolderPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithRightLockToken() throws Exception {
        final String path = "/lockedFolder/fileInLockedFolder";
        final String parentPath = "/lockedFolder";
        final String owner = "owner";
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, parentPath, owner, true, -1, TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, "/lockedFolder");
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject so = initFileStoredObject(RESOURCE_CONTENT);
                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockStore).removeObject(mockTransaction, path);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolderIfObjectNotExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/file"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject nonExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/folder/file");
                will(returnValue(nonExistingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), false);
        doDelete.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }
}
