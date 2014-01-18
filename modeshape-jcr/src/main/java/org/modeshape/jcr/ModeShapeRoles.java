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
