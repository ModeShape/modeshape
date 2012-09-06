package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;

public class DoUnlockTest extends AbstractWebDAVTest {

    private static final boolean EXCLUSIVE = true;

    @Test
    public void testDoUnlockIfReadOnly() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, new ResourceLocks(), READ_ONLY);
        doUnlock.execute(mockTransaction, mockReq, mockRes);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithRightLockToken() throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, EXCLUSIVE, 0, TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat(">");

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                StoredObject lockedSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockedSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !READ_ONLY);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithWrongLockToken() throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, EXCLUSIVE, 0, TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat("WRONG>");

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                one(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !READ_ONLY);
        doUnlock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaNotLockedResource() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        final String lockPath = "/notLockedResource";
        final String lockToken = "<opaquelocktoken:xxxx-xxxx-xxxxWRONG>";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                one(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !READ_ONLY);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockNullResource() throws Exception {

        final String parentPath = "/parentCollection";
        final String nullLoPath = parentPath.concat("/aNullResource");

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(nullLoPath));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, parentPath);
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

                one(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, parentPath);

                one(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, nullLoPath);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, nullLoPath, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, nullLoPath);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, nullLoPath);
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
                // ----------------now try to unlock it-----------------

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(nullLoPath));

                one(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)), with(any(String.class)),
                                            with(any(boolean.class)), with(any(int.class)), with(any(int.class)), with(any(
                        boolean.class)));
                will(returnValue(true));

                one(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                one(mockResourceLocks).getLockedObjectByID(mockTransaction, loId);
                will(returnValue(lockNullResourceLo));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                one(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                one(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                one(mockStore).removeObject(mockTransaction, nullLoPath);

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)), with(any(String.class)),
                                                                    with(any(String.class)));

            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoUnlock doUnlock = new DoUnlock(mockStore, mockResourceLocks, !READ_ONLY);
        doUnlock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();

    }

}
