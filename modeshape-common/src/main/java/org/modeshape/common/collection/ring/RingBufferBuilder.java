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

import java.util.concurrent.Executor;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.collection.ring.RingBuffer.ConsumerAdapter;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;

/**
 * @param <T> the type of entries stored in the buffer
 * @param <C> the type of consumer
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RingBufferBuilder<T, C> {

    /**
     * Create a builder for ring buffers that use the supplied {@link Executor} to create consumer threads and the supplied
     * {@link ConsumerAdapter} to adapt to custom consumer implementations. The ring buffer will <i>only</i> allow entries to be
     * added from a single thread.
     * 
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param adapter the adapter to the desired consumer interface; may not be null
     * @return the builder for ring buffers; never null
     */
    public static <T, C> RingBufferBuilder<T, C> withSingleProducer( Executor executor,
                                                                     ConsumerAdapter<T, C> adapter ) {
        return new RingBufferBuilder<>(executor, adapter).singleProducer();
    }

    /**
     * Create a builder for ring buffers that use the supplied {@link Executor} to create threads for each {@link Consumer}
     * instance. The ring buffer will <i>only</i> allow entries to be added from a single thread.
     * 
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param entryClass the type of entry that will be put into and consumed from the buffer; may not be null
     * @return the builder for ring buffers; never null
     */
    public static <T, C extends Consumer<T>> RingBufferBuilder<T, C> withSingleProducer( Executor executor,
                                                                                         Class<T> entryClass ) {
        return new RingBufferBuilder<>(executor, StandardConsumerAdapter.<T, C>create()).singleProducer();
    }

    /**
     * Create a builder for ring buffers that use the supplied {@link Executor} to create consumer threads and the supplied
     * {@link ConsumerAdapter} to adapt to custom consumer implementations. The ring buffer will allow entries to be added from
     * multiple threads.
     * 
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param adapter the adapter to the desired consumer interface; may not be null
     * @return the builder for ring buffers; never null
     */
    public static <T, C> RingBufferBuilder<T, C> withMultipleProducers( Executor executor,
                                                                        ConsumerAdapter<T, C> adapter ) {
        return new RingBufferBuilder<>(executor, adapter).multipleProducers();
    }

    /**
     * Create a builder for ring buffers that use the supplied {@link Executor} to create threads for each {@link Consumer}
     * instance. The ring buffer will allow entries to be added from multiple threads.
     * 
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param entryClass the type of entry that will be put into and consumed from the buffer; may not be null
     * @return the builder for ring buffers; never null
     */
    public static <T, C extends Consumer<T>> RingBufferBuilder<T, C> withMultipleProducers( Executor executor,
                                                                                            Class<T> entryClass ) {
        return new RingBufferBuilder<>(executor, StandardConsumerAdapter.<T, C>create()).multipleProducers();
    }

    public static final int DEFAULT_BUFFER_SIZE = 1 << 10; // 1024
    public static final boolean DEFAULT_GARBAGE_COLLECT_ENTITIES = false;
    public static final String DEFAULT_NAME = "ringbuffer";

    private final Executor executor;
    private final ConsumerAdapter<T, C> adapter;
    private final Logger logger = Logger.getLogger(getClass());
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private boolean garbageCollect = DEFAULT_GARBAGE_COLLECT_ENTITIES;
    private boolean singleProducer = true;
    private String name = DEFAULT_NAME;
    private WaitStrategy waitStrategy;
    
    /**
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param adapter the adapter for consumers; may not be null
     */
    protected RingBufferBuilder( Executor executor,
                                 ConsumerAdapter<T, C> adapter ) {
        CheckArg.isNotNull(executor, "executor");
        CheckArg.isNotNull(adapter, "adapter");
        this.executor = executor;
        this.adapter = adapter;
    }

    public RingBufferBuilder<T, C> ofSize( int bufferSize ) {
        CheckArg.isPositive(bufferSize, "bufferSize");
        try {
            CheckArg.isPowerOfTwo(bufferSize, "bufferSize");
        } catch (IllegalArgumentException e) {
            int nextPowerOf2 = nextPowerOf2(bufferSize);
            logger.warn(CommonI18n.incorrectRingBufferSize, bufferSize, nextPowerOf2);
            bufferSize = nextPowerOf2;
        }
        this.bufferSize = bufferSize;
        return this;
    }
    
    private int nextPowerOf2(int number) {
        if (number <= 0) {
            return 1;
        }
        --number;
        number = number | (number >> 1);
        number = number | (number >> 2);
        number = number | (number >> 4);
        number = number | (number >> 8);
        number = number | (number >> 16);
        return ++number;
    }

    public RingBufferBuilder<T, C> garbageCollect( boolean gcEntries ) {
        this.garbageCollect = gcEntries;
        return this;
    }

    public RingBufferBuilder<T, C> waitUsing( WaitStrategy waitStrategy ) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    protected RingBufferBuilder<T, C> singleProducer() {
        this.singleProducer = true;
        return this;
    }

    protected RingBufferBuilder<T, C> multipleProducers() {
        this.singleProducer = false;
        return this;
    }

    public RingBufferBuilder<T, C> named( String bufferName ) {
        if (bufferName != null && bufferName.trim().isEmpty()) this.name = bufferName;
        return this;
    }

    public RingBuffer<T, C> build() {
        WaitStrategy waitStrategy = this.waitStrategy;
        if (waitStrategy == null) waitStrategy = defaultWaitStrategy();
        Cursor cursor = defaultCursor(bufferSize, waitStrategy);
        return new RingBuffer<T, C>(name, cursor, executor, adapter, garbageCollect, singleProducer);
    }

    protected WaitStrategy defaultWaitStrategy() {
        return new BlockingWaitStrategy();
    }

    protected Cursor defaultCursor( int bufferSize,
                                    WaitStrategy waitStrategy ) {
        return new SingleProducerCursor(bufferSize, waitStrategy);
    }
}
