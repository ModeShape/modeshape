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
package org.modeshape.jcr.security;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.ModeShapePermissions;
import org.modeshape.jcr.value.Path;

/**
 * An interface that can authorize access to specific resources within repositories.
 */
public interface AuthorizationProvider {

    /**
     * Determine if the supplied execution context has permission for all of the named actions in the named workspace. If not all
     * actions are allowed, the method returns false.
     * 
     * @param context the context in which the subject is performing the actions on the supplied workspace
     * @param repositoryName the name of the repository containing the workspace content
     * @param repositorySourceName <i>This is no longer used and will always be the same as the repositoryName</i>
     * @param workspaceName the name of the workspace in which the path exists
     * @param absPath the absolute path on which the actions are occurring, or null if the permissions are at the workspace-level
     * @param actions the list of {@link ModeShapePermissions actions} to check
     * @return true if the subject has privilege to perform all of the named actions on the content at the supplied path in the
     *         given workspace within the repository, or false otherwise
     */
    boolean hasPermission( ExecutionContext context,
                           String repositoryName,
                           String repositorySourceName,
                           String workspaceName,
                           Path absPath,
                           String... actions );
}
