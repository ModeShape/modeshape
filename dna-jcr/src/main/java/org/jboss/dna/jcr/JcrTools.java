/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * Utility methods for working with JCR nodes and properties.
 */
public class JcrTools {

    /**
     * Create a map of properties for a given node's nodes 
     * 
     * @param propertyContainer the node and its children that may contain problems. may be null
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null
     * @return the map of loaded properties
     * @throws IllegalArgumentException if the problems argument is null
     */
    public Map<String, Object> loadProperties( Node propertyContainer,
                                               Problems problems ) {
        CheckArg.isNotNull(problems, "problems");
        return loadProperties(propertyContainer, null, problems);
    }

    /**
     * Create a map of properties for a given node's nodes
     * 
     * @param propertyContainer the node and its children that may contain problems. may be null
     * @param properties the existing properties map to append to. may be null
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null
     * @return the map of loaded properties
     * @throws IllegalArgumentException if the problems argument is null
     */
    public Map<String, Object> loadProperties( Node propertyContainer,
                                               Map<String, Object> properties,
                                               Problems problems ) {
        CheckArg.isNotNull(problems, "problems");
        if (properties == null) properties = new HashMap<String, Object>();
        if (propertyContainer != null) {
            try {
                NodeIterator iter = propertyContainer.getNodes();
                while (iter.hasNext()) {
                    Node propertyNode = iter.nextNode();
                    if (propertyNode != null && propertyNode.getPrimaryNodeType().isNodeType("dna:property")) {
                        String propertyName = propertyNode.getName();
                        Object propertyValue = getPropertyValue(propertyNode, "dna:propertyValue", true, problems);
                        properties.put(propertyName, propertyValue);
                    }
                }
            } catch (RepositoryException e) {
                problems.addError(e, RepositoryI18n.errorReadingPropertiesFromContainerNode, getReadable(propertyContainer));
            }
        }

        return properties;
    }

    /**
     * Removes problems attached to a given node
     * 
     * @param parent the parent node
     * @return true if problems existed and are removed else return false.
     * @throws RepositoryException
     * @throws IllegalArgumentException if the parent argument is null
     */
    public boolean removeProblems( Node parent ) throws RepositoryException {
        CheckArg.isNotNull(parent, "parent");
        Node problemsNode = null;
        if (parent.hasNode("dna:problems")) {
            problemsNode = parent.getNode("dna:problems");
            problemsNode.remove();
            return true;
        }
        return false;
    }

