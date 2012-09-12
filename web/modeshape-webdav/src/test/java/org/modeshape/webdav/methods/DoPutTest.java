/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;

@SuppressWarnings( "synthetic-access" )
public class DoPutTest extends AbstractWebDAVTest {
    private static final String PARENT_PATH = "/parentCollection";
    private static final String PATH = PARENT_PATH + "/fileToPut";
    private static final boolean LAZY_FOLDER_CREATION_ON_PUT = true;

    @Test
    public void testDoPutIfReadOnlyTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), READ_ONLY, LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfReadOnlyFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject fileSo = null;

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));

                one(mockStore).createResource(mockTransaction, PATH);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();

                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                fileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !READ_ONLY, LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsFalse() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(null));

                one(mockRes).sendError(with(equal(WebdavStatus.SC_NOT_FOUND)), with(any(String.class)));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !READ_ONLY, !LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, PARENT_PATH);

                StoredObject fileSo = null;

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));

                one(mockStore).createResource(mockTransaction, PATH);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                one(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                fileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));

            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !READ_ONLY, LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfParentPathIsResource() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !READ_ONLY, LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnALockNullResource() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

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

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, PARENT_PATH);

                parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, PATH);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, PATH, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, PATH);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
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

                // // -----LOCK on a non-existing resource successful------
                // // --------now doPUT on the lock-null resource----------

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(PATH));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                one(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                one(mockResourceLocks).getLockedObjectByID(mockTransaction, loId);
                will(returnValue(lockNullResourceLo));

                one(mockResourceLocks).lock(with(any(ITransaction.class)),
                                            with(any(String.class)),
                                            with(any(String.class)),
                                            with(any(boolean.class)),
                                            with(any(int.class)),
                                            with(any(int.class)),
                                            with(any(boolean.class)));
                will(returnValue(true));

                parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

                one(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                @SuppressWarnings( "null" )
                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                one(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                one(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                StoredObject newResourceSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(newResourceSo));

                one(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                                                                    with(any(String.class)),
                                                                    with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !READ_ONLY);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoPut doPut = new DoPut(mockStore, mockResourceLocks, !READ_ONLY, LAZY_FOLDER_CREATION_ON_PUT);
        doPut.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
