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
import org.modeshape.webdav.exceptions.ObjectAlreadyExistsException;
import org.modeshape.webdav.exceptions.ObjectNotFoundException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.ResourceLocks;

public class DoDelete extends AbstractMethod {

    private static Logger LOG = Logger.getLogger(DoDelete.class);

    private final IWebdavStore store;
    private final ResourceLocks resourceLocks;
    private final boolean readOnly;

    public DoDelete( IWebdavStore store,
                     ResourceLocks resourceLocks,
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

            if (!isUnlocked(transaction, req, resourceLocks, parentPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // parent is locked
            }

            if (!isUnlocked(transaction, req, resourceLocks, path)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // resource is locked
            }

            String tempLockOwner = "doDelete" + System.currentTimeMillis() + req.toString();
            if (resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
                    deleteResource(transaction, path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
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
     * deletes the recources at "path"
     * 
     * @param transaction indicates that the method is within the scope of a WebDAV transaction
     * @param path the folder to be deleted
     * @param errorList all errors that ocurred
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws WebdavException if an error in the underlying store occurs
     * @throws IOException when an error occurs while sending the response
     */
    public void deleteResource( ITransaction transaction,
                                String path,
                                Hashtable<String, Integer> errorList,
                                HttpServletRequest req,
                                HttpServletResponse resp ) throws IOException, WebdavException {

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

        if (!readOnly) {
            StoredObject so = store.getStoredObject(transaction, path);
            if (so != null) {
                if (so.isResource()) {
                    store.removeObject(transaction, path);
                } else {
                    if (so.isFolder()) {
                        deleteFolder(transaction, path, errorList);
                        store.removeObject(transaction, path);
                    } else {
                        resp.sendError(WebdavStatus.SC_NOT_FOUND);
                    }
                }
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
     * helper method of deleteResource() deletes the folder and all of its contents
     * 
     * @param transaction indicates that the method is within the scope of a WebDAV transaction
     * @param path the folder to be deleted
     * @param errorList all errors that ocurred
     * @throws WebdavException if an error in the underlying store occurs
     */
    private void deleteFolder( ITransaction transaction,
                               String path,
                               Hashtable<String, Integer> errorList ) throws WebdavException {

        String[] children = store.getChildrenNames(transaction, path);
        children = children == null ? new String[] {} : children;
        StoredObject so = null;
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = "/" + children[i];
            try {
                so = store.getStoredObject(transaction, path + children[i]);
                if (so.isResource()) {
                    store.removeObject(transaction, path + children[i]);

                } else {
                    deleteFolder(transaction, path + children[i], errorList);
                    store.removeObject(transaction, path + children[i]);
                }
            } catch (AccessDeniedException e) {
                errorList.put(path + children[i], WebdavStatus.SC_FORBIDDEN);
            } catch (ObjectNotFoundException e) {
                errorList.put(path + children[i], WebdavStatus.SC_NOT_FOUND);
            } catch (WebdavException e) {
                errorList.put(path + children[i], WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

}
