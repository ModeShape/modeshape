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
 * @param <C> the type of the consumer
 */
final class StandardConsumerAdapter<T, C extends Consumer<T>> implements ConsumerAdapter<T, C> {

    /**
     * Create a new instance.
     * 
     * @return the new adapter
     */
    public static <T, C extends Consumer<T>> ConsumerAdapter<T, C> create() {
        return new StandardConsumerAdapter<>();
    }

    private StandardConsumerAdapter() {
    }

    @Override
    public boolean consume( C consumer,
                            T event,
                            long position,
                            long maxPosition ) {
        return consumer.consume(event, position, maxPosition);
    }

    @Override
    public void close( C consumer ) {
        consumer.close();
    }

    @Override
    public void handleException( C consumer,
                                 Throwable t,
                                 T entry,
                                 long position,
                                 long maxPosition ) {
        // nothing by default
    }
}
