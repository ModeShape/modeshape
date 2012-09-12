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

public class DoPut extends AbstractMethod {

    private static Logger LOG = Logger.getLogger(DoPut.class);

    private final IWebdavStore store;
    private final IResourceLocks resourceLocks;
    private final boolean readOnly;
    private final boolean lazyFolderCreationOnPut;

    private String userAgent;

    public DoPut( IWebdavStore store,
                  IResourceLocks resLocks,
                  boolean readOnly,
                  boolean lazyFolderCreationOnPut ) {
        this.store = store;
        this.resourceLocks = resLocks;
        this.readOnly = readOnly;
        this.lazyFolderCreationOnPut = lazyFolderCreationOnPut;
    }

    @Override
    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (!readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(path);

            userAgent = req.getHeader("User-Agent");

            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!isUnlocked(transaction, req, resourceLocks, parentPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // parent is locked
            }

            if (!isUnlocked(transaction, req, resourceLocks, path)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // resource is locked
            }

            String tempLockOwner = "doPut" + System.currentTimeMillis() + req.toString();
            if (resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so = null;
                try {
                    parentSo = store.getStoredObject(transaction, parentPath);
                    if (parentPath != null && parentSo != null && parentSo.isResource()) {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                        return;

                    } else if (parentPath != null && parentSo == null && lazyFolderCreationOnPut) {
                        store.createFolder(transaction, parentPath);

                    } else if (parentPath != null && parentSo == null && !lazyFolderCreationOnPut) {
                        errorList.put(parentPath, WebdavStatus.SC_NOT_FOUND);
                        sendReport(req, resp, errorList);
                        return;
                    }

                    so = store.getStoredObject(transaction, path);

                    if (so == null) {
                        store.createResource(transaction, path);
                        // resp.setStatus(WebdavStatus.SC_CREATED);
                    } else {
                        // This has already been created, just update the data
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
                                so.setFolder(false);

                                String[] nullResourceLockOwners = nullResourceLo.getOwner();
                                String owner = null;
                                if (nullResourceLockOwners != null) {
                                    owner = nullResourceLockOwners[0];
                                }

                                if (!resourceLocks.unlock(transaction, lockToken, owner)) {
                                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                }
                            } else {
                                errorList.put(path, WebdavStatus.SC_LOCKED);
                                sendReport(req, resp, errorList);
                            }
                        }
                    }
                    // User-Agent workarounds
                    doUserAgentWorkaround(resp);

                    // setting resourceContent
                    long resourceLength = store.setResourceContent(transaction, path, req.getInputStream(), null, null);

                    so = store.getStoredObject(transaction, path);
                    if (resourceLength != -1) {
                        so.setResourceLength(resourceLength);
                    }
                    // Now lets report back what was actually saved

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

    /**
     * @param resp
     */
    private void doUserAgentWorkaround( HttpServletResponse resp ) {
        if (userAgent != null && userAgent.contains("WebDAVFS") && !userAgent.contains("Transmit")) {
            LOG.trace("DoPut.execute() : do workaround for user agent '" + userAgent + "'");
            resp.setStatus(WebdavStatus.SC_CREATED);
        } else if (userAgent != null && userAgent.contains("Transmit")) {
            // Transmit also uses WEBDAVFS 1.x.x but crashes
            // with SC_CREATED response
            LOG.trace("DoPut.execute() : do workaround for user agent '" + userAgent + "'");
            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        } else {
            resp.setStatus(WebdavStatus.SC_CREATED);
        }
    }
}
