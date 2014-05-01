package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoMkcolTest extends AbstractWebDAVTest {

    private static final String PARENT_PATH = "/parentCollection";
    private static final String MKCOL_PATH = PARENT_PATH + "/makeCollection";

    @Test
    public void testMkcolIfReadOnlyIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, MKCOL_PATH);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                String methodsAllowed = "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND";

                oneOf(mockRes).addHeader("Allow", methodsAllowed);
                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                oneOf(mockRes).addHeader("Allow",
                                       "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND, PUT");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, MKCOL_PATH);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                                            with(any(String.class)),
                                            with(any(String.class)),
                                            with(any(boolean.class)),
                                            with(any(int.class)),
                                            with(any(int.class)),
                                            with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, PARENT_PATH);

                parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, MKCOL_PATH);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction, MKCOL_PATH, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, MKCOL_PATH);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                String loId = null;
                if (lockNullResourceLo != null) {
                    loId = lockNullResourceLo.getID();
                }
                final String lockToken = "<opaquelocktoken:" + loId + ">";

                oneOf(mockRes).addHeader("Lock-Token", lockToken);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                                                                    with(any(String.class)),
                                                                    with(any(String.class)));

                // -----LOCK on a non-existing resource successful------
                // --------now MKCOL on the lock-null resource----------

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(MKCOL_PATH));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                                            with(any(String.class)),
                                            with(any(String.class)),
                                            with(any(boolean.class)),
                                            with(any(int.class)),
                                            with(any(int.class)),
                                            with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceSo));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, MKCOL_PATH);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                @SuppressWarnings( "null" )
                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                                                                    with(any(String.class)),
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
