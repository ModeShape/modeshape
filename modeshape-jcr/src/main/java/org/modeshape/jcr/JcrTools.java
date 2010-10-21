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
package org.modeshape.jcr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.mimetype.MimeTypeDetector;

/**
 * Utility methods for working with JCR nodes.
 */
public class JcrTools {

    /**
     * Remove all children from the specified node
     * 
     * @param node
     * @return the number of children removed.
     * @throws RepositoryException
     * @throws IllegalArgumentException if the node argument is null
     */
    public int removeAllChildren( Node node ) throws RepositoryException {
        CheckArg.isNotNull(node, "node");
        int childrenRemoved = 0;
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            child.remove();
            ++childrenRemoved;
        }
        return childrenRemoved;
    }

    /**
     * Get the node under a specified node at a location defined by the specified relative path. If node is required, then a
     * problem is created and added to the Problems list.
     * 
     * @param node a parent node from which to obtain a node relative to. may not be null
     * @param relativePath the path of the desired node. may not be null
     * @param required true if node is required to exist under the given node.
     * @return the node located relative the the input node
     * @throws RepositoryException
     * @throws IllegalArgumentException if the node, relativePath or problems argument is null
     */
    public Node getNode( Node node,
                         String relativePath,
                         boolean required ) throws RepositoryException {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(relativePath, "relativePath");
        Node result = null;
        try {
            result = node.getNode(relativePath);
        } catch (PathNotFoundException e) {
            if (required) {
                throw e;
            }
        }

        return result;
    }

    /**
     * Get the readable string form for a specified node.
     * 
     * @param node the node to obtain the readable string form. may be null
     * @return the readable string form for a specified node.
     */
    public String getReadable( Node node ) {
        if (node == null) return "";
        try {
            return node.getPath();
        } catch (RepositoryException err) {
            return node.toString();
        }
    }

    public MimeTypeDetector mimeTypeDetector( Session session ) {
        Repository repository = session.getRepository();
        ExecutionContext context = null;
        if (repository instanceof JcrRepository) {
            JcrRepository jcrRepository = (JcrRepository)repository;
            context = jcrRepository.getExecutionContext();
        } else {
            context = new ExecutionContext();
        }
        return context.getMimeTypeDetector();
    }

    /**
     * Detect the MIME type for the named filename.
     * 
     * @param session the JCR session
     * @param fileName the file name
     * @return the MIME type
     */
    public String detectMimeType( Session session,
                                  String fileName ) {
        try {
            return mimeTypeDetector(session).mimeTypeOf(fileName, null);
        } catch (IOException e) {
            // We're not reading the content, so wrap this as it is unexpected ...
            throw new RuntimeException(e);
        }
    }

    /**
     * Detect the MIME type for the named filename.
     * 
     * @param session the JCR session
     * @param file the file
     * @param useContent true if the content of the file at the URL should also be used to determine the MIME type, or false if
     *        only the URL itself should be used
     * @return the MIME type
     * @throws IOException if there is an error reading the file
     */
    public String detectMimeType( Session session,
                                  File file,
                                  boolean useContent ) throws IOException {
        InputStream stream = null;
        boolean error = false;
        try {
            if (useContent) stream = new BufferedInputStream(new FileInputStream(file));
            return mimeTypeDetector(session).mimeTypeOf(file.getName(), stream);
        } catch (IOException e) {
            error = true;
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Detect the MIME type for the named filename.
     * 
     * @param session the JCR session
     * @param url the URL
     * @param useContent true if the content of the file at the URL should also be used to determine the MIME type, or false if
     *        only the URL itself should be used
     * @return the MIME type
     * @throws IOException if there is an error reading the file
     */
    public String detectMimeType( Session session,
                                  URL url,
                                  boolean useContent ) throws IOException {
        InputStream stream = null;
        boolean error = false;
        try {
            if (useContent) stream = url.openStream();
            return mimeTypeDetector(session).mimeTypeOf(url.getPath(), stream);
        } catch (IOException e) {
            error = true;
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Upload the content in the supplied stream into the repository at the defined path, using the given session. This method
     * will create a 'nt:file' node at the supplied path, and any non-existant ancestors with nodes of type 'nt:folder'. As
     * defined by the JCR specification, the binary content (and other properties) will be placed on a child of the 'nt:file' node
     * named 'jcr:content' with a node type of 'nt:resource'.
     * <p>
     * This method always closes the supplied stream.
     * </p>
     * 
     * @param session the JCR session
     * @param path the path to the file
     * @param stream the stream containing the content to be uploaded
     * @return the newly created 'nt:file' node
     * @throws RepositoryException if there is a problem uploading the file
     * @throws IOException if there is a problem using the stream
     */
    public Node uploadFile( Session session,
                            String path,
                            InputStream stream ) throws RepositoryException, IOException {
        CheckArg.isNotNull(session, "session");
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(stream, "stream");
        Node fileNode = null;
        boolean error = false;
        try {
            // Create an 'nt:file' node at the supplied path, creating any missing intermediate nodes of type 'nt:folder' ...
            fileNode = findOrCreateNode(session.getRootNode(), path, "nt:folder", "nt:file");

            // Upload the file to that node ...
            Node contentNode = findOrCreateChild(fileNode, "jcr:content", "nt:resource");
            Binary binary = session.getValueFactory().createBinary(stream);
            contentNode.setProperty("jcr:data", binary);
        } catch (RepositoryException e) {
            error = true;
            throw e;
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            try {
                stream.close();
            } catch (RuntimeException e) {
                if (!error) throw e; // don't override any exception thrown in the block above
            }
        }
        return fileNode;
    }

    /**
     * Upload the content at the supplied URL into the repository at the defined path, using the given session. This method will
     * create a 'nt:file' node at the supplied path, and any non-existant ancestors with nodes of type 'nt:folder'. As defined by
     * the JCR specification, the binary content (and other properties) will be placed on a child of the 'nt:file' node named
     * 'jcr:content' with a node type of 'nt:resource'.
     * 
     * @param session the JCR session
     * @param path the path to the file
     * @param contentUrl the URL where the content can be found
     * @return the newly created 'nt:file' node
     * @throws RepositoryException if there is a problem uploading the file
     * @throws IOException if there is a problem using the stream
     */
    public Node uploadFile( Session session,
                            String path,
                            URL contentUrl ) throws RepositoryException, IOException {
        CheckArg.isNotNull(session, "session");
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(contentUrl, "contentUrl");

        // Open the URL's stream first ...
        InputStream stream = contentUrl.openStream();
        return uploadFile(session, path, stream);
    }

    /**
     * Upload the content in the supplied file into the repository at the defined path, using the given session. This method will
     * create a 'nt:file' node at the supplied path, and any non-existant ancestors with nodes of type 'nt:folder'. As defined by
     * the JCR specification, the binary content (and other properties) will be placed on a child of the 'nt:file' node named
     * 'jcr:content' with a node type of 'nt:resource'.
     * 
     * @param session the JCR session
     * @param path the path to the file
     * @param file the existing and readable file to be uploaded
     * @return the newly created 'nt:file' node
     * @throws RepositoryException if there is a problem uploading the file
     * @throws IOException if there is a problem using the stream
     * @throws IllegalArgumentException if the file does not exist or is not readable
     */
    public Node uploadFile( Session session,
                            String path,
                            File file ) throws RepositoryException, IOException {
        CheckArg.isNotNull(session, "session");
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(file, "file");

        if (!file.exists()) {
            throw new IllegalArgumentException("The file \"" + file.getCanonicalPath() + "\" does not exist");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("The file \"" + file.getCanonicalPath() + "\" is not readable");
        }
        // Determine the 'lastModified' timestamp ...
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(file.lastModified());

        // Open the URL's stream first ...
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        return uploadFile(session, path, stream);
    }

    /**
     * Get or create a node at the specified path.
     * 
     * @param session the JCR session. may not be null
     * @param path the path of the desired node to be found or created. may not be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if either the session or path argument is null
     */
    public Node findOrCreateNode( Session session,
                                  String path ) throws RepositoryException {
        return findOrCreateNode(session, path, null, null);
    }

    /**
     * Get or create a node at the specified path and node type.
     * 
     * @param session the JCR session. may not be null
     * @param path the path of the desired node to be found or created. may not be null
     * @param nodeType the node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if either the session or path argument is null
     */
    public Node findOrCreateNode( Session session,
                                  String path,
                                  String nodeType ) throws RepositoryException {
        return findOrCreateNode(session, path, nodeType, nodeType);
    }

    /**
     * Get or create a node at the specified path.
     * 
     * @param session the JCR session. may not be null
     * @param path the path of the desired node to be found or created. may not be null
     * @param defaultNodeType the default node type. may be null
     * @param finalNodeType the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if either the session or path argument is null
     */
    public Node findOrCreateNode( Session session,
                                  String path,
                                  String defaultNodeType,
                                  String finalNodeType ) throws RepositoryException {
        CheckArg.isNotNull(session, "session");
        Node root = session.getRootNode();
        return findOrCreateNode(root, path, defaultNodeType, finalNodeType);
    }

    /**
     * Get or create a node at the specified path.
     * 
     * @param parentNode the parent node. may not be null
     * @param path the path of the desired child node. may not be null
     * @param defaultNodeType the default node type. may be null
     * @param finalNodeType the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if either the parentNode or path argument is null
     */
    public Node findOrCreateNode( Node parentNode,
                                  String path,
                                  String defaultNodeType,
                                  String finalNodeType ) throws RepositoryException {
        CheckArg.isNotNull(parentNode, "parentNode");
        CheckArg.isNotNull(path, "path");
        // Remove leading and trailing slashes ...
        String relPath = path.replaceAll("^/+", "").replaceAll("/+$", "");

        // Look for the node first ...
        try {
            return parentNode.getNode(relPath);
        } catch (PathNotFoundException e) {
            // continue
        }
        // Create the node, which has to be done segment by segment ...
        String[] pathSegments = relPath.split("/");
        Node node = parentNode;
        for (int i = 0, len = pathSegments.length; i != len; ++i) {
            String pathSegment = pathSegments[i];
            pathSegment = pathSegment.trim();
            if (pathSegment.length() == 0) continue;
            if (node.hasNode(pathSegment)) {
                // Find the existing node ...
                node = node.getNode(pathSegment);
            } else {
                // Make sure there is no index on the final segment ...
                String pathSegmentWithNoIndex = pathSegment.replaceAll("(\\[\\d+\\])+$", "");
                // Create the node ...
                String nodeType = defaultNodeType;
                if (i == len - 1 && finalNodeType != null) nodeType = finalNodeType;
                if (nodeType != null) {
                    node = node.addNode(pathSegmentWithNoIndex, nodeType);
                } else {
                    node = node.addNode(pathSegmentWithNoIndex);
                }
            }
        }
        return node;
    }

    /**
     * Get or create a node with the specified node under the specified parent node.
     * 
     * @param parent the parent node. may not be null
     * @param name the name of the child node. may not be null
     * @return the existing or newly created child node
     * @throws RepositoryException
     * @throws IllegalArgumentException if either the parent or name argument is null
     */
    public Node findOrCreateChild( Node parent,
                                   String name ) throws RepositoryException {
        return findOrCreateChild(parent, name, null);
    }

    /**
     * Get or create a node with the specified node and node type under the specified parent node.
     * 
     * @param parent the parent node. may not be null
     * @param name the name of the child node. may not be null
     * @param nodeType the node type. may be null
     * @return the existing or newly created child node
     * @throws RepositoryException
     */
    public Node findOrCreateChild( Node parent,
                                   String name,
                                   String nodeType ) throws RepositoryException {
        return findOrCreateNode(parent, name, nodeType, nodeType);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @throws RepositoryException
     */
    public void printSubgraph( Node node ) throws RepositoryException {
        printSubgraph(node, Integer.MAX_VALUE);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param maxDepth the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    public void printSubgraph( Node node,
                               int maxDepth ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), maxDepth);
    }

    /**
     * Print this node and its properties to System.out if printing is enabled.
     * 
     * @param node the node to be printed
     * @throws RepositoryException
     */
    public void printNode( Node node ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), 1);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param lead the string that each line should begin with; may be null if there is no such string
     * @param depthOfSubgraph the depth of this subgraph's root node
     * @param maxDepthOfSubgraph the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    public void printSubgraph( Node node,
                               String lead,
                               int depthOfSubgraph,
                               int maxDepthOfSubgraph ) throws RepositoryException {
        int currentDepth = node.getDepth() - depthOfSubgraph + 1;
        if (currentDepth > maxDepthOfSubgraph) return;
        if (lead == null) lead = "";
        String nodeLead = lead + StringUtil.createString(' ', (currentDepth - 1) * 2);

        StringBuilder sb = new StringBuilder();
        sb.append(nodeLead);
        if (node.getDepth() == 0) {
            sb.append("/");
        } else {
            sb.append(node.getName());
            if (node.getIndex() != 1) {
                sb.append('[').append(node.getIndex()).append(']');
            }
        }
        sb.append(" jcr:primaryType=" + node.getPrimaryNodeType().getName());
        boolean referenceable = false;
        if (node.getMixinNodeTypes().length != 0) {
            sb.append(" jcr:mixinTypes=[");
            boolean first = true;
            for (NodeType mixin : node.getMixinNodeTypes()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(mixin.getName());
                if (mixin.getName().equals("mix:referenceable")) referenceable = true;
            }
            sb.append(']');
        }
        if (referenceable) {
            sb.append(" jcr:uuid=" + node.getIdentifier());
        }
        System.out.println(sb);

        List<String> propertyNames = new LinkedList<String>();
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            String name = property.getName();
            if (name.equals("jcr:primaryType") || name.equals("jcr:mixinTypes") || name.equals("jcr:uuid")) continue;
            propertyNames.add(property.getName());
        }
        Collections.sort(propertyNames);
        for (String propertyName : propertyNames) {
            Property property = node.getProperty(propertyName);
            sb = new StringBuilder();
            sb.append(nodeLead).append("  - ").append(propertyName).append('=');
            boolean binary = property.getType() == PropertyType.BINARY;
            if (property.isMultiple()) {
                sb.append('[');
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (first) first = false;
                    else sb.append(',');
                    if (binary) {
                        sb.append(value.getBinary());
                    } else {
                        sb.append(value.getString());
                    }
                }
                sb.append(']');
            } else {
                Value value = property.getValue();
                if (binary) {
                    sb.append(value.getBinary());
                } else {
                    sb.append(value.getString());
                }
            }
            System.out.println(sb);
        }

        if (currentDepth < maxDepthOfSubgraph) {
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                printSubgraph(child, lead, depthOfSubgraph, maxDepthOfSubgraph);
            }
        }
    }

}
