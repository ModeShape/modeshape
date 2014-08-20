/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.web.jcr.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.jcr.Item;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.web.jcr.RepositoryManager;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.exceptions.ObjectNotFoundException;
import org.modeshape.webdav.exceptions.WebdavException;

/**
 * Implementation of the {@code IWebdavStore} interface that uses a JCR repository as a backing store.
 * <p>
 * This implementation takes several OSX-specific WebDAV workarounds from the WebDAVImpl class in Drools Guvnor.
 * </p>
 */
public class ModeShapeWebdavStore implements IWebdavStore {

    /**
     * OS X attempts to create ".DS_Store" files to store a folder's icon positions and background image. We choose not to store
     * these in our implementation, so we ignore requests to create them.
     */
    private static final String DS_STORE_SUFFIX = ".DS_Store";

    private static final ThreadLocal<HttpServletRequest> THREAD_LOCAL_REQUEST = new ThreadLocal<HttpServletRequest>();

    /** OSX workaround */
    private static final Map<String, byte[]> OSX_DOUBLE_DATA = Collections.synchronizedMap(new WeakHashMap<String, byte[]>());

    private static final String CREATED_PROP_NAME = "jcr:created";

    /**
     * List of namespace prefixes that should not be returned in the XML response as a) they cannot appear in the actual elements
     * and b) there are certain clients which can misbehave if they see them in the response (e.g. the Windows Client and the XML
     * prefix)
     */
    protected static final Set<String> EXCLUDED_NAMESPACE_PREFIXES = org.modeshape.common.collection.Collections.unmodifiableSet(NamespaceRegistry.PREFIX_XML,
                                                                                                                                 "xs",
                                                                                                                                 "xsi",
                                                                                                                                 "xmlns");

    private final RequestResolver requestResolver;
    private final ContentMapper contentMapper;

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Creates a new store instance
     *
     * @param requestResolver a {@link RequestResolver} instance, never null
     * @param contentMapper a {@link ContentMapper} instance, never null
     */
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
    static void setRequest( HttpServletRequest request ) {
        THREAD_LOCAL_REQUEST.set(request);
    }

    @Override
    public ITransaction begin( Principal principal ) {
        return new JcrSessionTransaction(principal);
    }

    @Override
    public void commit( ITransaction transaction ) {
        CheckArg.isNotNull(transaction, "transaction");

        assert transaction instanceof JcrSessionTransaction;
        ((JcrSessionTransaction)transaction).commit();
    }

    @Override
    public void rollback( ITransaction transaction ) {
        // No op. By not saving the session, we will let the session expire without committing any changes
    }

    @Override
    public void checkAuthentication( ITransaction transaction ) {
        // No op.
    }

    @Override
    public void destroy() {
    }

    /**
     * @see IWebdavStore#createFolder(org.modeshape.webdav.ITransaction, String)
     */
    @Override
    public void createFolder( ITransaction transaction,
                              String folderUri ) {
        folderUri = removeTrailingSlash(folderUri);
        int ind = folderUri.lastIndexOf('/');
        String parentUri = folderUri.substring(0, ind + 1);
        String resourceName = folderUri.substring(ind + 1);

        try {
            logger.debug("WebDAV create folder at: " + parentUri);
            ResolvedRequest resolvedParent = resolveRequest(parentUri);
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
            throw translate(re);
        }
    }

    @Override
    public void createResource( ITransaction transaction,
                                String resourceUri ) {
        resourceUri = removeTrailingSlash(resourceUri);

        // Mac OS X workaround from Drools Guvnor
        if (resourceUri.endsWith(DS_STORE_SUFFIX)) {
            return;
        }

        int ind = resourceUri.lastIndexOf('/');
        String parentUri = resourceUri.substring(0, ind + 1);
        String resourceName = resourceUri.substring(ind + 1);

        // Mac OS X workaround from Drools Guvnor
        if (resourceName.startsWith("._")) {
            OSX_DOUBLE_DATA.put(resourceUri, null);
            return;
        }

        try {
            ResolvedRequest resolvedParent = resolveRequest(parentUri);
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
            throw translate(re);
        }

    }

