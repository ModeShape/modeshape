package org.modeshape.webdav.methods;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.locking.IResourceLocks;
import org.modeshape.webdav.locking.LockedObject;

public class DoUnlock extends DeterminableMethod {

    private static Logger LOG = Logger.getLogger(DoUnlock.class);

    private final IWebdavStore store;
    private final IResourceLocks resourceLocks;
    private final boolean readOnly;

    public DoUnlock( IWebdavStore store,
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

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else {

            String path = getRelativePath(req);
            String tempLockOwner = "doUnlock" + System.currentTimeMillis() + req.toString();
            try {
                if (resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {

                    String lockId = getLockIdFromLockTokenHeader(req);
                    LockedObject lo;
                    if (lockId != null && ((lo = resourceLocks.getLockedObjectByID(transaction, lockId)) != null)) {

                        String[] owners = lo.getOwner();
                        String owner = null;
                        if (lo.isShared()) {
                            // more than one owner is possible
                            if (owners != null) {
                                for (int i = 0; i < owners.length; i++) {
                                    // remove owner from LockedObject
                                    lo.removeLockedObjectOwner(owners[i]);
                                }
                            }
                        } else {
                            // exclusive, only one lock owner
                            if (owners != null) {
                                owner = owners[0];
                            }
                        }

                        if (resourceLocks.unlock(transaction, lockId, owner)) {
                            StoredObject so = store.getStoredObject(transaction, path);
                            if (so == null) {
                                resp.setStatus(WebdavStatus.SC_NOT_FOUND);
                            } else {
                                if (so.isNullResource()) {
                                    store.removeObject(transaction, path);
                                }
                                resp.setStatus(WebdavStatus.SC_NO_CONTENT);
                            }
                        } else {
                            LOG.trace("DoUnlock failure at " + lo.getPath());
                            resp.sendError(WebdavStatus.SC_METHOD_FAILURE);
                        }

                    } else {
                        resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                    }
                }
            } catch (LockFailedException e) {
                LOG.warn(e, new TextI18n("Cannot unlock resource"));
            } finally {
                resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
            }
        }
    }

}
