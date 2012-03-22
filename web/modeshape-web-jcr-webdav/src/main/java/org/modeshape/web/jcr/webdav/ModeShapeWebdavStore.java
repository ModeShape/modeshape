/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.jcr.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.jcr.Item;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.web.jcr.RepositoryManager;

/**
 * Implementation of the {@code IWebdavStore} interface that uses a JCR repository as a backing store.
 * <p>
 * This implementation takes several OSX-specific WebDAV workarounds from the WebDAVImpl class in Drools Guvnor.
 * </p>
 */
public class ModeShapeWebdavStore implements IWebdavStore {

    private static final ThreadLocal<HttpServletRequest> THREAD_LOCAL_REQUEST = new ThreadLocal<HttpServletRequest>();

    /** OSX workaround */
    private static final Map<String, byte[]> OSX_DOUBLE_DATA = Collections.synchronizedMap(new WeakHashMap<String, byte[]>());

    private static final String CREATED_PROP_NAME = "jcr:created";

    private final RequestResolver requestResolver;
    private final ContentMapper contentMapper;

    private final Logger logger = Logger.getLogger(getClass());

    public ModeShapeWebdavStore( RequestResolver requestResolver,
                                 ContentMapper contentMapper ) {
        super();

        this.requestResolver = requestResolver;
        this.contentMapper = contentMapper;
    }

