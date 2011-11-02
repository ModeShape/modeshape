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
package org.modeshape.jcr.api.nodetype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;

/**
 * An extension of the standard JCR {@link javax.jcr.nodetype.NodeTypeManager} with support for reading CND files.
 */
public interface NodeTypeManager extends javax.jcr.nodetype.NodeTypeManager {

    /**
     * Read the supplied stream containing node type definitions in the standard JCR 2.0 Compact Node Definition (CND) format, and
     * register the node types with this repository.
     * 
     * @param stream the stream containing the node type definitions in CND format
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException if the <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>NodeTypeDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    void registerNodeTypeDefinitions( InputStream stream )
        throws IOException, InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
        RepositoryException;

    /**
     * Read the supplied file containing node type definitions in the standard JCR 2.0 Compact Node Definition (CND) format, and
     * register the node types with this repository.
     * 
     * @param file the file containing the node types
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException if the <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>NodeTypeDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    void registerNodeTypeDefinitions( File file ) throws IOException, RepositoryException;

    /**
     * Read the supplied stream containing node type definitions in the standard JCR 2.0 Compact Node Definition (CND) format, and
     * register the node types with this repository.
     * 
     * @param url the URL that can be resolved to the file containing the node type definitions in CND format
     * @throws IOException if there is a problem reading from the supplied stream
     * @throws InvalidNodeTypeDefinitionException if the <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>NodeTypeDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    void registerNodeTypeDefinitions( URL url ) throws IOException, RepositoryException;
}
