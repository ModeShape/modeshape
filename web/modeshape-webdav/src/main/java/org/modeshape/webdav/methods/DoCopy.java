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
import org.modeshape.webdav.exceptions.ObjectAlreadyExistsException;
import org.modeshape.webdav.exceptions.ObjectNotFoundException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.fromcatalina.RequestUtil;
import org.modeshape.webdav.locking.ResourceLocks;
import java.io.IOException;
import java.util.Hashtable;

public class DoCopy extends AbstractMethod {

    private static Logger LOG = Logger.getLogger(DoCopy.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private DoDelete _doDelete;
    private boolean _readOnly;

    public DoCopy( IWebdavStore store,
                   ResourceLocks resourceLocks,
                   DoDelete doDelete,
                   boolean readOnly ) {
        _store = store;
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _readOnly = readOnly;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        String path = getRelativePath(req);
        if (!_readOnly) {

            String tempLockOwner = "doCopy" + System.currentTimeMillis() + req.toString();
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    copyResource(transaction, req, resp);
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_CONFLICT, req.getRequestURI());
                } catch (ObjectNotFoundException e) {
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
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }

    /**
     * Copy a resource.
     *
     * @param transaction indicates that the method is within the scope of a WebDAV
     * transaction
     * @param req Servlet request
     * @param resp Servlet response
     * @return true if the copy is successful
     * @throws WebdavException if an error in the underlying store occurs
     * @throws IOException when an error occurs while sending the response
     * @throws LockFailedException
     */
    public boolean copyResource( ITransaction transaction,
                                 HttpServletRequest req,
                                 HttpServletResponse resp ) throws WebdavException, IOException, LockFailedException {

        // Parsing destination header
        String destinationPath = parseDestinationHeader(req, resp);

        if (destinationPath == null) {
            return false;
        }

        String path = getRelativePath(req);

        if (path.equals(destinationPath)) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return false;
        }

        Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
        String parentDestinationPath = getParentPath(getCleanPath(destinationPath));

        if (!isUnlocked(transaction, req, resp, _resourceLocks, parentDestinationPath)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return false; // parentDestination is locked
        }

        if (!isUnlocked(transaction, req, resp, _resourceLocks, destinationPath)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return false; // destination is locked
        }

        // Parsing overwrite header
        boolean overwrite = shouldOverwrite(req);

        // Overwriting the destination
        String lockOwner = "copyResource" + System.currentTimeMillis() + req.toString();

        if (_resourceLocks.lock(transaction, destinationPath, lockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject copySo, destinationSo = null;
            try {
                copySo = _store.getStoredObject(transaction, path);
                // Retrieve the resources
                if (copySo == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return false;
                }

                if (copySo.isNullResource()) {
                    String methodsAllowed = DeterminableMethod.determineMethodsAllowed(copySo);
                    resp.addHeader("Allow", methodsAllowed);
                    resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                    return false;
                }

                errorList = new Hashtable<String, Integer>();

                destinationSo = _store.getStoredObject(transaction, destinationPath);

                if (overwrite) {

                    // Delete destination resource, if it exists
                    if (destinationSo != null) {
                        _doDelete.deleteResource(transaction, destinationPath, errorList, req, resp);
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }
                } else {
                    // If the destination exists, then it's a conflict
                    if (destinationSo != null) {
                        resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                        return false;
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }

                }
                copy(transaction, path, destinationPath, errorList, req, resp);

                if (!errorList.isEmpty()) {
                    sendReport(req, resp, errorList);
                }

            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, destinationPath, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        return true;

    }

    private boolean shouldOverwrite( HttpServletRequest req ) {
        boolean overwrite = true;
        String overwriteHeader = req.getHeader("Overwrite");

        if (overwriteHeader != null) {
            overwrite = overwriteHeader.equalsIgnoreCase("T");
        }
        return overwrite;
    }

    /**
     * copies the specified resource(s) to the specified destination.
     * preconditions must be handled by the caller. Standard status codes must
     * be handled by the caller. a multi status report in case of errors is
     * created here.
     *
     * @param transaction indicates that the method is within the scope of a WebDAV
     * transaction
     * @param sourcePath path from where to read
     * @param destinationPath path where to write
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws WebdavException if an error in the underlying store occurs
     * @throws IOException
     */
    private void copy( ITransaction transaction,
                       String sourcePath,
                       String destinationPath,
                       Hashtable<String, Integer> errorList,
                       HttpServletRequest req,
                       HttpServletResponse resp ) throws WebdavException, IOException {

        StoredObject sourceSo = _store.getStoredObject(transaction, sourcePath);
        if (sourceSo.isResource()) {
            _store.createResource(transaction, destinationPath);
            long resourceLength = _store.setResourceContent(transaction, destinationPath, _store.getResourceContent(transaction,
                                                                                                                    sourcePath),
                                                            null, null);

            if (resourceLength != -1) {
                StoredObject destinationSo = _store.getStoredObject(transaction, destinationPath);
                destinationSo.setResourceLength(resourceLength);
            }

        } else {

            if (sourceSo.isFolder()) {
                copyFolder(transaction, sourcePath, destinationPath, errorList, req, resp);
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        }
    }

    /**
     * helper method of copy() recursively copies the FOLDER at source path to
     * destination path
     *
     * @param transaction indicates that the method is within the scope of a WebDAV
     * transaction
     * @param sourcePath where to read
     * @param destinationPath where to write
     * @param errorList all errors that ocurred
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @throws WebdavException if an error in the underlying store occurs
     */
    private void copyFolder( ITransaction transaction,
                             String sourcePath,
                             String destinationPath,
                             Hashtable<String, Integer> errorList,
                             HttpServletRequest req,
                             HttpServletResponse resp ) throws WebdavException {

        _store.createFolder(transaction, destinationPath);
        boolean infiniteDepth = true;
        String depth = req.getHeader("Depth");
        if (depth != null) {
            if (depth.equals("0")) {
                infiniteDepth = false;
            }
        }
        if (infiniteDepth) {
            String[] children = _store.getChildrenNames(transaction, sourcePath);
            children = children == null ? new String[] { } : children;

            StoredObject childSo;
            for (int i = children.length - 1; i >= 0; i--) {
                children[i] = "/" + children[i];
                try {
                    childSo = _store.getStoredObject(transaction, (sourcePath + children[i]));
                    if (childSo.isResource()) {
                        _store.createResource(transaction, destinationPath + children[i]);
                        long resourceLength = _store.setResourceContent(transaction, destinationPath + children[i],
                                                                        _store.getResourceContent(transaction,
                                                                                                  sourcePath + children[i]),
                                                                        null, null);

                        if (resourceLength != -1) {
                            StoredObject destinationSo = _store.getStoredObject(transaction, destinationPath + children[i]);
                            destinationSo.setResourceLength(resourceLength);
                        }

                    } else {
                        copyFolder(transaction, sourcePath + children[i], destinationPath + children[i], errorList, req, resp);
                    }
                } catch (AccessDeniedException e) {
                    errorList.put(destinationPath + children[i], WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectNotFoundException e) {
                    errorList.put(destinationPath + children[i], WebdavStatus.SC_NOT_FOUND);
                } catch (ObjectAlreadyExistsException e) {
                    errorList.put(destinationPath + children[i], WebdavStatus.SC_CONFLICT);
                } catch (WebdavException e) {
                    errorList.put(destinationPath + children[i], WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * Parses and normalizes the destination header.
     *
     * @param req Servlet request
     * @param resp Servlet response
     * @return destinationPath
     * @throws IOException if an error occurs while sending response
     */
    private String parseDestinationHeader( HttpServletRequest req,
                                           HttpServletResponse resp ) throws IOException {
        String destinationPath = req.getHeader("Destination");

        if (destinationPath == null) {
            resp.sendError(WebdavStatus.SC_BAD_REQUEST);
            return null;
        }

        // Remove url encoding from destination
        destinationPath = RequestUtil.URLDecode(destinationPath, "UTF8");

        int protocolIndex = destinationPath.indexOf("://");
        if (protocolIndex >= 0) {
            // if the Destination URL contains the protocol, we can safely
            // trim everything upto the first "/" character after "://"
            int firstSeparator = destinationPath.indexOf("/", protocolIndex + 4);
            if (firstSeparator < 0) {
                destinationPath = "/";
            } else {
                destinationPath = destinationPath.substring(firstSeparator);
            }
        } else {
            String hostName = req.getServerName();
            if ((hostName != null) && (destinationPath.startsWith(hostName))) {
                destinationPath = destinationPath.substring(hostName.length());
            }

            int portIndex = destinationPath.indexOf(":");
            if (portIndex >= 0) {
                destinationPath = destinationPath.substring(portIndex);
            }

            if (destinationPath.startsWith(":")) {
                int firstSeparator = destinationPath.indexOf("/");
                if (firstSeparator < 0) {
                    destinationPath = "/";
                } else {
                    destinationPath = destinationPath.substring(firstSeparator);
                }
            }
        }

        // Normalize destination path (remove '.' and' ..')
        destinationPath = normalize(destinationPath);

        String contextPath = req.getContextPath();
        if ((contextPath != null) && (destinationPath.startsWith(contextPath))) {
            destinationPath = destinationPath.substring(contextPath.length());
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String servletPath = req.getServletPath();
            if ((servletPath != null) && (destinationPath.startsWith(servletPath))) {
                destinationPath = destinationPath.substring(servletPath.length());
            }
        }

        return destinationPath;
    }

    /**
     * Return a context-relative path, beginning with a "/", that represents the
     * canonical version of the specified path after ".." and "." elements are
     * resolved out. If the specified path attempts to go outside the boundaries
     * of the current context (i.e. too many ".." path elements are present),
     * return <code>null</code> instead.
     *
     * @param path Path to be normalized
     * @return normalized path
     */
    protected String normalize( String path ) {

        if (path == null) {
            return null;
        }

        // Create a place for the normalized path
        String normalized = path;

        if (normalized.equals("/.")) {
            return "/";
        }

        // Normalize the slashes and add leading slash if necessary
        if (normalized.indexOf('\\') >= 0) {
            normalized = normalized.replace('\\', '/');
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // Resolve occurrences of "//" in the normalized path
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0) {
                break;
            }
            if (index == 0) {
                return (null); // Trying to go outside our context
            }
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }

        // Return the normalized path that we have completed
        return (normalized);

    }

}
