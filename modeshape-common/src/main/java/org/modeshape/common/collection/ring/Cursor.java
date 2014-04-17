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
 * A cursor in a ring buffer at which point information can be added to the buffer. A ring buffer uses its cursor to keep track of
 * the {@link Pointer positions} of all consumers (to keep from overlapping them), and to ensure that entries are added to the
 * buffer in the correct fashion using a two-phase process:
 * <ol>
 * <li>{@link #claim() Claim} the next position for writing</li>
 * <li>{@link #publish Publish} that one or more positions has been successfully populated with entries and that the consumers are
 * free to consume them</li>
 * </ol>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface Cursor {

    /**
     * Get the size of the buffer that this cursor operates against.
     * 
     * @return the size of the ring buffer; always positive and a power of 2
     */
    int getBufferSize();

    /**
     * Get the current position that can be read.
     * 
     * @return the position for entries that have been published.
     */
    long getCurrent();

    /**
     * Claim the next position for writing and publishing. This method blocks until the next position is available.
     * 
     * @return the position that is available for publishing
     */
    long claim();

    /**
     * Claim a batch of positions for writing and publishing. This method blocks until all desired positions are available.
     * 
     * @param number the number of positions to be claimed; must be greater than 0
     * @return the largest position that is available for publishing
     */
    long claim( int number );

    /**
     * Publish the supplied position, making it available for consumers.
     * 
     * @param position the position that is now available for consumers
     * @return true if the position was published, or false if not
     */
    boolean publish( long position );

    /**
     * Get the highest published position that is equal to or between the supplied lower and upper positions.
     * 
     * @param lowerPosition the lowest potential position
     * @param upperPosition the highest potential position
     * @return the highest available position
     */
    long getHighestPublishedPosition( long lowerPosition,
                                      long upperPosition );

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

    /**
     * Add a new barrier that a consumer can use the wait for the next available positions.
     * 
     * @return the new barrier; never null
     */
    PointerBarrier newBarrier();

    /**
     * Return a new pointer that starts at this cursor's current position, ensuring that this cursor always
     * {@link #stayBehind(Pointer...) stays behind} it on the ring buffer.
     * 
     * @return the new pointer
     */
    Pointer newPointer();

    /**
     * Signal that no more entries will be written and that the {@link #newBarrier() barriers} will not block on this cursor and
     * should return a negative number from {@link PointerBarrier#waitFor(long)}.
     */
    void complete();
}
