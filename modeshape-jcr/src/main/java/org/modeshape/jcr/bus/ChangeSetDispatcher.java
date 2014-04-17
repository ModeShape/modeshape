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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkProcessor;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * A disruptor based class which uses a ring buffer to submit asynchronous {@link ChangeSet} instances.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @see <a href="https://github.com/LMAX-Exchange/disruptor/wiki/Introduction">LMAX Disruptor</a>
 */
public class ChangeSetDispatcher {

    private static final Logger LOGGER = Logger.getLogger(ChangeSetDispatcher.class);

    private static final EventTranslatorOneArg<ChangeSetEvent, ChangeSet> TRANSLATOR =
            new EventTranslatorOneArg<ChangeSetEvent, ChangeSet>() {
                @Override
                public void translateTo( ChangeSetEvent event, long sequence, ChangeSet arg0 ) {
                    event.setData(arg0);
                }
            };

    private static final EventFactory<ChangeSetEvent> FACTORY = new EventFactory<ChangeSetEvent>() {
        @Override
        public ChangeSetEvent newInstance() {
            return new ChangeSetEvent();
        }
    };

    private final Disruptor<ChangeSetEvent> disruptor;
    private final ExecutorService executorService;
    private final List<ChangeSetListener> syncListeners;
    private final List<AsyncChangeSetListener> asyncListeners;

    @SuppressWarnings( "unchecked" )
    protected ChangeSetDispatcher( ExecutorService executorService, int bufferSize ) {
        this.executorService = executorService;
        // Construct the Disruptor
        this.disruptor = new Disruptor(FACTORY,
                                       bufferSize,
                                       executorService,
                                       ProducerType.SINGLE,
                                       new BlockingWaitStrategy());
        // Start the Disruptor, starts all threads running
        disruptor.start();
        syncListeners = new ArrayList<>();
        asyncListeners = new ArrayList<>();
    }

    protected void dispatchAsync( ChangeSet changeSet ) {
        for (ChangeSetListener syncListener : syncListeners) {
            //all sync listener are notified in the same thread
            syncListener.notify(changeSet);
        }
        //all the others are notified via the disruptor
        disruptor.getRingBuffer().publishEvent(TRANSLATOR, changeSet);
    }

    protected void dispatchSync( ChangeSet changeSet ) {
        for (ChangeSetListener syncListener : syncListeners) {
            //all sync listener are notified in the same thread
            syncListener.notify(changeSet);
        }
        for (AsyncChangeSetListener asyncListener : asyncListeners) {
            //all the async listeners are notified in the same thread
            asyncListener.getListener().notify(changeSet);
        }
    }

    protected void stop() {
        syncListeners.clear();
        for (AsyncChangeSetListener asyncListener : asyncListeners) {
            asyncListener.stop();
        }
        asyncListeners.clear();
        disruptor.shutdown();
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    protected void addAsyncListener( ChangeSetListener listener ) {
        RingBuffer<ChangeSetEvent> ringBuffer = disruptor.getRingBuffer();
        EventProcessor processor = new WorkProcessor<>(
                ringBuffer, ringBuffer.newBarrier(),
                new ChangeSetHandler(listener),
                EventProcessorExceptionHandler.INSTANCE,
                new Sequence(ringBuffer.getCursor())
        );
        Future<?> future = executorService.submit(processor);
        asyncListeners.add(new AsyncChangeSetListener(processor, future, listener));
        ringBuffer.addGatingSequences(processor.getSequence());
    }

    protected void addSyncListener( ChangeSetListener listener ) {
        syncListeners.add(listener);
    }

    protected void removeListener( ChangeSetListener listener ) {
        boolean wasRemoved = syncListeners.remove(listener);
        if (wasRemoved) {
            return;
        }
        for (Iterator<AsyncChangeSetListener> asyncListenersIterator = asyncListeners.iterator(); asyncListenersIterator
                .hasNext(); ) {
            AsyncChangeSetListener wrapper = asyncListenersIterator.next();
            if (wrapper.getListener() == listener) {
                Sequence sequence = wrapper.getSequence();
                wrapper.stop();
                disruptor.getRingBuffer().removeGatingSequence(sequence);
                asyncListenersIterator.remove();
            }
        }
    }

    protected static class EventProcessorExceptionHandler implements ExceptionHandler {
        private static final EventProcessorExceptionHandler INSTANCE = new EventProcessorExceptionHandler();

        private EventProcessorExceptionHandler() {
        }

        @Override
        public void handleEventException( Throwable ex, long sequence, Object event ) {
            ChangeSet data = ((ChangeSetEvent)event).getData();
            if (ex instanceof InterruptedException) {
                LOGGER.debug(ex, "Interrupted exception from event processor for event {0}", event);
            } else {
                LOGGER.error(ex, BusI18n.errorProcessingAsyncEvent, data.toString(), sequence);
            }
        }

        @Override
        public void handleOnStartException( Throwable ex ) {
            LOGGER.error(ex, BusI18n.errorInitializingEventProcessor);

        }

        @Override
        public void handleOnShutdownException( Throwable ex ) {
            LOGGER.error(ex, BusI18n.errorInitializingEventProcessor);
        }
    }

    protected static class AsyncChangeSetListener {
        private EventProcessor eventProcessor;
        private Future<?> future;
        private ChangeSetListener listener;

        protected AsyncChangeSetListener( EventProcessor eventProcessor,
                                          Future<?> future,
                                          ChangeSetListener listener ) {
            this.eventProcessor = eventProcessor;
            this.future = future;
            this.listener = listener;
        }

        protected Sequence getSequence() {
            return eventProcessor != null ? eventProcessor.getSequence() : null;
        }

        protected ChangeSetListener getListener() {
            return listener;
        }

        protected void stop() {
            eventProcessor.halt();
            future.cancel(false);
            eventProcessor = null;
            future = null;
            listener = null;
        }

        @Override
        public boolean equals( Object o ) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AsyncChangeSetListener)) {
                return false;
            }

            AsyncChangeSetListener that = (AsyncChangeSetListener)o;
            return listener.equals(that.listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }

    protected static class ChangeSetEvent {
        private ChangeSet data;

        protected ChangeSetEvent() {
        }

        protected void setData( ChangeSet data ) {
            this.data = data;
        }

        protected ChangeSet getData() {
            return data;
        }
    }

    protected static class ChangeSetHandler implements WorkHandler<ChangeSetEvent> {
        private final ChangeSetListener listener;

        protected ChangeSetHandler( ChangeSetListener listener ) {
            this.listener = listener;
        }

        @Override
        public void onEvent( ChangeSetEvent event ) throws Exception {
            listener.notify(event.getData());
        }
    }
}
