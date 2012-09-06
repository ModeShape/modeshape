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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.IMimeTyper;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.exceptions.AccessDeniedException;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.exceptions.ObjectAlreadyExistsException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.ResourceLocks;
import java.io.IOException;

public class DoHead extends AbstractMethod {

    protected String _dftIndexFile;
    protected IWebdavStore _store;
    protected String _insteadOf404;
    protected ResourceLocks _resourceLocks;
    protected IMimeTyper _mimeTyper;
    protected int _contentLength;

    private static Logger LOG = Logger.getLogger(DoHead.class);

    public DoHead( IWebdavStore store,
                   String dftIndexFile,
                   String insteadOf404,
                   ResourceLocks resourceLocks,
                   IMimeTyper mimeTyper,
                   int contentLengthHeader ) {
        _store = store;
        _dftIndexFile = dftIndexFile;
        _insteadOf404 = insteadOf404;
        _resourceLocks = resourceLocks;
        _mimeTyper = mimeTyper;
        _contentLength = contentLengthHeader;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {

        // determines if the uri exists.

        boolean bUriExists = false;

        String path = getRelativePath(req);
        LOG.trace("-- " + this.getClass().getName());

        StoredObject so = _store.getStoredObject(transaction, path);
        if (so == null) {
            if (this._insteadOf404 != null && !_insteadOf404.trim().equals("")) {
                path = this._insteadOf404;
                so = _store.getStoredObject(transaction, this._insteadOf404);
            }
        } else {
            bUriExists = true;
        }

        if (so != null) {
            if (so.isFolder()) {
                if (_dftIndexFile != null && !_dftIndexFile.trim().equals("")) {
                    resp.sendRedirect(resp.encodeRedirectURL(req.getRequestURI() + this._dftIndexFile));
                    return;
                }
            } else if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }

            String tempLockOwner = "doGet" + System.currentTimeMillis() + req.toString();

            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    String eTagMatch = req.getHeader("If-None-Match");
                    if (eTagMatch != null) {
                        if (eTagMatch.equals(getETag(so))) {
                            resp.setStatus(WebdavStatus.SC_NOT_MODIFIED);
                            return;
                        }
                    }

                    if (so.isResource()) {
                        // path points to a file but ends with / or \
                        if (path.endsWith("/") || (path.endsWith("\\"))) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                        } else {

                            // setting headers
                            long lastModified = so.getLastModified().getTime();
                            resp.setDateHeader("last-modified", lastModified);

                            String eTag = getETag(so);
                            resp.addHeader("ETag", eTag);

                            long resourceLength = so.getResourceLength();

                            if (_contentLength == 1) {
                                if (resourceLength > 0) {
                                    if (resourceLength <= Integer.MAX_VALUE) {
                                        resp.setContentLength((int)resourceLength);
                                    } else {
                                        resp.setHeader("content-length", "" + resourceLength);
                                        // is "content-length" the right header?
                                        // is long a valid format?
                                    }
                                }
                            }

                            String mimeType = _mimeTyper.getMimeType(transaction, path);
                            if (mimeType != null) {
                                resp.setContentType(mimeType);
                            } else {
                                int lastSlash = path.replace('\\', '/').lastIndexOf('/');
                                int lastDot = path.indexOf(".", lastSlash);
                                if (lastDot == -1) {
                                    resp.setContentType("text/html");
                                }
                            }

                            doBody(transaction, resp, path);
                        }
                    } else {
                        folderBody(transaction, path, resp, req);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            folderBody(transaction, path, resp, req);
        }

        if (!bUriExists) {
            resp.setStatus(WebdavStatus.SC_NOT_FOUND);
        }

    }

    protected void folderBody( ITransaction transaction,
                               String path,
                               HttpServletResponse resp,
                               HttpServletRequest req ) throws IOException {
        // no body for HEAD
    }

    protected void doBody( ITransaction transaction,
                           HttpServletResponse resp,
                           String path ) throws IOException {
        // no body for HEAD
    }
}
