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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RingBufferTest {

    protected volatile boolean print = false;
    protected volatile boolean slightPausesInConsumers = false;

    @Before
    public void beforeEach() {
        print = false;
    }

    @Test
    public void shouldBeAbleToAddAndRemoveConsumers() throws Exception {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBuffer.withSingleProducer(executor, 8);
        print = true;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a second consumer that should start seeing items 20 and up ...
        MonotonicallyIncreasingConsumer consumer2 = new MonotonicallyIncreasingConsumer("second", 20L, 20L);
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
        ringBuffer.shutdown(true);
        assertTrue(consumer1.isClosed());
        assertTrue(consumer2.isClosed());
    }

    @Test
    public void consumersShouldSeeEventsInCorrectOrder() throws Exception {
        Executor executor = Executors.newCachedThreadPool();
        RingBuffer<Long, Consumer<Long>> ringBuffer = RingBuffer.withSingleProducer(executor, 8);
        print = true;

        // Add 10 entries with no consumers ...
        long value = 0L;
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        // Add a single consumer that should start seeing items 10 and up ...
        MonotonicallyIncreasingConsumer consumer1 = new MonotonicallyIncreasingConsumer("first", 10L, 10L);
        ringBuffer.addConsumer(consumer1);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        // Add a second consumer that should start seeing items 20 and up ...
        MonotonicallyIncreasingConsumer consumer2 = new MonotonicallyIncreasingConsumer("second", 20L, 20L);
        ringBuffer.addConsumer(consumer2);

        // Add 10 more entries ...
        for (int i = 0; i != 10; ++i) {
            print("Adding entry " + value);
            ringBuffer.add(value++);
            // Thread.sleep(100L);
        }

        print = false;
        slightPausesInConsumers = false;

        // Add 400K more entries
        Stopwatch sw = new Stopwatch();
        int count = 400000;
        sw.start();
        for (int i = 0; i != count; ++i) {
            ringBuffer.add(value++);
        }
        sw.stop();

        // Do 10 more while printing ...
        for (int i = 0; i != 10; ++i) {
            print = true;
            print("Adding entry " + value);
            ringBuffer.add(value++);
        }

        boolean blockWhileShuttingDown = false;
        ringBuffer.shutdown(blockWhileShuttingDown);
        print("");
        print("Ring buffer shutdown completed");
        if (blockWhileShuttingDown) {
            assertTrue(consumer1.isClosed());
            assertTrue(consumer2.isClosed());
        } else {
            // Wait while the threads complete
            while (!consumer1.isClosed()) {
                Thread.sleep(10L);
            }

            while (!consumer2.isClosed()) {
                Thread.sleep(10L);
            }
        }

        --value;
        assertThat(consumer1.getLastValue(), is(value));
        assertThat(consumer2.getLastValue(), is(value));

        print("");
        print("Time to add " + count + " entries: " + sw.getAverageDuration());
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

        public MonotonicallyIncreasingConsumer( String id,
                                                long firstValue,
                                                long firstPosition ) {
            this.id = id;
            this.lastValue = firstValue;
            this.lastPosition = firstPosition;
        }

        @Override
        public boolean consume( Long entry,
                                long position ) {
            assertTrue(!closed);
            print(id + " consuming " + entry.longValue() + " at position " + position);
            if (slightPausesInConsumers && position % 1000 == 0) {
                try {
                    Thread.sleep(100L);
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
                assertTrue(entry.longValue() > lastValue);
                lastValue = entry.longValue();
                assertTrue(position > lastPosition);
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

}
