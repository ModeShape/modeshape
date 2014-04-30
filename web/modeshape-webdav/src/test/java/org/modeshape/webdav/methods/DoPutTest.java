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
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));

                oneOf(mockStore).createResource(mockTransaction, PATH);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();

                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                fileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, PATH);
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(null));

                oneOf(mockRes).sendError(with(equal(WebdavStatus.SC_NOT_FOUND)), with(any(String.class)));
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
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                oneOf(mockReq).getContentLength();
                will(returnValue(2));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, PARENT_PATH);

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(fileSo));

                oneOf(mockStore).createResource(mockTransaction, PATH);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

                oneOf(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                fileSo = initFileStoredObject(RESOURCE_CONTENT);

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
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
               oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

               oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

               oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

               oneOf(mockReq).getContentLength();
                will(returnValue(2));

                StoredObject parentSo = initFileStoredObject(RESOURCE_CONTENT);

               oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

               oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
               oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

               oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

                LockedObject lockNullResourceLo = null;

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

               oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

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

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

               oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

               oneOf(mockStore).createFolder(mockTransaction, PARENT_PATH);

                parentSo = initFolderStoredObject();

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

               oneOf(mockStore).createResource(mockTransaction, PATH);

                lockNullResourceSo = initLockNullStoredObject();

               oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

               oneOf(mockReq).getInputStream();
                will(returnValue(exclusiveLockRequestStream()));

               oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

               oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

               oneOf(mockResourceLocks).exclusiveLock(mockTransaction, PATH, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, PATH);

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
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

                // // -----LOCK on a non-existing resource successful------
                // // --------now doPUT on the lock-null resource----------

               oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

               oneOf(mockReq).getPathInfo();
                will(returnValue(PATH));

               oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PARENT_PATH);
                will(returnValue(parentLo));

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

               oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

               oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction, loId);
                will(returnValue(lockNullResourceLo));

               oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                                            with(any(String.class)),
                                            with(any(String.class)),
                                            with(any(boolean.class)),
                                            with(any(int.class)),
                                            with(any(int.class)),
                                            with(any(boolean.class)));
                will(returnValue(true));

                parentSo = initFolderStoredObject();

               oneOf(mockStore).getStoredObject(mockTransaction, PARENT_PATH);
                will(returnValue(parentSo));

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(lockNullResourceSo));

               oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, PATH);
                will(returnValue(lockNullResourceLo));

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

               oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

               oneOf(mockReq).getInputStream();
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));

               oneOf(mockStore).setResourceContent(mockTransaction, PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                StoredObject newResourceSo = initFileStoredObject(RESOURCE_CONTENT);

               oneOf(mockStore).getStoredObject(mockTransaction, PATH);
                will(returnValue(newResourceSo));

               oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
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
