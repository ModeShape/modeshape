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

import java.security.AccessControlContext;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.security.auth.Subject;

/**
 * ModeShape currently defines three roles: {@link #READONLY readonly}, {@link #READWRITE readwrite}, and {@link #ADMIN admin}. If
 * the {@link Credentials}; passed into {@link Repository#login(Credentials)} (or the {@link Subject} from the
 * {@link AccessControlContext}, if one of the no-credential <code>login(...)</code> methods are used) have any of these roles,
 * the session will have the corresponding access to all workspaces within the repository.
 * <p>
 * The mapping from the roles to the actions that they allow is provided below, for any values of <code>path</code>.
 * </p>
 * <h3>Role / Action Mapping</h3>
 * <table border="1" cellspacing="0" cellpadding="2">
 * <tr>
 * <td><b>Action Name</b></td>
 * <td><b>readonly</b></td>
 * <td><b>readwrite</b></td>
 * <td><b>admin</b></td>
 * </tr>
 * </thead>
 * <tr>
 * <td>read</td>
 * <td>Allows</td>
 * <td>Allows</td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>add_node</td>
 * <td></td>
 * <td>Allows</td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>set_property</td>
 * <td></td>
 * <td>Allows</td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>remove</td>
 * <td></td>
 * <td>Allows</td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>register_namespace</td>
 * <td></td>
 * <td></td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>register_type</td>
 * <td></td>
 * <td></td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>unlock_any</td>
 * <td></td>
 * <td></td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>create_workspace</td>
 * <td></td>
 * <td></td>
 * <td>Allows</td>
 * </tr>
 * <tr>
 * <td>delete_workspace</td>
 * <td></td>
 * <td></td>
 * <td>Allows</td>
 * </tr>
 * </table>
 * </p>
 */
public interface ModeShapeRoles {

    /**
     * Constant containing the "readonly" role name.
     */
    public static final String READONLY = "readonly";
    /**
     * Constant containing the "readwrite" role name.
     */
    public static final String READWRITE = "readwrite";
    /**
     * Constant containing the "admin" role name.
     */
    public static final String ADMIN = "admin";

}
