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
package org.modeshape.jcr.security;

import javax.jcr.Session;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.ModeShapePermissions;
import org.modeshape.jcr.value.Path;

/**
 * An interface that can authorize access to specific resources within repositories. Unlike the more basic and simpl
 * {@link AuthenticationProvider}, this interface allows an implementation to get at additional information with each call to
 * {@link #hasPermission(Context, Path, String...)}.
 * <p>
 * In particular, the supplied {@link Context} instance contains the {@link Session} that is calling this provider, allowing the
 * provider implementation to access authorization-specific content within the repository to determine permissions for other
 * repository content.
 * </p>
 * <p>
 * In these cases, calls to the session to access nodes will result in their own calls to
 * {@link #hasPermission(Context, Path, String...)}. Therefore, such implementations need to handle these special
 * authorization-specific content permissions in an explicit fashion. It is also adviced that such providers cache as much of the
 * authorization-specifc content as possible, as the {@link #hasPermission(Context, Path, String...)} method is called frequently.
 * </p>
 */
public interface AdvancedAuthorizationProvider {

    /**
     * The context in which the calling session is operating, and which contains session-related information that a provider
     * implementation may find useful.
     */
    public static interface Context {
        /**
         * Get the execution context in which this session is running.
         * 
         * @return the session's execution context; never null
         */
        public ExecutionContext getExecutionContext();

        /**
         * Get the session that is requesting this authorization provider to
         * {@link AdvancedAuthorizationProvider#hasPermission(Context, Path, String...) determine permissions}. Provider
         * implementations are free to use the session to access nodes <i>other</i> than those for which permissions are being
         * determined. For example, the implementation may access other <i>authorization-related content</i> inside the same
         * repository. Just be aware that such accesses will generate additional calls to the
         * {@link AdvancedAuthorizationProvider#hasPermission(Context, Path, String...)} method.
         * 
         * @return the session; never null
         */
        public Session getSession();

        /**
         * Get the name of the repository that is being accessed.
         * 
         * @return the repository name; never null
         */
        public String getRepositoryName();

        /**
         * Get the name of the repository workspace that is being accessed.
         * 
         * @return the workspace name; never null
         */
        public String getWorkspaceName();
    }

    /**
     * Determine if the supplied execution context has permission for all of the named actions in the given context. If not all
     * actions are allowed, the method returns false.
     * 
     * @param context the context in which the subject is performing the actions on the supplied workspace
     * @param absPath the absolute path on which the actions are occurring, or null if the permissions are at the workspace-level
     * @param actions the list of {@link ModeShapePermissions actions} to check
     * @return true if the subject has privilege to perform all of the named actions on the content at the supplied path in the
     *         given workspace within the repository, or false otherwise
     */
    boolean hasPermission( Context context,
                           Path absPath,
                           String... actions );
}
