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
package org.modeshape.jcr.cache.change;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;

/**
 * 
 */
public interface ChangeSet extends Iterable<Change>, Serializable {

    /**
     * Return the number of individual changes.
     * 
     * @return the number of changes
     */
    public int size();

    public boolean isEmpty();

    public String getUserId();

    public Map<String, String> getUserData();

    public DateTime getTimestamp();

    /**
     * Get the key of the process in which the changes were made.
     * 
     * @return the process key; never null
     */
    public String getProcessKey();

    /**
     * Get the key of the repository in which the changes were made.
     * 
     * @return the repository key; never null
     */
    public String getRepositoryKey();

    /**
     * Get the name of the workspace in which the changes were made.
     * 
     * @return the workspace name; may be null only when workspaces are added or removed
     */
    public String getWorkspaceName();

    public Set<NodeKey> changedNodes();

    /**
     * Returns the set of binary keys for those binary values which have become unused.
     *
     * @return the set of binary keys; never null;
     */
    public Set<BinaryKey> unusedBinaries();

    /**
     * Returns the set of binary keys for those binary values which are being used.
     *
     * @return the set of binary keys; never null;
     */
    public Set<BinaryKey> usedBinaries();

    /**
     * Checks if there are any binary changes in this change set.
     *
     * @return true if there are any binary changes (either values used or unused), false otherwise.
     */
    public boolean hasBinaryChanges();

    /**
     * Returns the ID of the session in which the changes occurred.
     *
     * @return the id of a session, never {@code null}
     */
    public String getSessionId();
}
