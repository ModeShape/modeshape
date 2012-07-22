package org.modeshape.web.jcr.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.mimetype.aperture.ApertureMimeTypeDetector;

public class DefaultContentMapper implements ContentMapper {

    public static final String INIT_CONTENT_PRIMARY_TYPE_NAMES = "org.modeshape.web.jcr.webdav.CONTENT_PRIMARY_TYPE_NAMES";
    public static final String INIT_RESOURCE_PRIMARY_TYPES_NAMES = "org.modeshape.web.jcr.webdav.RESOURCE_PRIMARY_TYPE_NAMES";
    public static final String INIT_NEW_FOLDER_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_FOLDER_PRIMARY_TYPE_NAME";
    public static final String INIT_NEW_RESOURCE_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_RESOURCE_PRIMARY_TYPE_NAME";
    public static final String INIT_NEW_CONTENT_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_CONTENT_PRIMARY_TYPE_NAME";

    private static final String CONTENT_NODE_NAME = "jcr:content";
    private static final String DATA_PROP_NAME = "jcr:data";
    private static final String MODIFIED_PROP_NAME = "jcr:lastModified";
    private static final String ENCODING_PROP_NAME = "jcr:encoding";
    private static final String MIME_TYPE_PROP_NAME = "jcr:mimeType";

    private static final String DEFAULT_CONTENT_PRIMARY_TYPES = "nt:resource, mode:resource";
    private static final String DEFAULT_RESOURCE_PRIMARY_TYPES = "nt:file";
    private static final String DEFAULT_NEW_FOLDER_PRIMARY_TYPE = "nt:folder";
    private static final String DEFAULT_NEW_RESOURCE_PRIMARY_TYPE = "nt:file";
    private static final String DEFAULT_NEW_CONTENT_PRIMARY_TYPE = "nt:resource";

    private Collection<String> contentPrimaryTypes;
    private Collection<String> filePrimaryTypes;
    private String newFolderPrimaryType;
    private String newResourcePrimaryType;
    private String newContentPrimaryType;
    private MimeTypeDetector mimeTypeDetector = new ApertureMimeTypeDetector();

    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void initialize( ServletContext servletContext ) {

        String contentPrimaryTypes = getParam(servletContext, INIT_CONTENT_PRIMARY_TYPE_NAMES);
        String resourcePrimaryTypes = getParam(servletContext, INIT_RESOURCE_PRIMARY_TYPES_NAMES);
        String newFolderPrimaryType = getParam(servletContext, INIT_NEW_FOLDER_PRIMARY_TYPE_NAME);
        String newResourcePrimaryType = getParam(servletContext, INIT_NEW_RESOURCE_PRIMARY_TYPE_NAME);
        String newContentPrimaryType = getParam(servletContext, INIT_NEW_CONTENT_PRIMARY_TYPE_NAME);

        logger.debug("DefaultContentMapper initial content primary types = " + contentPrimaryTypes);
        logger.debug("DefaultContentMapper initial file primary types = " + filePrimaryTypes);
        logger.debug("DefaultContentMapper initial new folder primary types = " + newFolderPrimaryType);
        logger.debug("DefaultContentMapper initial new resource primary types = " + newResourcePrimaryType);
        logger.debug("DefaultContentMapper initial new content primary types = " + newContentPrimaryType);

        this.contentPrimaryTypes = split(contentPrimaryTypes != null ? contentPrimaryTypes : DEFAULT_CONTENT_PRIMARY_TYPES);
        this.filePrimaryTypes = split(resourcePrimaryTypes != null ? resourcePrimaryTypes : DEFAULT_RESOURCE_PRIMARY_TYPES);
        this.newFolderPrimaryType = newFolderPrimaryType != null ? newFolderPrimaryType : DEFAULT_NEW_FOLDER_PRIMARY_TYPE;
        this.newResourcePrimaryType = newResourcePrimaryType != null ? newResourcePrimaryType : DEFAULT_NEW_RESOURCE_PRIMARY_TYPE;
        this.newContentPrimaryType = newContentPrimaryType != null ? newContentPrimaryType : DEFAULT_NEW_CONTENT_PRIMARY_TYPE;
    }

