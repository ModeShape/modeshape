package org.modeshape.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.locking.IResourceLocks;
import org.modeshape.webdav.locking.LockedObject;
import java.io.IOException;

public class DoUnlock extends DeterminableMethod {

    private static Logger LOG = Logger.getLogger(DoUnlock.class);

    private IWebdavStore _store;
    private IResourceLocks _resourceLocks;
    private boolean _readOnly;

    public DoUnlock( IWebdavStore store,
                     IResourceLocks resourceLocks,
                     boolean readOnly ) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else {

            String path = getRelativePath(req);
            String tempLockOwner = "doUnlock" + System.currentTimeMillis() + req.toString();
            try {
                if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {

                    String lockId = getLockIdFromLockTokenHeader(req);
                    LockedObject lo;
                    if (lockId != null && ((lo = _resourceLocks.getLockedObjectByID(transaction, lockId)) != null)) {

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
                            } else {
                                owner = null;
                            }
                        }

                        if (_resourceLocks.unlock(transaction, lockId, owner)) {
                            StoredObject so = _store.getStoredObject(transaction, path);
                            if (so.isNullResource()) {
                                _store.removeObject(transaction, path);
                            }

                            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
                        } else {
                            LOG.trace("DoUnlock failure at " + lo.getPath());
                            resp.sendError(WebdavStatus.SC_METHOD_FAILURE);
                        }

                    } else {
                        resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                    }
                }
            } catch (LockFailedException e) {
                e.printStackTrace();
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
            }
        }
    }

}
