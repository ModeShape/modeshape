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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.jcr.Item;
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
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.mimetype.aperture.ApertureMimeTypeDetector;
import org.modeshape.web.jcr.RepositoryFactory;

/**
 * Implementation of the {@code IWebdavStore} interface that uses a JCR repository as a backing store.
 * <p>
 * This implementation takes several OSX-specific WebDAV workarounds from the WebDAVImpl class in Drools Guvnor.
 * </p>
 */
public class ModeShapeWebdavStore implements IWebdavStore {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final ThreadLocal<HttpServletRequest> THREAD_LOCAL_REQUEST = new ThreadLocal<HttpServletRequest>();

    /** OSX workaround */
    private static final Map<String, byte[]> OSX_DOUBLE_DATA = Collections.synchronizedMap(new WeakHashMap<String, byte[]>());

    private static final String CONTENT_NODE_NAME = "jcr:content";
    private static final String DATA_PROP_NAME = "jcr:data";
    private static final String CREATED_PROP_NAME = "jcr:created";
    private static final String MODIFIED_PROP_NAME = "jcr:lastModified";
    private static final String ENCODING_PROP_NAME = "jcr:encoding";
    private static final String MIME_TYPE_PROP_NAME = "jcr:mimeType";

    private static final String DEFAULT_CONTENT_PRIMARY_TYPES = "nt:resource, mode:resource";
    private static final String DEFAULT_RESOURCE_PRIMARY_TYPES = "nt:file";
    private static final String DEFAULT_NEW_FOLDER_PRIMARY_TYPE = "nt:folder";
    private static final String DEFAULT_NEW_RESOURCE_PRIMARY_TYPE = "nt:file";
    private static final String DEFAULT_NEW_CONTENT_PRIMARY_TYPE = "mode:resource";

    private final Collection<String> contentPrimaryTypes;
    private final Collection<String> filePrimaryTypes;
    private final String newFolderPrimaryType;
    private final String newResourcePrimaryType;
    private final String newContentPrimaryType;
    private final MimeTypeDetector mimeTypeDetector = new ApertureMimeTypeDetector();
    private final RequestResolver uriResolver;

    public ModeShapeWebdavStore( RequestResolver uriResolver ) {
        this(null, null, null, null, null, uriResolver);
    }

    public ModeShapeWebdavStore( String contentPrimaryTypes,
                                 String filePrimaryTypes,
                                 String newFolderPrimaryType,
                                 String newResourcePrimaryType,
                                 String newContentPrimaryType,
                                 RequestResolver uriResolver ) {
        super();
        this.contentPrimaryTypes = split(contentPrimaryTypes != null ? contentPrimaryTypes : DEFAULT_CONTENT_PRIMARY_TYPES);
        this.filePrimaryTypes = split(filePrimaryTypes != null ? filePrimaryTypes : DEFAULT_RESOURCE_PRIMARY_TYPES);
        this.newFolderPrimaryType = newFolderPrimaryType != null ? newFolderPrimaryType : DEFAULT_NEW_FOLDER_PRIMARY_TYPE;
        this.newResourcePrimaryType = newResourcePrimaryType != null ? newResourcePrimaryType : DEFAULT_NEW_RESOURCE_PRIMARY_TYPE;
        this.newContentPrimaryType = newContentPrimaryType != null ? newContentPrimaryType : DEFAULT_NEW_CONTENT_PRIMARY_TYPE;

        this.uriResolver = uriResolver;
    }

    /**
     * Returns an unmodifiable set containing the elements passed in to this method
     * 
     * @param elements a set of elements; may not be null
     * @return an unmodifiable set containing all of the elements in {@code elements}; never null
     */
    private static final Set<String> setFor( String... elements ) {
        Set<String> set = new HashSet<String>(elements.length);
        set.addAll(Arrays.asList(elements));

        return set;
    }

