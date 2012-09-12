/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.webdav.methods;

import java.io.IOException;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.AccessDeniedException;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.IResourceLocks;
import org.modeshape.webdav.locking.LockedObject;

public class DoMkcol extends AbstractMethod {

    private static Logger LOG = Logger.getLogger(DoMkcol.class);

    private final IWebdavStore store;
    private final IResourceLocks resourceLocks;
    private boolean readOnly;

    public DoMkcol( IWebdavStore store,
                    IResourceLocks resourceLocks,
                    boolean readOnly ) {
        this.store = store;
        this.resourceLocks = resourceLocks;
        this.readOnly = readOnly;
    }

    @Override
    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (!readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(getCleanPath(path));

            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!isUnlocked(transaction, req, resourceLocks, parentPath)) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
                return;
            }

            String tempLockOwner = "doMkcol" + System.currentTimeMillis() + req.toString();

            if (resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so = null;
                try {
                    parentSo = store.getStoredObject(transaction, parentPath);
                    if (parentSo == null) {
                        // parent not exists
                        resp.sendError(WebdavStatus.SC_CONFLICT);
                        return;
                    }
                    if (parentPath != null && parentSo.isFolder()) {
                        so = store.getStoredObject(transaction, path);
                        if (so == null) {
                            store.createFolder(transaction, path);
                            resp.setStatus(WebdavStatus.SC_CREATED);
                        } else {
                            // object already exists
                            if (so.isNullResource()) {

                                LockedObject nullResourceLo = resourceLocks.getLockedObjectByPath(transaction, path);
                                if (nullResourceLo == null) {
                                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                }
                                String nullResourceLockToken = nullResourceLo.getID();
                                String[] lockTokens = getLockIdFromIfHeader(req);
                                String lockToken = null;
                                if (lockTokens != null) {
                                    lockToken = lockTokens[0];
                                } else {
                                    resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                                    return;
                                }
                                if (lockToken.equals(nullResourceLockToken)) {
                                    so.setNullResource(false);
                                    so.setFolder(true);

                                    String[] nullResourceLockOwners = nullResourceLo.getOwner();
                                    String owner = null;
                                    if (nullResourceLockOwners != null) {
                                        owner = nullResourceLockOwners[0];
                                    }

                                    if (resourceLocks.unlock(transaction, lockToken, owner)) {
                                        resp.setStatus(WebdavStatus.SC_CREATED);
                                    } else {
                                        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    }

                                } else {
                                    errorList.put(path, WebdavStatus.SC_LOCKED);
                                    sendReport(req, resp, errorList);
                                }

                            } else {
                                String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
                                resp.addHeader("Allow", methodsAllowed);
                                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                            }
                        }

                    } else if (parentPath != null && parentSo.isResource()) {
                        String methodsAllowed = DeterminableMethod.determineMethodsAllowed(parentSo);
                        resp.addHeader("Allow", methodsAllowed);
                        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

                    } else {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

}
