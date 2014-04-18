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

import java.util.concurrent.TimeoutException;

/**
 * A barrier that encapsulates a {@link Pointer} and that will {@link #waitFor(long) wait} to advance it until it is safe to do so
 * and there are entries available to read. This is used within the {@link RingBuffer#addConsumer} method to provide each consumer
 * with a valid {@link Pointer} that stays as close as possible behind the ring buffer's {@link Cursor}.
 * 
 * @see Cursor#newBarrier()
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface PointerBarrier extends AutoCloseable {

    /**
     * Wait for the given position to be available for consumption.
     * 
     * @param position the sequence to wait for
     * @return the position up to which is available, which may be larger than the requested position or a negative number if
     *         there will never be any more positions
     * @throws InterruptedException if the thread needs awaking on a condition variable.
     * @throws TimeoutException if this blocking method times out
     */
    long waitFor( long position ) throws InterruptedException, TimeoutException;

    /**
     * Return whether this barrier has completed and should no longer be used.
     * 
     * @return true if this barrier is complete, or false otherwise
     */
    boolean isComplete();

    /**
     * Signal that this barrier is closed and should return -1 from {@link #waitFor(long)}.
     */
    @Override
    public void close();
}
