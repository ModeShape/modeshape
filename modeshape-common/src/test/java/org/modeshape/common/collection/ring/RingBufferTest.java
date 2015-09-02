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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.statistic.Stopwatch;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RingBufferTest {
    protected static final Random RANDOM = new Random();

    protected volatile boolean print = false;
    protected volatile boolean slightPausesInConsumers = false;

    @Before
    public void beforeEach() {
        print = false;
    }

    @Test
    public void shouldBuildWithNoGarbageCollection() {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(8)
                                                                       .garbageCollect(false).build();
        print = false;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L, 0);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        ringBuffer.shutdown();
        print("");
        print("Ring buffer shutdown completed");
        assertTrue(consumer1.isClosed());
    }

    @Test
    public void shouldBuildWithGarbageCollectionAnd8Entries() {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(8)
                                                                       .garbageCollect(true).build();
        print = false;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L, 0);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        ringBuffer.shutdown();
        print("");
        print("Ring buffer shutdown completed");
        // assertTrue(consumer1.isClosed());
    }

    @Test
    public void shouldBuildWithGarbageCollectionAnd1024Entries() {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(1024)
                                                                       .garbageCollect(true).build();
        print = false;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L, 0);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 1000; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        ringBuffer.shutdown();
        print("");
        print("Ring buffer shutdown completed");
        // assertTrue(consumer1.isClosed());
    }

    @Test
    public void shouldBeAbleToAddAndRemoveConsumers() throws Exception {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(8)
                                                                       .build();
        print = false;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L, 0);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a second consumer that should start seeing items 20 and up ...
        MonotonicallyIncreasingConsumer consumer2 = new MonotonicallyIncreasingConsumer("second", 20L, 20L, 0);
        ringBuffer.addConsumer(consumer2);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        ringBuffer.remove(consumer2);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        assertTrue(consumer2.isClosed());
        ringBuffer.shutdown();
        print("");
        print("Ring buffer shutdown completed");
        assertTrue(consumer1.isClosed());
        assertTrue(consumer2.isClosed());
    }

    @Test
    // @Ignore( "Takes a long time to run" )
    public void consumersShouldSeeEventsInCorrectOrder() throws Exception {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, MonotonicallyIncreasingConsumer> ringBuffer = RingBufferBuilder.withSingleProducer(executor,
                                                                                                            LongConsumerAdapter.INSTANCE)
                                                                                        .ofSize(8).garbageCollect(false).build();
        print = false;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L, 0);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer2 = new MonotonicallyIncreasingConsumer("second", 20L, 20L, 0);
        ringBuffer.addConsumer(consumer2);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a second consumer that should start seeing items 20 and up ...
        MonotonicallyIncreasingConsumer consumer3 = new MonotonicallyIncreasingConsumer("third", 30L, 30L, 0);
        ringBuffer.addConsumer(consumer3);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a second consumer that should start seeing items 20 and up ...
        MonotonicallyIncreasingConsumer consumer4 = new MonotonicallyIncreasingConsumer("fourth", 40L, 40L, 0);
        ringBuffer.addConsumer(consumer4);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // print = true;
        slightPausesInConsumers = false;
        boolean slightPauseBetweenEvents = false;

        // Add 400K more entries
        Stopwatch sw = new Stopwatch();
        int count = 2000;
        sw.start();
        for (int i = 0; i != count; ++i) {
            ringBuffer.add(value++);
            if (slightPauseBetweenEvents) {
                Thread.sleep(RANDOM.nextInt(50));
            }
        }
        sw.stop();

        // Do 10 more while printing ...
        for (int i = 0; i != 10; ++i) {
            // print = true;
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        ringBuffer.shutdown();
        print("");
        print("Ring buffer shutdown completed");
        assertTrue(consumer1.isClosed());
        assertTrue(consumer2.isClosed());
        assertTrue(consumer3.isClosed());
        assertTrue(consumer4.isClosed());

        --value;
        assertThat(consumer1.getLastValue(), is(value));
        assertThat(consumer2.getLastValue(), is(value));
        assertThat(consumer3.getLastValue(), is(value));
        assertThat(consumer4.getLastValue(), is(value));

        print("");
        print("Time to add " + count + " entries: " + sw.getAverageDuration());
    }
    
    @Test
    @FixFor( "MODE-2195" )
    public void shouldAutomaticallySetTheBufferSizeToTheNextPowerOf2() throws Exception {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(5)
                                                                       .garbageCollect(false).build();    
        assertEquals(8, ringBuffer.getBufferSize());

        ringBuffer = RingBufferBuilder.withSingleProducer(executor, Long.class).ofSize(1023).garbageCollect(false).build();
        assertEquals(1024, ringBuffer.getBufferSize());
    }

    protected void print( String message ) {
        if (print) System.out.println(message);
    }

    protected class MonotonicallyIncreasingConsumer extends Consumer<Long> {
        private final String id;
        private boolean first = true;
        private long lastValue = -1L;
        private long lastPosition = -1L;
        private boolean closed = false;
        private final int secondsToWork;

        public MonotonicallyIncreasingConsumer( String id,
                                                long firstValue,
                                                long firstPosition,
                                                int secondsToWork ) {
            this.id = id;
            this.lastValue = firstValue;
            this.lastPosition = firstPosition;
            this.secondsToWork = secondsToWork;
        }

        @Override
        public boolean consume( Long entry,
                                long position,
                                long max ) {
            assertTrue(!closed);
            print(id + " consuming " + entry.longValue() + " at position " + position + " with max " + max);
            try {
                Thread.sleep(secondsToWork * 1000);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            if (slightPausesInConsumers && position % 1000 == 0) {
                try {
                    Thread.sleep(RANDOM.nextInt(100));
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
            if (first) {
                assertTrue(entry.longValue() == lastValue);
                assertTrue(position == lastPosition);
                first = false;
            } else {
                assertTrue(entry.longValue() == (lastValue + 1));
                lastValue = entry.longValue();
                assertTrue(position == (lastPosition + 1));
                lastPosition = position;
            }
            return true;
        }

        @Override
        public void close() {
            super.close();
            print(id + " closing");
            closed = true;
        }

        public long getLastPosition() {
            return lastPosition;
        }

        public long getLastValue() {
            return lastValue;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    private static class LongConsumerAdapter implements RingBuffer.ConsumerAdapter<Long, MonotonicallyIncreasingConsumer> {
        protected static final LongConsumerAdapter INSTANCE = new LongConsumerAdapter();

        private LongConsumerAdapter() {
        }

        @Override
        public boolean consume( MonotonicallyIncreasingConsumer consumer,
                                Long event,
                                long position,
                                long maxPosition ) {
            consumer.consume(event, position, maxPosition);
            return true;
        }

        @Override
        public void close( MonotonicallyIncreasingConsumer consumer ) {
            consumer.close();
        }

        @Override
        public void handleException( MonotonicallyIncreasingConsumer consumer,
                                     Throwable t,
                                     Long entry,
                                     long position,
                                     long maxPosition ) {
            throw new AssertionError("Test failure", t);
        }
    }

}