    private String removeTrailingSlash( String uri ) {
        if (!StringUtil.isBlank(uri) && uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    @Override
    public String[] getChildrenNames( ITransaction transaction,
                                      String folderUri ) {
        try {
            logger.trace("WebDAV getChildrenNames(txn,\"" + folderUri + "\")");
            ResolvedRequest resolved = resolveRequest(folderUri);
            logger.trace("WebDAV -> resolves to: " + resolved);
            if (resolved.getPath() == null) {
                // It does not resolve to the path of a node, so see if the repository/workspace exist ...
                return childrenFor(transaction, resolved);
            }

            Node node = nodeFor(transaction, resolved); // throws exception if not found
            logger.trace("WebDAV -> node: " + node);

            if (!isFolder(node)) {
                return null; // no children
            }

            List<String> children = namesOfChildren(node);
            logger.trace("WebDAV -> children: " + children);
            return children.toArray(new String[children.size()]);
        } catch (RepositoryException re) {
            throw translate(re);
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

    @Override
    public InputStream getResourceContent( ITransaction transaction,
                                           String resourceUri ) {
        try {
            ResolvedRequest resolved = resolveRequest(resourceUri);
            if (resolved.getPath() == null) {
                // Not a node, so there's no content ...
                return null;
            }
            Node node = nodeFor(transaction, resolved); // throws exception if not found
            if (!isFile(node)) {
                return null;
            }
            return contentMapper.getResourceContent(node);

        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RepositoryException re) {
            throw translate(re);
        }
    }

    @Override
    public long getResourceLength( ITransaction transaction,
                                   String resourceUri ) {
        try {
            ResolvedRequest resolved = resolveRequest(resourceUri);
            if (resolved.getPath() == null) {
                // Not a node, so there's no length ...
                return -1;
            }
            Node node = nodeFor(transaction, resolved); // throws exception if not found

            return contentMapper.getResourceLength(node);
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RepositoryException re) {
            throw translate(re);
        }
    }

    @Override
    public StoredObject getStoredObject( ITransaction transaction,
                                         String uri ) {
        if (uri.length() == 0) {
            uri = "/";
        }

        StoredObject ob = new StoredObject();
        try {
            logger.trace("WebDAV getStoredObject at \"" + uri + "\"");
            ResolvedRequest resolved = resolveRequest(uri);
            logger.debug("WebDAV getStoredObject at \"" + uri + "\" resolved to \"" + resolved + "\"");
            String path = resolved.getPath();
            if (path == null) {
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

            int ind = path.lastIndexOf('/');
            String resourceName = path.substring(ind + 1);
            if (resourceName.startsWith("._")) {
                // OS-X uses these hidden files ...
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
            throw translate(re);
        }
        return ob;
    }

    @Override
    public void removeObject( ITransaction transaction,
                              String uri ) {
        // Mac OS X workaround from Drools Guvnor
        String resourceName = resourceNameFromResourcePath(uri);
        if (resourceName.startsWith("._")) {
            OSX_DOUBLE_DATA.put(uri, null);
            return;
        }

        try {
            ResolvedRequest resolved = resolveRequest(uri);
            if (resolved.getPath() != null) {
                // It does resolve to the path of a node, so try to find the node and remove it ...
                Node node = nodeFor(transaction, resolved);
                node.remove();
            }
            // Otherwise just return silently
        } catch (PathNotFoundException pnfe) {
            // Return silently
        } catch (RepositoryException re) {
            throw translate(re);
        }
    }

    protected String resourceNameFromResourcePath( String path ) {
        int ind = path.lastIndexOf('/');
        return path.substring(ind + 1);
    }

    @Override
    public long setResourceContent( ITransaction transaction,
                                    String resourceUri,
                                    InputStream content,
                                    String contentType,
                                    String characterEncoding ) {
        // Mac OS X workaround from Drools Guvnor
        if (shouldIgnoreResource(resourceUri)) {
            return 0;
        }

        // Mac OS X workaround from Drools Guvnor
        String resourceName = resourceNameFromResourcePath(resourceUri);
        if (resourceName.startsWith("._")) {
            try {
                OSX_DOUBLE_DATA.put(resourceUri, IoUtil.readBytes(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }

        try {
            ResolvedRequest resolved = resolveRequest(resourceUri);
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
            throw translate(re);
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        }
    }

    private boolean shouldIgnoreResource( String resourceUri ) {
        return resourceUri.endsWith(".DS_Store");
    }

    @Override
    public Map<String, String> setCustomProperties( ITransaction transaction,
                                                    String resourceUri,
                                                    Map<String, Object> propertiesToSet,
                                                    List<String> propertiesToRemove ) {
        resourceUri = removeTrailingSlash(resourceUri);
        if (shouldIgnoreResource(resourceUri)) {
            logger().debug("Resource {0} ignored.", resourceUri);
            return null;
        }
        try {
            ResolvedRequest resolvedRequest = resolveRequest(resourceUri);
            if (resolvedRequest.getPath() == null) {
                throw new ObjectNotFoundException("The resource at path " + resourceUri + " does not represent a valid JCR node");
            }
            Node node = nodeFor(transaction, resolvedRequest);

            Map<String, String> response = new LinkedHashMap<String, String>();

            // update properties
            for (String propertyName : propertiesToSet.keySet()) {
                String jcrPropertyName = jcrPropertyName(transaction, resolvedRequest, propertyName);
                Object value = propertiesToSet.get(propertyName);
                if (value instanceof Collection) {
                    Collection<?> collection = (Collection<?>)value;
                    String[] jcrValue = new String[collection.size()];
                    int i = 0;
                    for (Object collectionObject : collection) {
                        jcrValue[i++] = collectionObject.toString();
                    }
                    try {
                        node.setProperty(jcrPropertyName, jcrValue);
                    } catch (RepositoryException e) {
                        response.put(propertyName, e.getMessage());
                        markForRollback(transaction, resolvedRequest);
                        return response;
                    }
                } else {
                    try {
                        node.setProperty(jcrPropertyName, value.toString());
                    } catch (RepositoryException e) {
                        response.put(propertyName, e.getMessage());
                        markForRollback(transaction, resolvedRequest);
                        return response;
                    }
                }
            }

            // remove properties
            for (String propertyName : propertiesToRemove) {
                String jcrPropertyName = jcrPropertyName(transaction, resolvedRequest, propertyName);
                try {
                    node.getProperty(jcrPropertyName).remove();
                } catch (RepositoryException e) {
                    response.put(propertyName, e.getMessage());
                    markForRollback(transaction, resolvedRequest);
                    return response;
                }
            }

            return response;
        } catch (RepositoryException e) {
            throw translate(e);
        }
    }

    private String jcrPropertyName( ITransaction transaction,
                                    ResolvedRequest resolvedRequest,
                                    String webdavPropertyName ) throws RepositoryException {
        String[] parts = webdavPropertyName.split("\\:");
        if (parts.length == 0) {
            return webdavPropertyName;
        }
        List<String> parsedParts = new ArrayList<String>();
        for (String part : parts) {
            if (!StringUtil.isBlank(part) && !part.equalsIgnoreCase(":")) {
                parsedParts.add(part);
            }
        }
        if (parsedParts.isEmpty()) {
            return webdavPropertyName;
        }
        // use the last part as the local name of the jcr property
        String localName = parsedParts.remove(parsedParts.size() - 1);
        // try to take each part from the webdav property name to see if it matches a session prefix (e.g. jcr:)
        Session session = sessionFor(transaction, resolvedRequest);
        List<String> namespacePrefixes = Arrays.asList(session.getNamespacePrefixes());
        for (int i = parsedParts.size() - 1; i >= 0; i--) {
            String prefix = parsedParts.get(i);
            if (namespacePrefixes.contains(prefix)) {
                return prefix + ":" + localName;
            }
        }
        // we don't have a jcr-recognized prefix, so we'll just send the plain property
        return localName;
    }

    @Override
    public Map<String, Object> getCustomProperties( ITransaction transaction,
                                                    String resourceUri ) {
        resourceUri = removeTrailingSlash(resourceUri);
        if (shouldIgnoreResource(resourceUri)) {
            return Collections.emptyMap();
        }
        try {
            ResolvedRequest resolvedRequest = resolveRequest(resourceUri);
            if (resolvedRequest.getPath() == null) {
                return Collections.emptyMap();
            }
            Node node = nodeFor(transaction, resolvedRequest);
            Map<String, Object> response = new LinkedHashMap<String, Object>();

            PropertyIterator propertyIterator = node.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                if (property.isMultiple()) {
                    logger().debug(WebdavI18n.warnMultiValuedProperty.text(property.getPath()));
                    continue;
                }
                response.put(property.getName(), property.getString());
            }
            return response;
        } catch (RepositoryException e) {
            throw translate(e);
        }
    }

    @Override
    public Map<String, String> getCustomNamespaces( ITransaction transaction,
                                                    String resourceUri ) {
        resourceUri = removeTrailingSlash(resourceUri);
        try {
            ResolvedRequest resolvedRequest = resolveRequest(resourceUri);
            if (resolvedRequest.getPath() == null) {
                return Collections.emptyMap();
            }
            return ((JcrSessionTransaction)transaction).namespacesFor(resolvedRequest);
        } catch (RepositoryException e) {
            throw translate(e);
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
     * @param uri the URI from the request
     * @return the resolved information; never null
     * @throws WebdavException if the URI is invalid or otherwise not acceptable
     */
    private ResolvedRequest resolveRequest( String uri ) throws WebdavException {
        HttpServletRequest request = THREAD_LOCAL_REQUEST.get();
        return requestResolver.resolve(request, uri);
    }

    /**
     * Get the node that corresponds to the resolved request, using the supplied active transaction.
     *
     * @param transaction the active transaction; may not be null
     * @param request the resolved request; may not be null and must contain a repository name and workspace name
     * @return the node; never null
     * @throws RepositoryException if the node does not exist, or if there is another problem obtaining the node
     */
    private Node nodeFor( ITransaction transaction,
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
    private boolean repositoryAndWorkspaceExist( ITransaction transaction,
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
    private String[] childrenFor( ITransaction transaction,
                                  ResolvedRequest request ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).childrenFor(request);
    }

    private void markForRollback( ITransaction transaction,
                                  ResolvedRequest request ) {
        ((JcrSessionTransaction)transaction).markForRollback(request);
    }

    private Session sessionFor( ITransaction transaction,
                                ResolvedRequest request ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).session(request);
    }

    protected final Logger logger() {
        return logger;
    }

    /**
     * Implementation of the {@link ITransaction} interface that uses a {@link Session JCR session} to load and store webdav
     * content. The session also provides support for transactional access to the underlying store.
     */
    class JcrSessionTransaction implements ITransaction {

        private final Map<SessionKey, Session> sessions = new HashMap<SessionKey, Session>();
        private final Set<SessionKey> sessionsMarkedForRollback = new HashSet<SessionKey>();
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
                try {
                    result = RepositoryManager.getSession(request.getRequest(), repositoryName, workspaceName);
                } catch (RepositoryException e) {
                    logger().warn(WebdavI18n.cannotGetRepositorySession, repositoryName, e.getMessage());
                    throw translate(e);
                }
                sessions.put(key, result);
            }
            return result;
        }

        Node nodeFor( ResolvedRequest request ) throws RepositoryException {
            Session session = session(request);
            Item item = session.getItem(request.getPath());
            if (item instanceof Property) {
                throw new WebdavException(WebdavI18n.errorPropertyPath.text(item.getPath()));
            }
            return (Node)item;
        }

        void markForRollback( ResolvedRequest request ) {
            sessionsMarkedForRollback.add(new SessionKey(request.getRepositoryName(), request.getWorkspaceName()));
        }

        Map<String, String> namespacesFor( ResolvedRequest request ) throws RepositoryException {
            Session session = session(request);
            Map<String, String> namespaces = new HashMap<String, String>();
            for (String namespacePrefix : session.getNamespacePrefixes()) {
                if (StringUtil.isBlank(namespacePrefix) || EXCLUDED_NAMESPACE_PREFIXES.contains(namespacePrefix.toLowerCase())) {
                    continue;
                }
                String namespaceURI = session.getNamespaceURI(namespacePrefix);
                namespaces.put(namespaceURI, namespacePrefix);
            }
            return namespaces;
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
                        if (!repositoryName.equals(key.repositoryName)) {
                            continue;
                        }
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
                    } catch (RepositoryException e) {
                        logger().warn(WebdavI18n.cannotGetRepositorySession, repositoryName, e.getMessage());
                        throw translate(e);
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

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        /**
         * Commits any pending changes to the underlying store.
         */
        void commit() {
            try {
                for (Map.Entry<SessionKey, Session> entry : sessions.entrySet()) {
                    if (!sessionsMarkedForRollback.contains(entry.getKey())) {
                        entry.getValue().save();
                    }
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
                sessionsMarkedForRollback.clear();
            }
        }
    }

    /**
     * Converts the JCR Exceptions to WebDAV ones.
     *
     * @param exception the repository exception
     * @return the WebDAV exception
     */
    protected WebdavException translate( RepositoryException exception ) {
        return ModeShapeWebdavServlet.translateError(exception);
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

        @Override
        public int hashCode() {
            return repositoryName.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SessionKey) {
                SessionKey that = (SessionKey)obj;
                return this.repositoryName.equals(that.repositoryName) && this.workspaceName.equals(that.workspaceName);
            }
            return false;
        }

        @Override
        public String toString() {
            return repositoryName + "/" + workspaceName;
        }
    }

}
