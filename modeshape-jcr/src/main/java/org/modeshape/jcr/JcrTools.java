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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.util.CheckArg;

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
     * Get the node under a specified node at a location defined by the specified relative path. If node is required, then a problem
     * is created and added to the Problems list.
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
                         boolean required) throws RepositoryException {
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
        return findOrCreateChild( parent, name, null);
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
        return findOrCreateNode( parent, name, nodeType, nodeType);
    }

}
