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

package org.modeshape.jcr.index.local;

import org.modeshape.jcr.spi.index.provider.Costable;
import org.modeshape.jcr.spi.index.provider.Filter;

/**
 * @param <T> the type of value that is added to the index
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface LocalIndex<T> extends Filter, Costable {

    /**
     * Get the estimated number of entries within this index.
     *
     * @return the number of entries, or -1 if not known
     */
    long estimateTotalCount();

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

    void add( String nodeKey,
              T value );

    void remove( String nodeKey );

    void remove( String nodeKey,
                 T value );
}
