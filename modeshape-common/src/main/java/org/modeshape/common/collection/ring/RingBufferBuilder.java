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
import org.modeshape.common.collection.ring.RingBuffer.ConsumerAdapter;
import org.modeshape.common.util.CheckArg;

/**
 * @param <T> the type of entries stored in the buffer
 * @param <C> the type of consumer
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RingBufferBuilder<T, C> {

    /**
     * Create a builder for ring buffers that use the supplied {@link Executor} to create consumer threads and the supplied
     * {@link ConsumerAdapter} to adapt to custom consumer implementations.
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
     * instance.
     * 
     * @param executor the executor that should be used to create threads to run {@link Consumer}s; may not be null
     * @param entryClass the type of entry that will be put into and consumed from the buffer; may not be null
     * @return the builder for ring buffers; never null
     */
    public static <T, C extends Consumer<T>> RingBufferBuilder<T, C> withSingleProducer( Executor executor,
                                                                                         Class<T> entryClass ) {
        return new RingBufferBuilder<>(executor, StandardConsumerAdapter.<T, C>create()).singleProducer();
    }

    public static final int DEFAULT_BUFFER_SIZE = 1 << 10; // 1024
    public static final boolean DEFAULT_GARBAGE_COLLECT_ENTITIES = false;

    private final Executor executor;
    private final ConsumerAdapter<T, C> adapter;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private boolean garbageCollect = DEFAULT_GARBAGE_COLLECT_ENTITIES;
    private boolean singleProducer = true;
    private WaitStrategy waitStrategy;
    private Cursor cursor;

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
        CheckArg.isPowerOfTwo(bufferSize, "bufferSize");
        this.bufferSize = bufferSize;
        return this;
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

    public RingBuffer<T, C> build() {
        WaitStrategy waitStrategy = this.waitStrategy;
        if (waitStrategy == null) waitStrategy = defaultWaitStrategy();
        Cursor cursor = this.cursor;
        if (cursor == null) cursor = defaultCursor(singleProducer, bufferSize, waitStrategy);
        return new RingBuffer<T, C>(cursor, executor, adapter, garbageCollect);
    }

    protected WaitStrategy defaultWaitStrategy() {
        return new BlockingWaitStrategy();
    }

    protected Cursor defaultCursor( boolean singleProducer,
                                    int bufferSize,
                                    WaitStrategy waitStrategy ) {
        return new SingleProducerCursor(bufferSize, waitStrategy);
    }
}
