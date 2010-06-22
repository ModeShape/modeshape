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
package org.modeshape.jcr;

import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * The set of constants that can be used for action literals with the {@link Session#checkPermission(String, String)} method.
 * <p>
 * ModeShape has extended the set of JCR-defined actions ("add_node", "set_property", "remove", and "read") with additional
 * actions ("register_type", "register_namespace", and "unlock_any"). The {@link #REGISTER_TYPE register_type} and
 * {@link #REGISTER_NAMESPACE register_namespace} permissions define the ability to register (and unregister) node types and
 * namespaces, respectively. The {@link #UNLOCK_ANY unlock_any} permission grants the user the ability to unlock any locked node
 * or branch (as opposed to users without that permission who can only unlock nodes or branches that they have locked themselves
 * or for which they hold the lock token).
 * </p>
 * <p>
 * Permissions to perform these actions are aggregated in {@link ModeShapeRoles roles} that can be assigned to users.
 * </p>
 */
public interface ModeShapePermissions {

    /**
     * The {@link #REGISTER_NAMESPACE register_namespace} permission define the ability to register (and unregister) namespaces.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String REGISTER_NAMESPACE = "register_namespace";
    /**
     * The {@link #REGISTER_TYPE register_type} permission define the ability to register (and unregister) node types.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String REGISTER_TYPE = "register_type";
    /**
     * The {@link #UNLOCK_ANY unlock_any} permission grants the user the ability to unlock any locked node or branch (as opposed
     * to users without that permission who can only unlock nodes or branches that they have locked themselves or for which they
     * hold the lock token).
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String UNLOCK_ANY = "unlock_any";

    /**
     * A standard JCR-defined permission to grant the user the ability to add nodes.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String ADD_NODE = "add_node";
    /**
     * A standard JCR-defined permission to grant the user the ability to add or change properties.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String SET_PROPERTY = "set_property";
    /**
     * A standard JCR-defined permission to grant the user the ability to remove nodes and/or properties.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String REMOVE = "remove";
    /**
     * A standard JCR-defined permission to grant the user the ability to read nodes and/or properties.
     * 
     * @see Session#checkPermission(String, String)
     */
    public static final String READ = "read";

    /**
     * The {@link #CREATE_WORKSPACE create_workspace} permission allows the user the ability to
     * {@link Workspace#createWorkspace(String) create new workspaces}.
     */
    public static final String CREATE_WORKSPACE = "create_workspace";

    /**
     * The {@link #DELETE_WORKSPACE delete_workspace} permission allows the user the ability to
     * {@link Workspace#deleteWorkspace(String) delete workspaces}.
     */
    public static final String DELETE_WORKSPACE = "delete_workspace";

}
