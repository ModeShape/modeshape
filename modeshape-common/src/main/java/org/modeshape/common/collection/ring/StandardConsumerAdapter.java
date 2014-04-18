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

import org.modeshape.common.collection.ring.RingBuffer.ConsumerAdapter;

/**
 * An implementation of {@link ConsumerAdapter} that uses the standard {@link Consumer} interface.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the type of entry
 */
final class StandardConsumerAdapter<T> implements ConsumerAdapter<T, Consumer<T>> {

    /**
     * Create a new instance.
     * 
     * @return the new adapter
     */
    public static <T> ConsumerAdapter<T, Consumer<T>> create() {
        return new StandardConsumerAdapter<T>();
    }

    private StandardConsumerAdapter() {
    }

    @Override
    public boolean consume( Consumer<T> consumer,
                            T event,
                            long position ) {
        return consumer.consume(event, position);
    }

    @Override
    public void close( Consumer<T> consumer ) {
        consumer.close();
    }

}
