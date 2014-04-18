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

import static java.util.Arrays.copyOf;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Static methods for atomically modifying an array of {@link Pointer} instances.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Pointers {

    private Pointers() {
    }

    public static long getMinimum( Pointer[] pointers,
                                   long minimum ) {
        for (int i = 0; i != pointers.length; ++i) {
            minimum = Math.min(minimum, pointers[i].get());
        }
        return minimum;
    }

    /**
     * Atomically add the specified {@link Pointer} instance(s) to an array known to the given updator.
     * 
     * @param holder the object that holds the array; may not be null
     * @param updater the updator of the array; may not be null
     * @param cursor the cursor that the pointers will follow; may be null if none of the pointers are to be changed
     * @param pointersToAdd the pointer(s) to be added to the existing pointers
     */
    public static <T> void add( T holder,
                                AtomicReferenceFieldUpdater<T, Pointer[]> updater,
                                Cursor cursor,
                                Pointer... pointersToAdd ) {
        long currentPosition = 0L;
        Pointer[] updatedPointers;
        Pointer[] currentPointers;

        do {
            currentPointers = updater.get(holder);
            updatedPointers = copyOf(currentPointers, currentPointers.length + pointersToAdd.length);
            if (cursor != null) currentPosition = cursor.getCurrent();

            int index = currentPointers.length;
            for (Pointer pointer : pointersToAdd) {
                if (cursor != null) pointer.set(currentPosition);
                updatedPointers[index++] = pointer;
            }
        } while (!updater.compareAndSet(holder, currentPointers, updatedPointers));

        if (cursor != null) {
            // Set all of the new pointers to the current position ...
            currentPosition = cursor.getCurrent();
            for (Pointer pointer : pointersToAdd) {
                pointer.set(currentPosition);
            }
        }
    }

    /**
     * Atomically remove the specified {@link Pointer} instance from an array known to the given updator.
     * 
     * @param holder the object that holds the array; may not be null
     * @param updater the updator of the array; may not be null
     * @param pointer the pointer to be removed from the existing pointers
     * @return true if the pointer was removed, or false if the array never contained the pointer
     */
    public static <T> boolean remove( T holder,
                                      AtomicReferenceFieldUpdater<T, Pointer[]> updater,
                                      Pointer pointer ) {
        int numToRemove;
        Pointer[] oldPointers;
        Pointer[] newPointers;

        do {
            oldPointers = updater.get(holder);
            numToRemove = countMatching(oldPointers, pointer);
            if (0 == numToRemove) break;

            final int oldSize = oldPointers.length;
            newPointers = new Pointer[oldSize - numToRemove];

            // Copy all but the 'pointer' into the new array ...
            for (int i = 0, pos = 0; i < oldSize; i++) {
                final Pointer testPointer = oldPointers[i];
                if (pointer != testPointer) {
                    newPointers[pos++] = testPointer;
                }
            }
        } while (!updater.compareAndSet(holder, oldPointers, newPointers));
        return numToRemove != 0;
    }

    private static <T> int countMatching( final T[] values,
                                          final T toMatch ) {
        int numToRemove = 0;
        for (T value : values) {
            // Use object identity ...
            if (value == toMatch) numToRemove++;
        }
        return numToRemove;
    }
}
