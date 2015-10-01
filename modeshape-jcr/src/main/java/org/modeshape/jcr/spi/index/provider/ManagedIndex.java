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

package org.modeshape.jcr.spi.index.provider;

import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * The top-level interface for an index owned by a provider, with common methods needed by indexes regardless of number of
 * columns.
 * <p>
 * Index providers may choose to implement this from scratch and return it to the repository, or they may choose to implement a
 * {@link ManagedIndexBuilder} instance and use an {@link DefaultManagedIndex} instance to wrap their {@link ProvidedIndex} instances.
 * </p>
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 * 
 * @see ManagedIndexBuilder
 * @see ProvidedIndex
 */
public interface ManagedIndex extends Filter, Costable, Lifecycle, Reindexable {

    /**
     * Get the ChangeSetAdapter implementation through which changes to content are sent to the index. Each local index has an
     * {@link ChangeSetListener} that is registered on the event bus and kept throughout the lifetime of the index (even if there
     * are changes), and that listener delegates to this adapter.
     *
     * @return the adapter; never null
     */
    IndexChangeAdapter getIndexChangeAdapter();

    /**
     * Mark whether this index is enabled for use, or not enabled meaning it should not be used.
     *
     * @param enable true if the index is to be enabled, or false otherwise
     */
    void enable( boolean enable );

    /**
     * Determine if this index is enabled for use.
     * 
     * @return true if enabled, or false otherwise
     */
    boolean isEnabled();

    /**
     * Return the current status of the managed index 
     * 
     * @return a {@link org.modeshape.jcr.api.index.IndexManager.IndexStatus} instance, never null
     */
    IndexManager.IndexStatus getStatus();

    /**
     * Update the status of this index to a new value if the current status is {@code currentStatus}
     * 
     * @param currentStatus a {@link org.modeshape.jcr.api.index.IndexManager.IndexStatus} instance, may not be null 
     * @param newStatus a {@link org.modeshape.jcr.api.index.IndexManager.IndexStatus} instance, may not be null
     */
    void updateStatus(IndexManager.IndexStatus currentStatus, IndexManager.IndexStatus newStatus);
}
