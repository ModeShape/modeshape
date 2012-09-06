package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;

public class DoMkcolTest extends AbstractWebDAVTest {

    private static final String PARENT_PATH = "/parentCollection";
    private static final String MKCOL_PATH = PARENT_PATH + "/makeCollection";

    @Test
    public void testMkcolIfReadOnlyIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolSuccess() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                one(mockStore).createFolder(mockTransaction, MKCOL_PATH);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsNoFolder() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                String methodsAllowed = "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND";

                one(mockRes).addHeader("Allow", methodsAllowed);
                one(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsAFolderButObjectAlreadyExists() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                one(mockRes).addHeader("Allow",
                                       "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND, PUT");

                one(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithRightLockToken() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, PARENT_PATH, OWNER, true, -1, 200, false);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, PARENT_PATH);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                one(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                StoredObject parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                one(mockStore).createFolder(mockTransaction, MKCOL_PATH);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithWrongLockToken() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, PARENT_PATH, OWNER, true, -1, 200, false);
        final String wrongLockToken = "(<opaquelocktoken:" + "aWrongLockToken" + ">)";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                one(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolOnALockNullResource() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)), with(any(String.class)),
                                            with(any(boolean.class)), with(any(int.class)), with(any(int.class)), with(any(
                        boolean.class)));
                will(returnValue(true));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, PARENT_PATH);

                parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, MKCOL_PATH);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, MKCOL_PATH, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, MKCOL_PATH);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                String loId = null;
                if (lockNullResourceLo != null) {
                    loId = lockNullResourceLo.getID();
                }
                final String lockToken = "<opaquelocktoken:" + loId + ">";

                one(mockRes).addHeader("Lock-Token", lockToken);

                one(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)), with(any(String.class)),
                                                                    with(any(String.class)));

                // -----LOCK on a non-existing resource successful------
                // --------now MKCOL on the lock-null resource----------

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                one(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)), with(any(String.class)),
                                            with(any(boolean.class)), with(any(int.class)), with(any(int.class)), with(any(
                        boolean.class)));
                will(returnValue(true));

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                one(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                one(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)), with(any(String.class)),
                                                                    with(any(String.class)));

            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoMkcol doMkcol = new DoMkcol(mockStore, mockResourceLocks, !READ_ONLY);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
