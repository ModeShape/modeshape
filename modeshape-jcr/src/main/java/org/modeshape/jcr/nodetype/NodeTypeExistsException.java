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
package org.modeshape.jcr.nodetype;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;

/**
 * An exception that captures the error condition that a referenced node type already exists.
 */
@Immutable
public class NodeTypeExistsException extends javax.jcr.nodetype.NodeTypeExistsException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private Name nodeType;

    /**
     * @param nodeType the name of the existing node type
     */
    public NodeTypeExistsException( Name nodeType ) {
        super();
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param message
     * @param rootCause
     */
    public NodeTypeExistsException( Name nodeType,
                                    String message,
                                    Throwable rootCause ) {
        super(message, rootCause);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param message
     */
    public NodeTypeExistsException( Name nodeType,
                                    String message ) {
        super(message);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param rootCause
     */
    public NodeTypeExistsException( Name nodeType,
                                    Throwable rootCause ) {
        super(rootCause);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * Get the name of the existing node type.
     * 
     * @return the existing node type name
     */
    public Name getNodeType() {
        return nodeType;
    }

}
