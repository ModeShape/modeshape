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
package org.modeshape.jcr.api.version;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * ModeShape's extension of {@link javax.jcr.version.VersionManager}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface VersionManager extends javax.jcr.version.VersionManager {

    /**
     * Removes the versionable node at {@code absPath} together will all the version histories of each versionable node
     * in the subgraph, provided that only the root version exists for each versionable node and each versionable node is
     * not shared.
     * <br>
     * This method should only be used when wanting to clean up the version histories of versionable nodes and when there is no
     * desire to ever restore the versionable node at any time in the future.
     * <br>
     * This is a workspace-write method, meaning there is no need to call {@code session.save()} in order to persist the changes.
     *
     * @param absPath a {@link String} the absolute path to a versionable node; never {@code null}
     * @throws javax.jcr.PathNotFoundException if no node exists at {@code absPath}
     * @throws UnsupportedRepositoryOperationException if the given node is not {@code mix:versionable} or any versionable node
     * in the hierarchy has a non-empty version history or any versionable node in the hierarchy is a shared node.
     * @throws javax.jcr.RepositoryException if anything unexpected fails while performing the operation
     */
    public void remove(String absPath) throws UnsupportedRepositoryOperationException, PathNotFoundException,RepositoryException;
}
