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

import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * The top-level interface for an index owned by a provider, with common methods needed by indexes regardless of number of
 * columns.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface ManagedIndex extends Filter, Costable {

    /**
     * Get the ChangeSetAdapter implementation through which changes to content are sent to the index. Each local index has an
     * {@link ChangeSetListener} that is registered on the event bus and kept throughout the lifetime of the index (even if there
     * are changes), and that listener delegates to this adapter.
     *
     * @return the adapter; never null
     */
    IndexChangeAdapter getIndexChangeAdapter();

    /**
     * Remove all of the index entries from the index. This is typically called prior to reindexing.
     */
    void removeAll();

    /**
     * Shut down this index and release all runtime resources. If {@code destroyed} is {@code true}, then this index has been
     * removed from the repository and will not be reused; thus all persistent resources should also be released. If
     * {@code destroyed} is {@code false}, then this repository is merely shutting down and the index's persistent resources
     * should be kept so that they are available when the repository is restarted.
     *
     * @param destroyed true if this index is being permanently removed from the repository and all runtime and persistent
     *        resources can/should be released and cleaned up, or false if the repository is being shutdown and this index will be
     *        needed the next time the repository is started
     */
    void shutdown( boolean destroyed );

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
}
