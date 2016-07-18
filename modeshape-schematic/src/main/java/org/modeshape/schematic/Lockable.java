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
package org.modeshape.schematic;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link SchematicDb} which has the ability to lock.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Lockable {

    /**
     * Locks a list of keys exclusively, for writing.
     * 
     * @param locks a list of locks
     * @return {@code true} if the operation was successful and the locks were obtained, false otherwise
     */
    boolean lockForWriting( List<String> locks );

    /**
     * @see Lockable#lockForWriting(List) 
     */
    default boolean lockForWriting( String...locks ) {
        return lockForWriting(Arrays.asList(locks));        
    }
}