    /**
     * Updates thread-local storage for the current thread to reference the given request.
     * 
     * @param request the request to store in thread-local storage; null to clear the storage
     */
    static final void setRequest( HttpServletRequest request ) {
        THREAD_LOCAL_REQUEST.set(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITransaction begin( Principal principal ) {
        return new JcrSessionTransaction(principal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit( ITransaction transaction ) {
        CheckArg.isNotNull(transaction, "transaction");

        assert transaction instanceof JcrSessionTransaction;
        ((JcrSessionTransaction)transaction).commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback( ITransaction transaction ) {
        // No op. By not saving the session, we will let the session expire without committing any changes

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAuthentication( ITransaction transaction ) {
        // No op.
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.webdav.IWebdavStore#createFolder(net.sf.webdav.ITransaction, java.lang.String)
     */
    @Override
    public void createFolder( ITransaction transaction,
                              String folderUri ) {
        int ind = folderUri.lastIndexOf('/');
        String parentUri = folderUri.substring(0, ind + 1);
        String resourceName = folderUri.substring(ind + 1);

        try {
            logger.debug("WebDAV create folder at: " + parentUri);
            ResolvedRequest resolvedParent = resolveRequest(transaction, parentUri);
            logger.debug("WebDAV create folder at: " + resolvedParent);
            if (resolvedParent.getPath() == null) {
                if (resolvedParent.getRepositoryName() == null) {
                    // Can't create a repository ...
                    throw new WebdavException(WebdavI18n.cannotCreateRepository.text(resourceName));
                }
                if (resolvedParent.getWorkspaceName() != null) {
                    // Really trying to create a node under the root ...
                    resolvedParent = resolvedParent.withPath("/");
                } else {
                    // Can't create a workspace ...
                    I18n msg = WebdavI18n.cannotCreateWorkspaceInRepository;
                    throw new WebdavException(msg.text(resourceName, resolvedParent.getRepositoryName()));
                }
            }
            Node parentNode = nodeFor(transaction, resolvedParent);
            contentMapper.createFolder(parentNode, resourceName);

        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createResource( ITransaction transaction,
                                String resourceUri ) {
        // Mac OS X workaround from Drools Guvnor
        if (resourceUri.endsWith(".DS_Store")) return;

        int ind = resourceUri.lastIndexOf('/');
        String parentUri = resourceUri.substring(0, ind + 1);
        String resourceName = resourceUri.substring(ind + 1);

        // Mac OS X workaround from Drools Guvnor
        if (resourceName.startsWith("._")) {
            OSX_DOUBLE_DATA.put(resourceUri, null);
            return;
        }

        try {
            ResolvedRequest resolvedParent = resolveRequest(transaction, parentUri);
            if (resolvedParent.getPath() == null) {
                if (resolvedParent.getRepositoryName() == null) {
                    // Can't create a repository ...
                    throw new WebdavException(WebdavI18n.cannotCreateRepository.text(resourceName));
                }
                if (resolvedParent.getWorkspaceName() != null) {
                    // Really trying to create a node under the root ...
                    resolvedParent = resolvedParent.withPath("/");
                } else {
                    // Can't create a workspace ...
                    I18n msg = WebdavI18n.cannotCreateWorkspaceInRepository;
                    throw new WebdavException(msg.text(resourceName, resolvedParent.getRepositoryName()));
                }
            }
            Node parentNode = nodeFor(transaction, resolvedParent);
            contentMapper.createFile(parentNode, resourceName);

        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getChildrenNames( ITransaction transaction,
                                      String folderUri ) {
        try {
            logger.trace("WebDAV getChildrenNames(txn,\"" + folderUri + "\")");
            ResolvedRequest resolved = resolveRequest(transaction, folderUri);
            logger.trace("WebDAV -> resolves to: " + resolved);
            if (resolved.getPath() == null) {
                // It does not resolve to the path of a node, so see if the repository/workspace exist ...
                return childrenFor(transaction, resolved);
            }

            Node node = nodeFor(transaction, resolved); // throws exception if not found
            logger.trace("WebDAV -> node: " + node);

            if (!isFolder(node)) return null; // no children

            List<String> children = namesOfChildren(node);
            logger.trace("WebDAV -> children: " + children);
            return children.toArray(new String[children.size()]);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    protected static List<String> namesOfChildren( Node node ) throws RepositoryException {
        List<String> children = new LinkedList<String>();
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            String name = child.getIndex() == 1 ? child.getName() : child.getName() + "[" + child.getIndex() + "]";
            children.add(name);
        }
        return children;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceContent( ITransaction transaction,
                                           String resourceUri ) {
        try {
            ResolvedRequest resolved = resolveRequest(transaction, resourceUri);
            if (resolved.getPath() == null) {
                // Not a node, so there's no content ...
                return null;
            }
            Node node = nodeFor(transaction, resolved); // throws exception if not found
            if (!isFile(node)) return null;
            return contentMapper.getResourceContent(node);

        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getResourceLength( ITransaction transaction,
                                   String uri ) {
        try {
            ResolvedRequest resolved = resolveRequest(transaction, uri);
            if (resolved.getPath() == null) {
                // Not a node, so there's no length ...
                return -1;
            }
            Node node = nodeFor(transaction, resolved); // throws exception if not found

            return contentMapper.getResourceLength(node);
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredObject getStoredObject( ITransaction transaction,
                                         String uri ) {
        if (uri.length() == 0) uri = "/";

        StoredObject ob = new StoredObject();
        try {
            logger.trace("WebDAV getStoredObject at \"" + uri + "\"");
            ResolvedRequest resolved = resolveRequest(transaction, uri);
            logger.debug("WebDAV getStoredObject at \"" + uri + "\" resolved to \"" + resolved + "\"");
            if (resolved.getPath() == null) {
                // It does not resolve to the path of a node, so see if the repository/workspace exist ...
                if (repositoryAndWorkspaceExist(transaction, resolved)) {
                    ob.setFolder(true);
                    Date now = new Date();
                    ob.setCreationDate(now);
                    ob.setLastModified(now);
                    ob.setResourceLength(0);
                    return ob;
                }
                // It does not exist, so return null
                return null;
            }

            Node node = nodeFor(transaction, resolved);

            if (isFolder(node)) {
                ob.setFolder(true);
                Date createDate = null;
                if (node.hasProperty(CREATED_PROP_NAME)) {
                    createDate = node.getProperty(CREATED_PROP_NAME).getDate().getTime();
                } else {
                    createDate = new Date();
                }
                ob.setCreationDate(createDate);
                ob.setLastModified(new Date());
                ob.setResourceLength(0);
            } else if (isFile(node)) {
                ob.setFolder(false);
                Date createDate = null;
                if (node.hasProperty(CREATED_PROP_NAME)) {
                    createDate = node.getProperty(CREATED_PROP_NAME).getDate().getTime();
                } else {
                    createDate = new Date();
                }
                ob.setCreationDate(createDate);
                ob.setLastModified(contentMapper.getLastModified(node));
                ob.setResourceLength(contentMapper.getResourceLength(node));
            } else {
                ob.setNullResource(true);
            }

        } catch (PathNotFoundException pnfe) {
            return null;
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
        return ob;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeObject( ITransaction transaction,
                              String uri ) {
        int ind = uri.lastIndexOf('/');
        String resourceName = uri.substring(ind + 1);

        // Mac OS X workaround from Drools Guvnor
        if (resourceName.startsWith("._")) {
            OSX_DOUBLE_DATA.put(uri, null);
            return;
        }

        try {
            ResolvedRequest resolved = resolveRequest(transaction, uri);
            if (resolved.getPath() != null) {
                // It does resolve to the path of a node, so try to find the node and remove it ...
                Node node = nodeFor(transaction, resolved);
                node.remove();
            }
            // Otherwise just return silently
        } catch (PathNotFoundException pnfe) {
            // Return silently
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long setResourceContent( ITransaction transaction,
                                    String resourceUri,
                                    InputStream content,
                                    String contentType,
                                    String characterEncoding ) {
        // Mac OS X workaround from Drools Guvnor
        if (resourceUri.endsWith(".DS_Store")) return 0;

        int ind = resourceUri.lastIndexOf('/');
        String resourceName = resourceUri.substring(ind + 1);

        // Mac OS X workaround from Drools Guvnor
        if (resourceName.startsWith("._")) {
            try {
                OSX_DOUBLE_DATA.put(resourceUri, IoUtil.readBytes(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }

        try {
            ResolvedRequest resolved = resolveRequest(transaction, resourceUri);
            if (resolved.getPath() == null) {
                // The request does not resolve to a node
                return -1;
            }
            // It does resolve to the path of a node, though the node may not exist ...
            Node node = nodeFor(transaction, resolved);
            if (!isFile(node)) {
                return -1;
            }

            return contentMapper.setContent(node, resourceName, content, contentType, characterEncoding);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RuntimeException t) {
            throw t;
        }
    }

    /**
     * @param node the node to check; may not be null
     * @return true if {@code node} represents a file (as opposed to a folder or file content)
     * @throws RepositoryException if an error occurs checking the node's primary type
     */
    private boolean isFile( Node node ) throws RepositoryException {
        return contentMapper.isFile(node);
    }

    /**
     * @param node the node to check; may not be null
     * @return true if {@code node} represents a folder (as opposed to a file or file content)
     * @throws RepositoryException if an error occurs checking the node's primary type
     */
    private boolean isFolder( Node node ) throws RepositoryException {
        return contentMapper.isFolder(node);
    }

    /**
     * Resolve the URI into a repository name, workspace name, and node path. Note that some URIs might not resolve to a
     * repository (but no workspace or path), a workspace (but no path), or even a repository.
     * 
     * @param transaction the active transaction; may not be null
     * @param uri the URI from the request
     * @return the resolved information; never null
     * @throws WebdavException if the URI is invalid or otherwise not anceptable
     */
    private final ResolvedRequest resolveRequest( ITransaction transaction,
                                                  String uri ) throws WebdavException {
        HttpServletRequest request = THREAD_LOCAL_REQUEST.get();
        return requestResolver.resolve(request, uri);
    }

    // private final String relativePathFrom( HttpServletRequest request ) {
    // // Are we being processed by a RequestDispatcher.include()?
    // if (request.getAttribute("javax.servlet.include.request_uri") != null) {
    // String result = (String)request.getAttribute("javax.servlet.include.path_info");
    // // if (result == null)
    // // result = (String) request
    // // .getAttribute("javax.servlet.include.servlet_path");
    // if ((result == null) || (result.equals(""))) result = "/";
    // return (result);
    // }
    // // No, extract the desired path directly from the request
    // String result = request.getPathInfo();
    // // if (result == null) {
    // // result = request.getServletPath();
    // // }
    // if ((result == null) || (result.equals(""))) {
    // result = "/";
    // }
    // return result;
    // }

    /**
     * Get the node that corresponds to the resolved request, using the supplied active transaction.
     * 
     * @param transaction the active transaction; may not be null
     * @param request the resolved request; may not be null and must contain a repository name and workspace name
     * @return the node; never null
     * @throws RepositoryException if the node does not exist, or if there is another problem obtaining the node
     */
    private final Node nodeFor( ITransaction transaction,
                                ResolvedRequest request ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).nodeFor(request);
    }

    /**
     * Determine if the repository and/or workspace named in the supplied request do exist.
     * 
     * @param transaction the active transaction; may not be null
     * @param request the resolved request; may not be null and must contain a repository name and workspace name
     * @return true if the repository and/or workspace do exist, or false otherwise
     * @throws RepositoryException if is a problem accessing the repository
     */
    private final boolean repositoryAndWorkspaceExist( ITransaction transaction,
                                                       ResolvedRequest request ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).repositoryAndWorkspaceExist(request);
    }

    /**
     * Determine the names of the children given the supplied request
     * 
     * @param transaction the active transaction; may not be null
     * @param request the resolved request; may not be null and must contain a repository name and workspace name
     * @return the children names, or null if there are no children
     * @throws RepositoryException if is a problem accessing the repository
     */
    private final String[] childrenFor( ITransaction transaction,
                                        ResolvedRequest request ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).childrenFor(request);
    }

    protected final RequestResolver requestResolver() {
        return requestResolver;
    }

    /**
     * Implementation of the {@link ITransaction} interface that uses a {@link Session JCR session} to load and store webdav
     * content. The session also provides support for transactional access to the underlying store.
     */
    class JcrSessionTransaction implements ITransaction {

        private final Map<SessionKey, Session> sessions = new HashMap<SessionKey, Session>();
        private final Principal principal;

        JcrSessionTransaction( Principal principal ) {
            this.principal = principal;
        }

        protected boolean owns( Session session ) {
            return sessions.containsValue(session);
        }

        /**
         * @param request the resolved request; may not be null
         * @return the session associated with this transaction; never null
         * @throws RepositoryException if there is a problem obtaining a repository session for the request
         */
        Session session( ResolvedRequest request ) throws RepositoryException {
            String repositoryName = request.getRepositoryName();
            String workspaceName = request.getWorkspaceName();
            assert repositoryName != null;
            assert workspaceName != null;
            SessionKey key = new SessionKey(repositoryName, workspaceName);
            Session result = sessions.get(key);
            if (result == null) {
                result = RepositoryManager.getSession(request.getRequest(), repositoryName, workspaceName);
                sessions.put(key, result);
            }
            return result;
        }

        Node nodeFor( ResolvedRequest request ) throws RepositoryException {
            Session session = session(request);
            Item item = session.getItem(request.getPath());
            if (item instanceof Property) {
                throw new WebdavException();
            }
            return (Node)item;
        }

        boolean repositoryAndWorkspaceExist( ResolvedRequest request ) throws RepositoryException {
            assert request != null;
            if (request.getRepositoryName() != null) {
                if (request.getWorkspaceName() != null) {
                    try {
                        session(request);
                        return true;
                    } catch (NoSuchWorkspaceException e) {
                        // the workspace does not exist ...
                        return false;
                    }
                }
                // See if the repository exists ...
                return RepositoryManager.getJcrRepositoryNames().contains(request.getRepositoryName());
            }
            // Otherwise, the request doesn't even specify the repository name, so we'll treat this as existing ...
            return true;
        }

        String[] childrenFor( ResolvedRequest request ) throws RepositoryException {
            assert request != null;
            Collection<String> names = null;
            if (request.getRepositoryName() != null) {
                if (request.getWorkspaceName() != null) {
                    try {
                        Session session = session(request);
                        names = namesOfChildren(session.getRootNode());
                    } catch (NoSuchWorkspaceException e) {
                        // the workspace does not exist ...
                        return null;
                    }
                } else {
                    // Get the list of accessible workspaces ...

                    // First look in a session for the same repository ...
                    String repositoryName = request.getRepositoryName();
                    for (Map.Entry<SessionKey, Session> entry : sessions.entrySet()) {
                        SessionKey key = entry.getKey();
                        if (!repositoryName.equals(key.repositoryName)) continue;
                        Session session = entry.getValue();
                        try {
                            return session.getWorkspace().getAccessibleWorkspaceNames();
                        } catch (RepositoryException e) {
                            // try another session ...
                        }
                    }
                    // Didn't have an existing session for that repository, so create one ...
                    Session session = null;
                    try {
                        session = RepositoryManager.getSession(request.getRequest(), repositoryName, null);
                        return session.getWorkspace().getAccessibleWorkspaceNames();
                    } finally {
                        if (session != null) {
                            session.logout(); // always terminate this session!
                        }
                    }
                }
            } else {
                // Get the list of repository names ...
                names = RepositoryManager.getJcrRepositoryNames();
            }
            return names == null ? null : names.toArray(new String[names.size()]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Principal getPrincipal() {
            return principal;
        }

        /**
         * Commits any pending changes to the underlying store.
         */
        void commit() {
            try {
                for (Session session : sessions.values()) {
                    session.save();
                }
            } catch (RepositoryException re) {
                throw new WebdavException(re);
            } finally {
                for (Session session : sessions.values()) {
                    try {
                        session.logout();
                    } catch (Throwable t) {
                        // do nothing
                    }
                }
                sessions.clear();
            }
        }
    }

    protected static final class SessionKey {
        protected final String repositoryName;
        protected final String workspaceName;

        protected SessionKey( String repositoryName,
                              String workspaceName ) {
            this.repositoryName = repositoryName;
            this.workspaceName = workspaceName;
            assert this.repositoryName != null;
            assert this.workspaceName != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return repositoryName.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SessionKey) {
                SessionKey that = (SessionKey)obj;
                return this.repositoryName.equals(that.repositoryName) && this.workspaceName.equals(that.workspaceName);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return repositoryName + "/" + workspaceName;
        }
    }

}
