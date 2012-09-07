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
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.AccessDeniedException;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.ResourceLocks;
import java.io.IOException;

public class DoOptions extends DeterminableMethod {

    private static Logger LOG = Logger.getLogger(DoOptions.class);

    private IWebdavStore store;
    private ResourceLocks resourceLocks;

    public DoOptions( IWebdavStore store,
                      ResourceLocks resLocks ) {
        this.store = store;
        resourceLocks = resLocks;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {

        LOG.trace("-- " + this.getClass().getName());

        String tempLockOwner = "doOptions" + System.currentTimeMillis() + req.toString();
        String path = getRelativePath(req);
        if (resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so = null;
            try {
                resp.addHeader("DAV", "1, 2");

                so = store.getStoredObject(transaction, path);
                String methodsAllowed = determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
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
    }
}
