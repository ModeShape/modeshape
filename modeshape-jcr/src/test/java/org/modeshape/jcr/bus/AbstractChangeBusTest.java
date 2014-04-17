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
    @Ignore("This is just a perf test")
    public void shouldNotifyAllRegisteredListenersWithLotsOfEvents() throws Exception {
        int eventCount = 1000000;
        int listenerCount = 100;

        List<TestListener> listeners = new ArrayList<>();
        for (int i = 0; i < listenerCount; i++) {
            TestListener listener = new TestListener(eventCount, TimeUnit.SECONDS.toMillis(10));
            listeners.add(listener);
            getChangeBus().register(listener);
        }

        long start = System.nanoTime();

        for (int i = 0; i < eventCount; i++) {
            getChangeBus().notify(new TestChangeSet());
        }
        for (TestListener listener : listeners) {
            assertTrue(listener.await());
            assertEquals(eventCount, listener.getObservedChangeSet().size());
        }
        System.out.println("Elapsed: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " millis");
    }

    protected ChangeBus getChangeBus() throws Exception {
        return changeBus;
    }

    protected void assertChangesDispatched( TestListener listener ) throws InterruptedException {
        assertTrue("Changes not dispatched to listener", listener.await());

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
                assertTrue(receivedTime <= changeSet.getTimestamp().getMilliseconds());
            }
            receivedTimes.add(changeSet.getTimestamp().getMilliseconds());
        }
    }

    protected static class TestChangeSet implements ChangeSet {

        private static final long serialVersionUID = 1L;

        private final String workspaceName;
        private final DateTime dateTime;

        public TestChangeSet() {
            this(UUID.randomUUID().toString());
        }

        protected TestChangeSet( String workspaceName ) {
            this.workspaceName = workspaceName;
            this.dateTime = new JodaDateTime(System.currentTimeMillis());
        }

        @Override
        public Set<NodeKey> changedNodes() {
            return Collections.emptySet();
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
            return dateTime;
        }

        @Override
        public String getProcessKey() {
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
            return null;
        }

        @Override
        public boolean equals( Object o ) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestChangeSet changes = (TestChangeSet)o;

            if (!dateTime.equals(changes.dateTime)) return false;
            if (!workspaceName.equals(changes.workspaceName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = workspaceName.hashCode();
            result = 31 * result + dateTime.hashCode();
            return result;
        }
    }

    protected static class TestListener implements ChangeSetListener {
        private final List<TestChangeSet> receivedChangeSet;
        private final long timeoutMillis;
        private CountDownLatch latch;

        public TestListener() {
            this(0, 350);
        }

        protected TestListener( int expectedNumberOfChangeSets ) {
            this(expectedNumberOfChangeSets, 350);
        }

        protected TestListener( int expectedNumberOfChangeSets, long timeoutMillis ) {
            latch = new CountDownLatch(expectedNumberOfChangeSets);
            receivedChangeSet = new ArrayList<>();
            this.timeoutMillis = timeoutMillis;
        }

        public void expectChangeSet( int expectedNumberOfChangeSet ) {
            latch = new CountDownLatch(expectedNumberOfChangeSet);
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

        public boolean await() throws InterruptedException {
            return latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        public List<TestChangeSet> getObservedChangeSet() {
            return new ArrayList<>(receivedChangeSet);
        }
    }
}