    /**
     * Add problems to a specified node.
     * 
     * @param parent the parent node
     * @param problems the list of problems to add to node
     * @return true if problems were added else return false.
     * @throws RepositoryException
     * @throws IllegalArgumentException if the parent or problems argument is null
     */
    public boolean storeProblems( Node parent,
                                  Problems problems ) throws RepositoryException {
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotNull(problems, "problems");
        Node problemsNode = null;
        if (parent.hasNode("dna:problems")) {
            problemsNode = parent.getNode("dna:problems");
            // Delete all problems ...
            removeAllChildren(problemsNode);
        }
        if (problems.isEmpty()) {
            return false;
        }
        if (problemsNode == null) {
            problemsNode = parent.addNode("dna:problems"); // primary type dictated by child definition
        }

        // Add a child for each problem ...
        for (Problem problem : problems) {
            Node problemNode = problemsNode.addNode("problem", "dna:problem");
            // - dna:status (string) mandatory copy
            // < 'ERROR', 'WARNING', 'INFO'
            // - dna:message (string) mandatory copy
            // - dna:code (string) copy
            // - dna:type (string) copy
            // - dna:resource (string) copy
            // - dna:location (string) copy
            // - dna:trace (string) copy
            problemNode.setProperty("dna:status", problem.getStatus().name());
            problemNode.setProperty("dna:message", problem.getMessageString());
            if (problem.getCode() != Problem.DEFAULT_CODE) {
                problemNode.setProperty("dna:code", Integer.toString(problem.getCode()));
            }
            String resource = problem.getResource();
            if (resource != null) {
                problemNode.setProperty("dna:resource", resource);
            }
            String location = problem.getLocation();
            if (location != null) {
                problemNode.setProperty("dna:location", location);
            }
            Throwable t = problem.getThrowable();
            if (t != null) {
                String trace = StringUtil.getStackTrace(t);
                problemNode.setProperty("dna:trace", trace);
            }
        }
        return true;
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
     * Get the string property value for a given node and property name.
     * 
     * @param node the node containing the property. may not be null
     * @param propertyName the name of the property attached to the node. may not be null
     * @param required true if property is required to exist for the given node.
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null 
     * @return the property string
     * @throws IllegalArgumentException if the node, propertyName or problems argument is null
     */
    public String getPropertyAsString( Node node,
                                       String propertyName,
                                       boolean required,
                                       Problems problems ) {
        return getPropertyAsString(node, propertyName, required, null);
    }

    /**
     * Get the string property value for a given node and property name.
     * 
     * @param node the node containing the property. may not be null
     * @param propertyName the name of the property attached to the node. may not be null
     * @param required true if property is required to exist for the given node.
     * @param defaultValue the default property. may be null
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null 
     * @return the property string
     * @throws IllegalArgumentException if the node, propertyName or problems argument is null
     */
    public String getPropertyAsString( Node node,
                                       String propertyName,
                                       boolean required,
                                       String defaultValue,
                                       Problems problems ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(problems, "problems");
        try {
            Property property = node.getProperty(propertyName);
            return property.getString();
        } catch (ValueFormatException e) {
            if (required) {
                problems.addError(e,
                                  RepositoryI18n.requiredPropertyOnNodeWasExpectedToBeStringValue,
                                  propertyName,
                                  getReadable(node));
            } else {
                problems.addError(e,
                                  RepositoryI18n.optionalPropertyOnNodeWasExpectedToBeStringValue,
                                  propertyName,
                                  getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
            if (!required) return defaultValue;
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return null;
    }

    /**
     * Get the property value for specified node and property name on <code>Object</code> format.
     * 
     * @param node the node containing the property. may not be null
     * @param propertyName the name of the property attached to the node. may not be null
     * @param required true if property is required to exist for the given node.
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null
     * @return the property value
     * @throws IllegalArgumentException if the node, propertyName or problems argument is null
     */
    public Object getPropertyValue( Node node,
                                    String propertyName,
                                    boolean required,
                                    Problems problems ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(problems, "problems");
        try {
            Property property = node.getProperty(propertyName);
            switch (property.getType()) {
                case PropertyType.BINARY: {
                    InputStream stream = property.getStream();
                    try {
                        stream = property.getStream();
                        return IoUtil.readBytes(stream);
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // Log ...
                                Logger.getLogger(this.getClass())
                                      .error(e,
                                             RepositoryI18n.errorClosingBinaryStreamForPropertyFromNode,
                                             propertyName,
                                             node.getPath());
                            }
                        }
                    }
                }
                default: {
                    return property.getString();
                }
            }
        } catch (IOException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyOnNodeCouldNotBeRead, propertyName, getReadable(node));
            } else {
                problems.addError(e, RepositoryI18n.optionalPropertyOnNodeCouldNotBeRead, propertyName, getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return null;
    }

    /**
     * Get the property value for a specified node and property name in string array format.
     * 
     * @param node the node containing the property. may not be null
     * @param propertyName the name of the property attached to the node. may not be null
     * @param required true if property is required to exist for the given node.
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null
     * @param defaultValues
     * @return the array of string properties
     * @throws IllegalArgumentException if the node, propertyName or problems argument is null
     */
    public String[] getPropertyAsStringArray( Node node,
                                              String propertyName,
                                              boolean required,
                                              Problems problems,
                                              String... defaultValues ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(problems, "problems");
        String[] result = defaultValues;
        try {
            Property property = node.getProperty(propertyName);
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                result = new String[values.length];
                int i = 0;
                for (Value value : values) {
                    result[i++] = value.getString();
                }
            } else {
                result = new String[] {property.getString()};
            }
        } catch (ValueFormatException e) {
            if (required) {
                problems.addError(e,
                                  RepositoryI18n.requiredPropertyOnNodeWasExpectedToBeStringArrayValue,
                                  propertyName,
                                  getReadable(node));
            } else {
                problems.addError(e,
                                  RepositoryI18n.optionalPropertyOnNodeWasExpectedToBeStringArrayValue,
                                  propertyName,
                                  getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return result;
    }

    /**
     * Get the node under a specified node at a location defined by the specified relative path.
     * 
     * @param node a parent node from which to obtain a node relative to. may not be null
     * @param relativePath the path of the desired node. may not be null
     * @param required true if node is required to exist under the given node.
     * @param problems the list of problems to add to if problems encountered loading properties. may not be null
     * @return the node located relative the the input node
     * @throws IllegalArgumentException if the node, relativePath or problems argument is null
     */
    public Node getNode( Node node,
                         String relativePath,
                         boolean required,
                         Problems problems ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(relativePath, "relativePath");
        CheckArg.isNotNull(problems, "problems");
        Node result = null;
        try {
            result = node.getNode(relativePath);
        } catch (PathNotFoundException e) {
            if (required) problems.addError(e,
                                            RepositoryI18n.requiredNodeDoesNotExistRelativeToNode,
                                            relativePath,
                                            getReadable(node));
        } catch (RepositoryException err) {
            problems.addError(err, RepositoryI18n.errorGettingNodeRelativeToNode, relativePath, getReadable(node));
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
