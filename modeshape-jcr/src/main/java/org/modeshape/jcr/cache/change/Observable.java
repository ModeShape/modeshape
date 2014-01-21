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

import org.modeshape.common.annotation.ThreadSafe;

/**
 * Interface used to register {@link ChangeSetListener listeners}. Implementations should use a {@link ChangeSetListener} to
 * actually manage the listeners.
 */
@ThreadSafe
public interface Observable {

    /**
     * Register the supplied observer. This method does nothing if the observer reference is null.
     * 
     * @param observer the observer to be added; may be null
     * @return true if the observer was added, or false if the observer was null, if the observer was already registered, or if
     *         the observer could not be added
     */
    boolean register( ChangeSetListener observer );

    /**
     * Unregister the supplied observer. This method does nothing if the observer reference is null.
     * 
     * @param observer the observer to be removed; may not be null
     * @return true if the observer was removed, or false if the observer was null or if the observer was not registered on this
     *         source
     */
    boolean unregister( ChangeSetListener observer );

}
