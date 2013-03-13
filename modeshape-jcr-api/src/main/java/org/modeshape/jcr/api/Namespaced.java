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

import javax.jcr.RepositoryException;

/**
 * An orthogonal interface that defines methods to obtain the local part and namespace URI of an object that has a namespaced
 * name.
 */
public interface Namespaced {

    /**
     * Return the local part of the object's name.
     * 
     * @return the local part of the name, or an empty string if this Item is the root node of the workspace.
     * @throws RepositoryException if an error occurs.
     */
    public String getLocalName() throws RepositoryException;

    /**
     * Returns the URI in which this object's name is scoped. For example, if this object's JCR name is "jcr:primaryType", the
     * namespace prefix used in the name is "jcr", and so this method would return the "http://www.jcp.org/jcr/1.0" URI.
     * 
     * @return the URI of the namespace, or an empty string if the name does not use a namespace
     * @throws RepositoryException if an error occurs.
     */
    public String getNamespaceURI() throws RepositoryException;
}
