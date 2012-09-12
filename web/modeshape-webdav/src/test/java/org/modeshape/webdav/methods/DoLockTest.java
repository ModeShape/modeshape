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
public class DoLockTest extends AbstractWebDAVTest {

    private static final boolean EXCLUSIVE = true;
    private static final String DEPTH_STRING = "-1";
    private static final int DEPTH = -1;
    private static final String TIMEOUT_STRING = "10";

    @Test
    public void testDoLockIfReadOnly() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoLock doLock = new DoLock(mockStore, resLocks, READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoRefreshLockOnLockedResource() throws Exception {

        final String lockPath = "/aFileToLock";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, OWNER, EXCLUSIVE, DEPTH, TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        String lockTokenString = lo.getID();
        final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("If");
                will(returnValue(lockToken));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                exactly(2).of(mockReq).getHeader("If");
                will(returnValue(lockToken));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                one(mockRes).addHeader("Lock-Token", lockToken.substring(lockToken.indexOf("(") + 1, lockToken.indexOf(")")));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoExclusiveLockOnResource() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(DEPTH_STRING));

                one(mockReq).getHeader("Timeout");
                will(returnValue(TIMEOUT_STRING));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoSharedLockOnResource() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(sharedLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(DEPTH_STRING));

                one(mockReq).getHeader("Timeout");
                will(returnValue(TIMEOUT_STRING));

                one(mockRes).setStatus(WebdavStatus.SC_OK);
                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoExclusiveLockOnCollection() throws Exception {
        final String lockPath = "/aFolderToLock";

        ResourceLocks resLocks = new ResourceLocks();

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(DEPTH_STRING));

                one(mockReq).getHeader("Timeout");
                will(returnValue(TIMEOUT_STRING));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoSharedLockOnCollection() throws Exception {
        final String lockPath = "/aFolderToLock";

        ResourceLocks resLocks = new ResourceLocks();

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(sharedLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(DEPTH_STRING));

                one(mockReq).getHeader("Timeout");
                will(returnValue(TIMEOUT_STRING));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(getPrintWriter()));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoLockNullResourceLock() throws Exception {
        final String parentPath = "/parentCollection";
        final String lockPath = parentPath.concat("/aNullResource");

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, lockPath);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;
                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, parentPath);
                will(returnValue(parentLo));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockResourceLocks).lock(with(any(ITransaction.class)),
                                            with(any(String.class)),
                                            with(any(String.class)),
                                            with(any(boolean.class)),
                                            with(any(int.class)),
                                            with(any(int.class)),
                                            with(any(boolean.class)));
                will(returnValue(true));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, parentPath);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, lockPath);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, lockPath, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, lockPath);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, lockPath);
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
                one(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                                                                    with(any(String.class)),
                                                                    with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
