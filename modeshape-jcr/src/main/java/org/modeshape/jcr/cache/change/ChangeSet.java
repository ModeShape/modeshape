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
package org.modeshape.jcr.cache.change;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;

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
     * @return the set of changed node keys; never {@code null}
     */
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

    /**
     * Returns the identifier of the local {@link org.modeshape.jcr.journal.ChangeJournal} instance.
     * 
     * @return either a non-null {@link String} if journaling is enabled, or {@code null} if journaling isn't enabled.
     */
    public String getJournalId();

    /**
     * Returns a unique identifier for this change set.
     *
     * @return a {@link String}; never null.
     */
    public String getUUID();

}
