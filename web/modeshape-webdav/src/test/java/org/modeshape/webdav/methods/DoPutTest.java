/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

                one(mockReq).getContentLength();
                will(returnValue(2));

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

                one(mockReq).getContentLength();
                will(returnValue(2));

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
