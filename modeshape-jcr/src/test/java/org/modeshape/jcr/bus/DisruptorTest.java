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

package org.modeshape.jcr.bus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Test;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class DisruptorTest {

    public static class LongEvent {
        private long value;

        public void set( long value ) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    public static class LongEventFactory implements EventFactory<LongEvent> {
        @Override
        public LongEvent newInstance() {
            return new LongEvent();
        }
    }

    public static class LongEventHandler implements EventHandler<LongEvent> {
        @Override
        public void onEvent( LongEvent event,
                             long sequence,
                             boolean endOfBatch ) {
            try {
                System.out.println("--> Started handling event: " + event);
                Thread.sleep(1200);
                System.out.println("--> Completed handling event: " + event);
            } catch (Exception e) {
                System.err.println("Failed to sleep in event handler");
            }
        }
    }

    public static class LongEventProducer {
        private final RingBuffer<LongEvent> ringBuffer;
        private long nextValue = 1000L;

        public LongEventProducer( RingBuffer<LongEvent> ringBuffer ) {
            this.ringBuffer = ringBuffer;
        }

        public void produce() {
            System.out.println("----> Asking for next event");
            long sequence = ringBuffer.next(); // Grab the next sequence
            System.out.println("----> Generating next event: " + nextValue);
            try {
                LongEvent event = ringBuffer.get(sequence); // Get the entry in the Disruptor
                                                            // for the sequence
                event.set(nextValue); // Fill with data
                ++nextValue;
                System.out.println("----> Publishing next event: " + event);
            } finally {
                ringBuffer.publish(sequence);
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void test() throws Exception {
        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // The factory for the event
        LongEventFactory factory = new LongEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 8;

        // Construct the Disruptor
        Disruptor<LongEvent> disruptor = new Disruptor<>(factory, bufferSize, executor, ProducerType.SINGLE,
                                                         new BlockingWaitStrategy());

        // Connect the handler
        disruptor.handleEventsWith(new LongEventHandler());

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        LongEventProducer producer = new LongEventProducer(ringBuffer);

        for (int i = 0; i != 10; ++i) {
            producer.produce();
            // Thread.sleep(100);
        }
        Thread.sleep(10000);
    }
}
