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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.AccessDeniedException;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.exceptions.ObjectAlreadyExistsException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.ResourceLocks;
import java.io.IOException;
import java.util.Hashtable;

public class DoMove extends AbstractMethod {

    private static Logger LOG = Logger.getLogger(DoMove.class);

    private final ResourceLocks resourceLocks;
    private final DoDelete doDelete;
    private final DoCopy doCopy;
    private final boolean readOnly;

    public DoMove( ResourceLocks resourceLocks,
                   DoDelete doDelete,
                   DoCopy doCopy,
                   boolean readOnly ) {
        this.resourceLocks = resourceLocks;
        this.doDelete = doDelete;
        this.doCopy = doCopy;
        this.readOnly = readOnly;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {

        if (!readOnly) {
            LOG.trace("-- " + this.getClass().getName());

            String sourcePath = getRelativePath(req);
            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!isUnlocked(transaction, req, resourceLocks, sourcePath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String destinationPath = req.getHeader("Destination");
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!isUnlocked(transaction, req, resourceLocks, destinationPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String tempLockOwner = "doMove" + System.currentTimeMillis() + req.toString();

            if (resourceLocks.lock(transaction, sourcePath, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    if (doCopy.copyResource(transaction, req, resp)) {

                        errorList = new Hashtable<String, Integer>();
                        doDelete.deleteResource(transaction, sourcePath, errorList, req, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(req, resp, errorList);
                        }
                    }

                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    resourceLocks.unlockTemporaryLockedObjects(transaction, sourcePath, tempLockOwner);
                }
            } else {
                errorList.put(req.getHeader("Destination"), WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }
    }
}