    /**
     * Splits a comma-delimited string into an unmodifiable set containing the substrings between the commas in the source string.
     * The elements in the set will be {@link String#trim() trimmed}.
     * 
     * @param commaDelimitedString input string; may not be null, but need not contain any commas
     * @return an unmodifiable set whose elements are the trimmed substrings of the source string; never null
     */
    private static final Set<String> split( String commaDelimitedString ) {
        return setFor(commaDelimitedString.split("\\s*,\\s*"));
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
     */
    @Override
    public void createFolder( ITransaction transaction,
                              String folderUri ) {
        int ind = folderUri.lastIndexOf('/');
        String parentUri = folderUri.substring(0, ind + 1);
        String resourceName = folderUri.substring(ind + 1);

        try {
            Node parentNode = nodeFor(transaction, parentUri);
            parentNode.addNode(resourceName, newFolderPrimaryType);

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
            Node parentNode = nodeFor(transaction, parentUri);
            Node resourceNode = parentNode.addNode(resourceName, newResourcePrimaryType);

            Node contentNode = resourceNode.addNode(CONTENT_NODE_NAME, newContentPrimaryType);
            contentNode.setProperty(DATA_PROP_NAME, "");
            contentNode.setProperty(MODIFIED_PROP_NAME, Calendar.getInstance());
            contentNode.setProperty(ENCODING_PROP_NAME, "UTF-8");
            contentNode.setProperty(MIME_TYPE_PROP_NAME, "text/plain");

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
            Node node = nodeFor(transaction, folderUri);

            if (isFile(node) || isContent(node)) {
                return null;
            }

            List<String> children = new LinkedList<String>();
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                children.add(iter.nextNode().getName());
            }

            return children.toArray(EMPTY_STRING_ARRAY);
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceContent( ITransaction transaction,
                                           String resourceUri ) {
        try {
            Node node = nodeFor(transaction, resourceUri);

            if (!isFile(node)) {
                return null;
            }

            if (!node.hasNode(CONTENT_NODE_NAME)) {
                return null;
            }

            return node.getProperty(CONTENT_NODE_NAME + "/" + DATA_PROP_NAME).getStream();

        } catch (RepositoryException re) {
            throw new WebdavException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getResourceLength( ITransaction transaction,
                                   String path ) {
        try {
            Node node = nodeFor(transaction, path);

            if (!isFile(node)) {
                return -1;
            }

            if (!node.hasNode(CONTENT_NODE_NAME)) {
                return -1;
            }

            Property contentProp = node.getProperty(CONTENT_NODE_NAME + "/" + DATA_PROP_NAME);
            long length = contentProp.getLength();
            if (length != -1) return length;

            String data = contentProp.getString();
            return data.length();

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
            Node node = nodeFor(transaction, uri);

            if (isContent(node)) {
                return null;
            }

            if (!isFile(node)) {
                ob.setFolder(true);
                ob.setCreationDate(new Date());
                ob.setLastModified(new Date());
                ob.setResourceLength(0);

            } else if (node.hasNode(CONTENT_NODE_NAME)) {
                Node content = node.getNode(CONTENT_NODE_NAME);

                ob.setFolder(false);
                Date createDate;
                if (node.hasProperty(CREATED_PROP_NAME)) {
                    createDate = node.getProperty(CREATED_PROP_NAME).getDate().getTime();
                } else {
                    createDate = new Date();
                }
                ob.setCreationDate(createDate);
                ob.setLastModified(content.getProperty(MODIFIED_PROP_NAME).getDate().getTime());
                ob.setResourceLength(content.getProperty(DATA_PROP_NAME).getLength());
            } else {
                ob.setNullResource(true);
            }

        } catch (PathNotFoundException pnfe) {
            return null;
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
            nodeFor(transaction, uri).remove();
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
            Node node = nodeFor(transaction, resourceUri);
            if (!isFile(node)) {
                return -1;
            }

            Node contentNode;
            if (node.hasNode(CONTENT_NODE_NAME)) {
                contentNode = node.getNode(CONTENT_NODE_NAME);
            } else {
                contentNode = node.addNode(CONTENT_NODE_NAME, newContentPrimaryType);
            }

            // contentNode.setProperty(MIME_TYPE_PROP_NAME, contentType != null ? contentType : "application/octet-stream");
            contentNode.setProperty(ENCODING_PROP_NAME, characterEncoding != null ? characterEncoding : "UTF-8");
            contentNode.setProperty(DATA_PROP_NAME, content);
            contentNode.setProperty(MODIFIED_PROP_NAME, Calendar.getInstance());

            // Copy the content to the property, THEN re-read the content from the property's stream to avoid discaring the first
            // bytes of the stream
            if (contentType == null) {
                contentType = mimeTypeDetector.mimeTypeOf(resourceName, contentNode.getProperty(DATA_PROP_NAME).getStream());
            }

            return contentNode.getProperty(DATA_PROP_NAME).getLength();
        } catch (RepositoryException re) {
            throw new WebdavException(re);
        } catch (IOException ioe) {
            throw new WebdavException(ioe);
        } catch (RuntimeException t) {
            throw t;
        }
    }

    /**
     * @param node the node to check
     * @return true if {@code node}'s primary type is one of the types in {@link #contentPrimaryTypes}; may not be null
     * @throws RepositoryException if an error occurs checking the node's primary type
     */
    private boolean isContent( Node node ) throws RepositoryException {
        for (String nodeType : contentPrimaryTypes) {
            if (node.isNodeType(nodeType)) return true;
        }

        return false;
    }

    /**
     * @param node the node to check
     * @return true if {@code node}'s primary type is one of the types in {@link #filePrimaryTypes}; may not be null
     * @throws RepositoryException if an error occurs checking the node's primary type
     */
    private boolean isFile( Node node ) throws RepositoryException {
        for (String nodeType : filePrimaryTypes) {
            if (node.isNodeType(nodeType)) return true;
        }

        return false;
    }

    /**
     * @param transaction the active transaction; may not be null
     * @param uri the uri from which the node should be read; never null
     * @return the node at the given uri; never null
     * @throws WebdavException if the uri references a property instead of a node
     * @throws RepositoryException if an error occurs accessing the repository or no {@link Item} exists at the given uri
     */
    private final Node nodeFor( ITransaction transaction,
                                String uri ) throws RepositoryException {
        return ((JcrSessionTransaction)transaction).nodeFor(uri);
    }

    protected final RequestResolver uriResolver() {
        return uriResolver;
    }

    /**
     * Implementation of the {@link ITransaction} interface that uses a {@link Session JCR session} to load and store webdav
     * content. The session also provides support for transactional access to the underlying store.
     */
    class JcrSessionTransaction implements ITransaction {

        private final Principal principal;
        private final Session session;
        private final UriResolver uriResolver;

        @SuppressWarnings( "synthetic-access" )
        JcrSessionTransaction( Principal principal ) {
            super();
            this.principal = principal;

            HttpServletRequest request = THREAD_LOCAL_REQUEST.get();

            if (request == null) {
                throw new WebdavException(WebdavI18n.noStoredRequest.text());
            }

            try {
                ResolvedRequest resolvedRequest = uriResolver().resolve(request);

                this.session = RepositoryFactory.getSession(request,
                                                            resolvedRequest.getRepositoryName(),
                                                            resolvedRequest.getWorkspaceName());
                this.uriResolver = resolvedRequest.getUriResolver();
                assert session != null;
            } catch (RepositoryException re) {
                throw new WebdavException(re);
            }
        }

        /**
         * @return the session associated with this transaction; never null
         */
        Session session() {
            return this.session;
        }

        Node nodeFor( String uri ) throws RepositoryException {
            String resolvedUri = uriResolver.resolve(uri);
            Item item = session.getItem(resolvedUri);
            if (item instanceof Property) {
                throw new WebdavException();
            }

            return (Node)item;

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
                this.session.save();
            } catch (RepositoryException re) {
                throw new WebdavException(re);
            }
        }
    }
}