    protected String getParam( ServletContext servletContext,
                               String name ) {
        return servletContext.getInitParameter(name);
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

    @Override
    public InputStream getResourceContent( Node node ) throws RepositoryException {
        if (!node.hasNode(CONTENT_NODE_NAME)) return null;
        return node.getProperty(CONTENT_NODE_NAME + "/" + DATA_PROP_NAME).getBinary().getStream();
    }

    @Override
    public long getResourceLength( Node node ) throws RepositoryException, IOException {
        if (!node.hasNode(CONTENT_NODE_NAME)){
            return -1;
        }
        return node.getProperty(CONTENT_NODE_NAME + "/" + DATA_PROP_NAME).getLength();
    }

    @Override
    public Date getLastModified( Node node ) throws RepositoryException {
        if (!node.hasNode(CONTENT_NODE_NAME)) return null;

        return node.getProperty(CONTENT_NODE_NAME + "/" + MODIFIED_PROP_NAME).getDate().getTime();
    }

    @Override
    public boolean isFolder( Node node ) throws RepositoryException {
        return !isFile(node) && !isContent(node);
    }

    /**
     * @param node the node to check
     * @return true if {@code node}'s primary type is one of the types in {@link #filePrimaryTypes}; may not be null
     * @throws RepositoryException if an error occurs checking the node's primary type
     */
    @Override
    public boolean isFile( Node node ) throws RepositoryException {
        for (String nodeType : filePrimaryTypes) {
            if (node.isNodeType(nodeType)) return true;
        }

        return false;
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

    @Override
    public void createFile( Node parentNode,
                            String fileName ) throws RepositoryException {
        Node resourceNode = parentNode.addNode(fileName, newResourcePrimaryType);

        Node contentNode = resourceNode.addNode(CONTENT_NODE_NAME, newContentPrimaryType);
        contentNode.setProperty(DATA_PROP_NAME, "");
        contentNode.setProperty(MODIFIED_PROP_NAME, Calendar.getInstance());
        contentNode.setProperty(ENCODING_PROP_NAME, "UTF-8");
        contentNode.setProperty(MIME_TYPE_PROP_NAME, "text/plain");

    }

    @Override
    public void createFolder( Node parentNode,
                              String folderName ) throws RepositoryException {
        parentNode.addNode(folderName, newFolderPrimaryType);

    }

    @Override
    public long setContent( Node parentNode,
                            String resourceName,
                            InputStream newContent,
                            String contentType,
                            String characterEncoding ) throws RepositoryException, IOException {
        Node contentNode;
        if (parentNode.hasNode(CONTENT_NODE_NAME)) {
            contentNode = parentNode.getNode(CONTENT_NODE_NAME);
        } else {
            contentNode = parentNode.addNode(CONTENT_NODE_NAME, newContentPrimaryType);
        }

        // contentNode.setProperty(MIME_TYPE_PROP_NAME, contentType != null ? contentType : "application/octet-stream");
        contentNode.setProperty(ENCODING_PROP_NAME, characterEncoding != null ? characterEncoding : "UTF-8");
        Binary binary = parentNode.getSession().getValueFactory().createBinary(newContent);
        contentNode.setProperty(DATA_PROP_NAME, binary);
        contentNode.setProperty(MODIFIED_PROP_NAME, Calendar.getInstance());

        // Copy the content to the property, THEN re-read the content from the Binary value to avoid discaring the first
        // bytes of the stream
        if (contentType == null) {
            contentType = mimeTypeDetector.mimeTypeOf(resourceName, binary.getStream());
        }

        return contentNode.getProperty(DATA_PROP_NAME).getLength();

    }

}
