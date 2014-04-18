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
 * A consumer of entries that are added to the ring buffer.
 * 
 * @param <T> the type of entry
 * @see RingBuffer#addConsumer(Object)
 * @see RingBuffer#addConsumer(Object, int)
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class Consumer<T> implements AutoCloseable {
    /**
     * Consume an entry from the ring buffer. All exceptions should be handled by the implementation.
     * 
     * @param entry the entry
     * @param position the position of the entry in the ring buffer
     * @return true if the consumer should continue processing the next entry, or false if this consumer should stop
     */
    public abstract boolean consume( T entry,
                                     long position );

    /**
     * Called by the {@link RingBuffer} when the buffer has been shutdown and after this consumer's has
     * {@link #consume(Object, long) consumed} all entries that remain in the now-closed buffer.
     * <p>
     * This method does nothing by default, but subclasses can override it to be notified when the consumer will not be called
     * again and can clean up any resources.
     * </p>
     */
    @Override
    public void close() {
        // do nothing by default
    }

}
