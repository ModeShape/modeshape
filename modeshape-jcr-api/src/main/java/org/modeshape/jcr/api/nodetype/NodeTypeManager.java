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
package org.modeshape.jcr.api.nodetype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * An extension of JCR 2.0's {@link javax.jcr.nodetype.NodeTypeManager} interface, with methods to support registering node type
 * definitions from CND and Jackrabbit XML files.
 */
public interface NodeTypeManager extends javax.jcr.nodetype.NodeTypeManager {

    /**
     * Registers or updates the node type definitions per the Compact Node Definition
     * (CND) file given by the supplied stream. This method is used to register 
     * or update a set of node types with mutual dependencies. Returns an iterator
     * over the resulting <code>NodeType</code> objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node
     * types are registered or updated.
     *
     * @param stream the stream containing the node type definitions in CND format
     * @param allowUpdate a boolean stating whether existing node type definitions should be modified/updated
     * @return the registered node types.
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException
     *                                 if a <code>NodeTypeDefinition</code>
     *                                 within the <code>Collection</code> is invalid or if the
     *                                 <code>Collection</code> contains an object of a type other than
     *                                 <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *                                 <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *                                 <code>Collection</code> specifies a node type name that is already
     *                                 registered.
     * @throws UnsupportedRepositoryOperationException
     *                                 if this implementation
     *                                 does not support node type registration.
     * @throws RepositoryException     if another error occurs.
     */
    NodeTypeIterator registerNodeTypes( InputStream stream,
                                        boolean allowUpdate )
        throws IOException, InvalidNodeTypeDefinitionException, NodeTypeExistsException, 
        UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Registers or updates the node type definitions per the Compact Node Definition
     * (CND) file given by the supplied file. This method is used to register 
     * or update a set of node types with mutual dependencies. Returns an iterator
     * over the resulting <code>NodeType</code> objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node
     * types are registered or updated.
     *
     * @param file the file containing the node types
     * @param allowUpdate a boolean stating whether existing node type definitions should be modified/updated
     * @return the registered node types.
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException
     *                                 if a <code>NodeTypeDefinition</code>
     *                                 within the <code>Collection</code> is invalid or if the
     *                                 <code>Collection</code> contains an object of a type other than
     *                                 <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *                                 <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *                                 <code>Collection</code> specifies a node type name that is already
     *                                 registered.
     * @throws UnsupportedRepositoryOperationException
     *                                 if this implementation
     *                                 does not support node type registration.
     * @throws RepositoryException     if another error occurs.
     */
    NodeTypeIterator registerNodeTypes( File file,
                                        boolean allowUpdate ) 
        throws IOException, InvalidNodeTypeDefinitionException, NodeTypeExistsException, 
        UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Registers or updates the node type definitions per the Compact Node Definition
     * (CND) file given by the supplied URL. This method is used to register 
     * or update a set of node types with mutual dependencies. Returns an iterator
     * over the resulting <code>NodeType</code> objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node
     * types are registered or updated.
     *
     * @param url the URL that can be resolved to the file containing the node type definitions in CND format
     * @param allowUpdate a boolean stating whether existing node type definitions should be modified/updated
     * @return the registered node types.
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException
     *                                 if a <code>NodeTypeDefinition</code>
     *                                 within the <code>Collection</code> is invalid or if the
     *                                 <code>Collection</code> contains an object of a type other than
     *                                 <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *                                 <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *                                 <code>Collection</code> specifies a node type name that is already
     *                                 registered.
     * @throws UnsupportedRepositoryOperationException
     *                                 if this implementation
     *                                 does not support node type registration.
     * @throws RepositoryException     if another error occurs.
     */
    NodeTypeIterator registerNodeTypes( URL url,
                                        boolean allowUpdate ) 
        throws IOException, InvalidNodeTypeDefinitionException, NodeTypeExistsException, 
        UnsupportedRepositoryOperationException, RepositoryException;
}
