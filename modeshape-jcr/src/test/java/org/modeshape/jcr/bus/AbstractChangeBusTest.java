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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * Base class for different {@link org.modeshape.jcr.bus.ChangeBus} implementations.
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractChangeBusTest {

    protected static final String WORKSPACE1 = "ws1";
    protected static final String WORKSPACE2 = "ws2";

    private ChangeBus changeBus;

    @Before
    public void beforeEach() throws Exception {
        changeBus = createRepositoryChangeBus();
        changeBus.start();
    }

    protected abstract ChangeBus createRepositoryChangeBus() throws Exception;

    @After
    public void afterEach() {
        changeBus.shutdown();
    }

    @Test
    public void shouldNotAllowTheSameListenerTwice() throws Exception {
        TestListener listener1 = new TestListener();

        assertTrue(getChangeBus().register(listener1));
        assertFalse(getChangeBus().register(listener1));

        TestListener listener2 = new TestListener();
        assertTrue(getChangeBus().register(listener2));
        assertFalse(getChangeBus().register(listener2));

        assertFalse(getChangeBus().register(null));
    }

    @Test
    public void shouldAllowListenerRemoval() throws Exception {
        TestListener listener1 = new TestListener();

        assertTrue(getChangeBus().register(listener1));
        assertTrue(getChangeBus().unregister(listener1));

        TestListener listener2 = new TestListener();
        assertFalse(getChangeBus().unregister(listener2));
    }

    @Test
    public void shouldNotifyAllRegisteredListenersKeepingEventOrder() throws Exception {
        TestListener listener1 = new TestListener(4);
        getChangeBus().register(listener1);

        TestListener listener2 = new TestListener(4);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldOnlyDispatchEventsAfterListenerRegistration() throws Exception {
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        TestListener listener1 = new TestListener(4);
        getChangeBus().register(listener1);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        TestListener listener2 = new TestListener(2);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldDispatchEventsIfWorkspaceNameIsMissing() throws Exception {
        TestListener listener = new TestListener(2);
        getChangeBus().register(listener);

        getChangeBus().notify(new TestChangeSet(null));
        getChangeBus().notify(new TestChangeSet(null));

        assertChangesDispatched(listener);
    }

    @Test
    public void shouldNotDispatchEventsAfterListenerRemoval() throws Exception {
        TestListener listener1 = new TestListener(3);
        getChangeBus().register(listener1);

        TestListener listener2 = new TestListener(2);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        Thread.sleep(50);

        getChangeBus().unregister(listener2);
        Thread.sleep(50);

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldNotDispatchEventsIfShutdown() throws Exception {
        TestListener listener = new TestListener(1);
        getChangeBus().register(listener);
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        Thread.sleep(50);

        getChangeBus().shutdown();
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        assertChangesDispatched(listener);
    }

    @Test
    @Ignore( "This is a perf test" )
    public void shouldNotifyLotsOfConsumersAsync() throws Exception {
        int eventsPerBatch = 300000;
        int listenersPerBatch = 30;
        int batches = 4;
        List<AbstractChangeBusTest.TestListener> listeners = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < batches; i++) {
            listeners.addAll(submitBatch(eventsPerBatch, listenersPerBatch, (batches - i) * eventsPerBatch));
            Thread.sleep(50);
        }

        for (AbstractChangeBusTest.TestListener listener : listeners) {
            listener.assertExpectedEventsCount();
        }
        System.out.println("Elapsed: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " millis");
    }

    private List<AbstractChangeBusTest.TestListener> submitBatch( int eventCount,
                                                                  int listenerCount,
                                                                  int expectedEventsCount ) throws Exception {
        List<AbstractChangeBusTest.TestListener> listeners = new ArrayList<>();
        for (int i = 0; i < listenerCount; i++) {
            AbstractChangeBusTest.TestListener listener = new AbstractChangeBusTest.TestListener(expectedEventsCount, 500);
            listeners.add(listener);
            getChangeBus().register(listener);
        }

        for (int i = 0; i < eventCount; i++) {
            getChangeBus().notify(new AbstractChangeBusTest.TestChangeSet());
        }
        return listeners;
    }

    protected ChangeBus getChangeBus() throws Exception {
        return changeBus;
    }

    protected void assertChangesDispatched( TestListener listener ) {
        listener.assertExpectedEventsCount();

        List<TestChangeSet> receivedChanges = listener.getObservedChangeSet();
        Map<String, List<Long>> changeSetsPerWs = new HashMap<>();

        for (TestChangeSet changeSet : receivedChanges) {
            String wsName = changeSet.getWorkspaceName();
            List<Long> receivedTimes = changeSetsPerWs.get(wsName);
            if (receivedTimes == null) {
                receivedTimes = new ArrayList<>();
                changeSetsPerWs.put(wsName, receivedTimes);
            }
            for (Long receivedTime : receivedTimes) {
                assertTrue(receivedTime <= changeSet.time());
            }
            receivedTimes.add(changeSet.time());
        }
    }

    protected static class TestChangeSet implements ChangeSet {

        private static final long serialVersionUID = 1L;

        private final String workspaceName;
        private final long time;
        private final String uuid = UUID.randomUUID().toString();

        public TestChangeSet() {
            this(UUID.randomUUID().toString());
        }

        protected TestChangeSet( String workspaceName ) {
            this.workspaceName = workspaceName;
            this.time = System.currentTimeMillis();
        }

        @Override
        public Set<NodeKey> changedNodes() {
            return Collections.emptySet();
        }

        @Override
        public Set<BinaryKey> unusedBinaries() {
            return Collections.emptySet();
        }

        @Override
        public Set<BinaryKey> usedBinaries() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasBinaryChanges() {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public Map<String, String> getUserData() {
            return Collections.emptyMap();
        }

        @Override
        public DateTime getTimestamp() {
            return new JodaDateTime(time);
        }

        public long time() {
            return time;
        }

        @Override
        public String getProcessKey() {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public String getRepositoryKey() {
            return null;
        }

        @Override
        public String getWorkspaceName() {
            return workspaceName;
        }

        @Override
        public Iterator<Change> iterator() {
            return Collections.<Change>emptySet().iterator();
        }

        @Override
        public String getJournalId() {
            return null;
        }

        @Override
        public String getUUID() {
            return uuid;
        }

        @Override
        public boolean equals( Object o ) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestChangeSet)) {
                return false;
            }

            TestChangeSet changes = (TestChangeSet)o;

            if (time != changes.time) {
                return false;
            }
            if (!workspaceName.equals(changes.workspaceName)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = workspaceName.hashCode();
            result = 31 * result + (int)(time ^ (time >>> 32));
            return result;
        }
    }

    protected static class TestListener implements ChangeSetListener {
        private final List<TestChangeSet> receivedChangeSet;
        private final long timeoutMillis;
        private int expectedNumberOfEvents;
        private CountDownLatch latch;

        protected TestListener() {
            this(0, 350);
        }

        protected TestListener( int expectedNumberOfEvents ) {
            this(expectedNumberOfEvents, 350);
        }

        protected TestListener( int expectedNumberOfEvents,
                                long timeoutMillis ) {
            this.latch = new CountDownLatch(expectedNumberOfEvents);
            this.receivedChangeSet = new ArrayList<>();
            this.timeoutMillis = timeoutMillis;
            this.expectedNumberOfEvents = expectedNumberOfEvents;
        }

        public void expectChangeSet( int expectedNumberOfEvents ) {
            this.latch = new CountDownLatch(expectedNumberOfEvents);
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            receivedChangeSet.clear();
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (!(changeSet instanceof TestChangeSet)) {
                throw new IllegalArgumentException("Invalid type of change set received");
            }
            receivedChangeSet.add((TestChangeSet)changeSet);
            latch.countDown();
        }

        public void assertExpectedEventsCount() {
            try {
                assertTrue("Not enough events received", latch.await(timeoutMillis, TimeUnit.MILLISECONDS));
                assertEquals("Incorrect number of events received", expectedNumberOfEvents, receivedChangeSet.size());
            } catch (InterruptedException e) {
                Thread.interrupted();
                fail("Interrupted while waiting to verify event count");
            }
        }

        public List<TestChangeSet> getObservedChangeSet() {
            return new ArrayList<>(receivedChangeSet);
        }
    }
}
