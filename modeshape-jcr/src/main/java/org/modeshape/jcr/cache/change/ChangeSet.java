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

/**
 * A set of internal repository changes.
 */
public interface ChangeSet extends Iterable<Change>, Serializable {

    /**
     * Return the number of individual changes.
     * 
     * @return the number of changes
     */
    public int size();

    /**
     * Checks if this set has any changes.
     *
     * @return {@code true} if there are any changes in this set.
     */
    public boolean isEmpty();

    /**
     * Returns the ID (username) of the user which performed the changes.
     *
     * @return a {@link String} representing the username; may be {@code null} in the case of changes performed "by the system.
     */
    public String getUserId();

    /**
     * Returns a set of (key,value) pairs which may contain additional user information.
     *
     * @return a {@link Map} of additional information; never {@code null} but possibly empty.
     */
    public Map<String, String> getUserData();

    /**
     * Returns the time at which the change set was created.
     *
     * @return a {@code DateTime} instance; never {@code null}
     */
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

    /**
     * Returns the set of keys for the nodes which has been changed.
     *
     * @return a {@link Set<NodeKey>} instance; never {@code null}
     */
    public Set<NodeKey> changedNodes();

    /**
     * Returns the identifier of the local {@link org.modeshape.jcr.journal.ChangeJournal} instance.
     *
     * @return either a non-null {@link String} if journaling is enabled, or {@code null} if journaling isn't enabled.
     */
    public String getJournalId();

}
