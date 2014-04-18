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

package org.modeshape.common.collection.ring;

/**
 * An interface that defines methods to add and remove dependent {@link Pointer}s.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface DependentOnPointers {

    /**
     * Ensure that this cursor always remains behind the specified pointer.
     * 
     * @param pointers the pointers that this cursor may not run past in the ring buffer
     */
    void stayBehind( Pointer... pointers );

    /**
     * Ignore the specified pointer that cursor had previously {@link #stayBehind(Pointer[]) stayed behind}. This should be called
     * when the supplied pointer is removed from the ring buffer.
     * 
     * @param pointer the pointer that this cursor may no longer depend upon
     * @return true if this cursor did depend on the supplied pointer and now no longer does, or false if this cursor never
     *         dependent upon the supplied pointer
     */
    boolean ignore( Pointer pointer );
}
