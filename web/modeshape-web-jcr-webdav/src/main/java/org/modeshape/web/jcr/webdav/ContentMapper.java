package org.modeshape.web.jcr.webdav;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

/**
 * Interface that supports mapping incoming WebDAV requests to create, modify, and access JCR content to node hierarchies and
 * properties on these nodes.
 * 
 * @see DefaultContentMapper
 */
public interface ContentMapper {

    /**
     * Initialize the content mapper based on the provided context
     * 
     * @param context the servlet context for this servlet
     */
    void initialize( ServletContext context );

    /**
     * @param node the node to check; may not be null
     * @return true if {@code node} should be treated as a WebDAV folder
     * @throws RepositoryException if the node cannot be accessed
     */
    boolean isFolder( Node node ) throws RepositoryException;

    /**
     * @param node the node to check; may not be null
     * @return true if {@code node} should be treated as a WebDAV file
     * @throws RepositoryException if the node cannot be accessed
     */
    boolean isFile( Node node ) throws RepositoryException;

    /**
     * @param node the node to check; may not be null
     * @return the contents for the node; null if the node maps to a WebDAV folder
     * @throws RepositoryException if the node cannot be accessed
     * @throws IOException if the content of the node cannot be accessed
     */
    InputStream getResourceContent( Node node ) throws RepositoryException, IOException;

    /**
     * @param node the node to check; may not be null
     * @return the length of the file content for the node; -1 if the node maps to a WebDAV folder
     * @throws RepositoryException if the node cannot be accessed
     * @throws IOException if the content of the node cannot be accessed
     */
    long getResourceLength( Node node ) throws RepositoryException, IOException;

    /**
     * @param parentNode the parent node of the new folder; may not be null
     * @param folderName the name of the folder that is to be created; may not be null
     * @throws RepositoryException if the node cannot be created
     */
    void createFolder( Node parentNode,
                      String folderName ) throws RepositoryException;

    /**
     * @param parentNode the parent node of the new file; may not be null
     * @param fileName the name of the file that is to be created; may not be null
     * @throws RepositoryException if the node cannot be created
     */
    void createFile( Node parentNode,
                    String fileName ) throws RepositoryException;

    /**
     * Creates or modifies the content of a WebDAV file. Implementations may choose whether to store this content directly on the
     * file node or in some other location (e.g., the jcr:content child of the nt:file node that corresponds to the WebDAV file).
     * 
     * @param parentNode node corresponding to the parent folder of the file; may not be null
     * @param resourceName the name of the file to which this content belongs; may not be null
     * @param newContent the content to store; may not be null
     * @param contentType the MIME type of the content; may not be null
     * @param characterEncoding the character encoding of the content; may not be null
     * @return the length of the newly-created content
     * @throws RepositoryException if the node cannot be created or updated
     * @throws IOException if the content of the node cannot be modified or created
     */
    long setContent( Node parentNode,
                     String resourceName,
                     InputStream newContent,
                     String contentType,
                     String characterEncoding ) throws RepositoryException, IOException;

}
