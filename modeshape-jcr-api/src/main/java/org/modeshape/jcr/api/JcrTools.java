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
package org.modeshape.jcr.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
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
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * Utility methods for working with JCR nodes.
 */
public class JcrTools {

    private boolean debug = false;

    public JcrTools() {
    }

    public JcrTools( boolean debug ) {
        this.debug = debug;
    }

    /**
     * Remove all children from the specified node
     * 
     * @param node
     * @return the number of children removed.
     * @throws RepositoryException
     * @throws IllegalArgumentException if the node argument is null
     */
    public int removeAllChildren( Node node ) throws RepositoryException {
        isNotNull(node, "node");
        int childrenRemoved = 0;
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            child.remove();
            ++childrenRemoved;
        }
        return childrenRemoved;
    }

    public int removeAllChildren( Session session,
                                  String absPath ) throws RepositoryException {
        try {
            Node node = session.getNode(absPath);
            return removeAllChildren(node);
        } catch (PathNotFoundException e) {
            // ignore
        }
        return 0;
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
        isNotNull(node, "node");
        isNotNull(relativePath, "relativePath");
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
     * @throws IllegalArgumentException is any of the parameters are null
     */
    public Node uploadFile( Session session,
                            String path,
                            InputStream stream ) throws RepositoryException, IOException {
        isNotNull(session, "session");
        isNotNull(path, "path");
        isNotNull(stream, "stream");
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
     * @throws IllegalArgumentException is any of the parameters are null
     */
    public Node uploadFile( Session session,
                            String path,
                            URL contentUrl ) throws RepositoryException, IOException {
        isNotNull(session, "session");
        isNotNull(path, "path");
        isNotNull(contentUrl, "contentUrl");

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
     * @throws IllegalArgumentException is any of the parameters are null
     */
    public Node uploadFile( Session session,
                            String path,
                            File file ) throws RepositoryException, IOException {
        isNotNull(session, "session");
        isNotNull(path, "path");
        isNotNull(file, "file");

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

    public void uploadFileAndBlock( Session session,
                                    String resourceFilePath,
                                    String parentPath ) throws RepositoryException, IOException {
        uploadFileAndBlock(session, resourceUrl(resourceFilePath), parentPath);
    }

    public void uploadFileAndBlock( Session session,
                                    String folder,
                                    String fileName,
                                    String parentPath ) throws RepositoryException, IOException {
        uploadFileAndBlock(session, resourceUrl(folder + fileName), parentPath);
    }

    public void uploadFileAndBlock( Session session,
                                    URL url,
                                    String parentPath ) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        if (!parentPath.startsWith("/")) parentPath = "/" + parentPath;
        if (!parentPath.endsWith("/")) parentPath = parentPath + "/";
        final String nodePath = parentPath + filename;

        // Wait a bit before uploading, to make sure everything is ready ...
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (debug) {
            System.out.println("---> Uploading '" + filename + "' into '" + nodePath + "'");
        }

        // Now use the JCR API to upload the file ...
        final CountDownLatch latch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            /**
             * {@inheritDoc}
             * 
             * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
             */
            @Override
            public void onEvent( EventIterator events ) {
                while (events.hasNext()) {
                    try {
                        if (events.nextEvent().getPath().equals(nodePath)) {
                            latch.countDown();
                        }
                    } catch (Throwable e) {
                        latch.countDown();
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        };
        session.getWorkspace()
               .getObservationManager()
               .addEventListener(listener, Event.NODE_ADDED, parentPath, true, null, null, false);
        uploadFile(session, nodePath, url);

        // Save the session ...
        session.save();

        // Now await for the event describing the newly-added file ...
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void uploadFilesAndBlock( String destinationPath,
                                     String... resourcePaths ) throws Exception {
        for (String resourcePath : resourcePaths) {
            uploadFilesAndBlock(resourcePath, destinationPath);
        }
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
        isNotNull(session, "session");
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
        isNotNull(parentNode, "parentNode");
        isNotNull(path, "path");
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

    public boolean isDebug() {
        return debug;
    }

    public void print( Object msg ) {
        if (debug && msg != null) {
            System.out.println(msg.toString());
        }
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
        String nodeLead = lead + createString(' ', (currentDepth - 1) * 2);

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
        boolean referenceable = node.isNodeType("mix:referenceable");
        if (node.getMixinNodeTypes().length != 0) {
            sb.append(" jcr:mixinTypes=[");
            boolean first = true;
            for (NodeType mixin : node.getMixinNodeTypes()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(mixin.getName());
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
            int type = property.getType();
            boolean binary = type == PropertyType.BINARY;
            if (property.isMultiple()) {
                sb.append('[');
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (first) first = false;
                    else sb.append(',');
                    if (binary) {
                        sb.append(value.getBinary());
                    } else {
                        sb.append(getStringValue(value, type));
                    }
                }
                sb.append(']');
            } else {
                Value value = property.getValue();
                if (binary) {
                    sb.append(value.getBinary());
                } else {
                    sb.append(getStringValue(value, type));
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

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param session the session
     * @param jcrSql2 the JCR-SQL2 query
     * @return the results
     * @throws RepositoryException
     */
    public QueryResult printQuery( Session session,
                                   String jcrSql2 ) throws RepositoryException {
        return printQuery(session, jcrSql2, Query.JCR_SQL2, -1, null);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param session the session
     * @param jcrSql2 the JCR-SQL2 query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @param variables the variables for the query
     * @return the results
     * @throws RepositoryException
     */
    public QueryResult printQuery( Session session,
                                   String jcrSql2,
                                   long expectedNumberOfResults,
                                   Variable... variables ) throws RepositoryException {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        for (Variable var : variables) {
            keyValuePairs.put(var.key, var.value);
        }
        return printQuery(session, jcrSql2, Query.JCR_SQL2, expectedNumberOfResults, keyValuePairs);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param session the session
     * @param jcrSql2 the JCR-SQL2 query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @param variables the array of variable maps for the query; all maps will be combined into a single map
     * @return the results
     * @throws RepositoryException
     */
    public QueryResult printQuery( Session session,
                                   String jcrSql2,
                                   long expectedNumberOfResults,
                                   Map<String, String> variables ) throws RepositoryException {
        return printQuery(session, jcrSql2, Query.JCR_SQL2, expectedNumberOfResults, variables);
    }

    public QueryResult printQuery( Session session,
                                   String queryExpression,
                                   String queryLanguage,
                                   long expectedNumberOfResults,
                                   Map<String, String> variables ) throws RepositoryException {
        QueryResult results = null;
        for (int i = 0; i != 10; ++i) {
            Query query = session.getWorkspace().getQueryManager().createQuery(queryExpression, queryLanguage);
            if (variables != null && !variables.isEmpty()) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    String key = entry.getKey();
                    Value value = session.getValueFactory().createValue(entry.getValue());
                    query.bindValue(key, value);
                }
            }
            results = query.execute();
            if (results.getRows().getSize() == expectedNumberOfResults) {
                break;
            }
            // We got a different number of results. It could be that we caught the indexer before it was done indexing
            // the changes, so sleep for a bit and try again ...
            try {
                if (debug) {
                    print("---> Waiting for query: " + queryExpression + (variables != null ? " using " + variables : ""));
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        assert results != null;
        if (expectedNumberOfResults >= 0L && expectedNumberOfResults != results.getRows().getSize()) {
            throw new AssertionError("Expected different number of rows from '" + queryExpression + "': got "
                                     + results.getRows().getSize() + " but expected " + expectedNumberOfResults);
        }
        if (debug) {
            print(queryExpression);
            print(results);
            print("");
        }
        return results;
    }

    public Variable var( String key,
                         String value ) {
        return new Variable(key, value);
    }

    public Map<String, String> vars( String... keyValuePairs ) {
        assert keyValuePairs.length % 2 == 0 : "Must provide an even number of keys and values";
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i != keyValuePairs.length; ++i) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[++i];
            map.put(key, value);
        }
        return map;
    }

    public static class Variable {
        protected final String key;
        protected final String value;

        public Variable( String key,
                         String value ) {
            this.key = key;
            this.value = value;
        }
    }

    protected String getStringValue( Value value,
                                     int type ) throws RepositoryException {
        String result = value.getString();
        if (type == PropertyType.STRING) {
            result = "\"" + result + "\"";
        }
        return result;
    }

    public void registerNodeTypes( Session session,
                                   String pathToCndResourceFile ) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(pathToCndResourceFile);
        if (stream == null) {
            String msg = "\"" + pathToCndResourceFile + "\" does not reference an existing file";
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }
        assert stream != null;
        try {
            NodeTypeManager nodeTypeMgr = (NodeTypeManager)session.getWorkspace().getNodeTypeManager();
            nodeTypeMgr.registerNodeTypes(stream, true);
        } catch (RepositoryException re) {
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not access node type definition files", ioe);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Unknown repository implementation; unable to import CND files", e);
        } finally {
            try {
                stream.close();
            } catch (IOException closer) {
            }
        }
    }

    public void importContent( Session session,
                               String pathToResourceFile ) throws Exception {
        importContent(session, getClass(), pathToResourceFile);
    }

    public void importContent( Session session,
                               String pathToResourceFile,
                               int importBehavior ) throws Exception {
        importContent(session, getClass(), pathToResourceFile, null, importBehavior);
    }

    public void importContent( Session session,
                               String pathToResourceFile,
                               String jcrPathToImportUnder ) throws Exception {
        importContent(session, getClass(), pathToResourceFile, jcrPathToImportUnder);
    }

    public void importContent( Session session,
                               String pathToResourceFile,
                               String jcrPathToImportUnder,
                               int importBehavior ) throws Exception {
        importContent(session, getClass(), pathToResourceFile, jcrPathToImportUnder, importBehavior);
    }

    public static void importContent( Session session,
                                      Class<?> testClass,
                                      String pathToResourceFile ) throws Exception {
        importContent(session, testClass, pathToResourceFile, null);
    }

    public static void importContent( Session session,
                                      Class<?> testClass,
                                      String pathToResourceFile,
                                      String jcrPathToImportUnder ) throws Exception {
        int behavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        importContent(session, testClass, pathToResourceFile, null, behavior);
    }

    public static void importContent( Session session,
                                      Class<?> testClass,
                                      String pathToResourceFile,
                                      String jcrPathToImportUnder,
                                      int importBehavior ) throws Exception {

        // Use a session to load the contents ...
        try {
            InputStream stream = testClass.getClassLoader().getResourceAsStream(pathToResourceFile);
            if (stream == null) {
                String msg = "\"" + pathToResourceFile + "\" does not reference an existing file";
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
            }
            assert stream != null;
            if (jcrPathToImportUnder == null || jcrPathToImportUnder.trim().length() == 0) jcrPathToImportUnder = "/";

            try {
                session.getWorkspace().importXML(jcrPathToImportUnder, stream, importBehavior);
            } finally {
                try {
                    session.save();
                } finally {
                    stream.close();
                    session.logout();
                }
            }
            session.save();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw t;
        } catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    protected URL resourceUrl( String name ) {
        return getClass().getClassLoader().getResource(name);
    }

    public void repeatedlyWithSession( Repository repository,
                                       int times,
                                       Operation operation ) throws Exception {
        for (int i = 0; i != times; ++i) {
            double time = withSession(repository, operation);
            print("Time to execute \"" + operation.getClass().getSimpleName() + "\": " + time + " ms");
        }
    }

    public double withSession( Repository repository,
                               Operation operation ) throws Exception {
        long startTime = System.nanoTime();
        Session session = repository.login();
        try {
            operation.run(session);
        } finally {
            session.logout();
        }
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    public static interface Operation {
        public void run( Session session ) throws Exception;
    }

    public static abstract class BasicOperation implements Operation {
        protected JcrTools tools;

        protected BasicOperation( JcrTools tools ) {
            this.tools = tools;
        }

        protected Node assertNode( Session session,
                                   String path,
                                   String primaryType,
                                   String... mixinTypes ) throws RepositoryException {
            Node node = session.getNode(path);
            assert node.getPrimaryNodeType().getName().equals(primaryType);
            Set<String> expectedMixinTypes = new HashSet<String>(Arrays.asList(mixinTypes));
            Set<String> actualMixinTypes = new HashSet<String>();
            for (NodeType mixin : node.getMixinNodeTypes()) {
                actualMixinTypes.add(mixin.getName());
            }
            assert actualMixinTypes.equals(expectedMixinTypes) : "Mixin types do not match";
            return node;
        }
    }

    public static class BrowseContent extends BasicOperation {
        private String path;

        public BrowseContent( JcrTools tools,
                              String path ) {
            super(tools);
            this.path = path;
        }

        @Override
        public void run( Session s ) throws RepositoryException {
            // Verify the file was imported ...
            Node node = s.getNode(path);
            assert node != null : "Node at " + path + " is null";
        }

    }

    public static class CountNodes extends BasicOperation {
        public long numNonSystemNodes = 0L;

        public CountNodes( JcrTools tools ) {
            super(tools);
        }

        @Override
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:primaryType] FROM [nt:base]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            numNonSystemNodes += query.execute().getRows().getSize();
            if (tools != null) tools.print("  # nodes NOT in '/jcr:system' branch: " + numNonSystemNodes);
        }
    }

    public static class PrintNodes extends BasicOperation {
        public PrintNodes( JcrTools tools ) {
            super(tools);
        }

        @Override
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            if (tools != null) tools.print(query.execute());
        }
    }

    private static String createString( final char charToRepeat,
                                        int numberOfRepeats ) {
        assert numberOfRepeats >= 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numberOfRepeats; ++i) {
            sb.append(charToRepeat);
        }
        return sb.toString();
    }

    private static void isNotNull( Object argument,
                                   String name ) {
        if (argument == null) {
            throw new IllegalArgumentException("The argument \"" + name + "\" may not be null");
        }
    }

}
