package org.modeshape.jcr;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.basic.JodaDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Unit test for {@link RepositoryChangeBus}
 * 
 * @author Horia Chiorean
 */
public class RepositoryChangeBusTest {

    private static final String WORKSPACE1 = "ws1";
    private static final String WORKSPACE2 = "ws2";

    private RepositoryChangeBus changeBus;

    @Before
    public void beforeEach() {
        changeBus = new RepositoryChangeBus(Executors.newCachedThreadPool(), null, false);
    }

    @After
    public void afterEach() {
        changeBus.shutdown();
    }
    
    @Test
    public void shouldNotAllowTheSameListenerTwice() {
        TestListener listener1 = new TestListener();

        assertTrue(changeBus.register(listener1));
        assertFalse(changeBus.register(listener1));

        TestListener listener2 = new TestListener();
        assertTrue(changeBus.register(listener2));
        assertFalse(changeBus.register(listener2));

        assertFalse(changeBus.register(null));
    }

    @Test
    public void shouldAllowListenerRemoval() {
        TestListener listener1 = new TestListener();

        assertTrue(changeBus.register(listener1));
        assertTrue(changeBus.unregister(listener1));

        TestListener listener2 = new TestListener();
        assertFalse(changeBus.unregister(listener2));
    }

    @Test
    public void shouldNotifyAllRegisteredListenersKeepingEventOrder() throws Exception {
        TestListener listener1 = new TestListener();
        changeBus.register(listener1);
        TestListener listener2 = new TestListener();
        changeBus.register(listener2);

        changeBus.notify(new TestChangeSet(WORKSPACE1));
        changeBus.notify(new TestChangeSet(WORKSPACE1));

        changeBus.notify(new TestChangeSet(WORKSPACE2));
        changeBus.notify(new TestChangeSet(WORKSPACE2));

        waitForChangesToArrive();

        assertChangesDispatched(listener1, 4);
        assertChangesDispatched(listener2, 4);
    }

    @Test
    public void shouldOnlyDispatchEventsAfterListenerRegistration() throws Exception {
        changeBus.notify(new TestChangeSet(WORKSPACE1));
        
        TestListener listener1 = new TestListener();
        changeBus.register(listener1);

        changeBus.notify(new TestChangeSet(WORKSPACE1));
        changeBus.notify(new TestChangeSet(WORKSPACE1));

        TestListener listener2 = new TestListener();
        changeBus.register(listener2);

        changeBus.notify(new TestChangeSet(WORKSPACE2));
        changeBus.notify(new TestChangeSet(WORKSPACE2));

        waitForChangesToArrive();

        assertChangesDispatched(listener1, 4);
        assertChangesDispatched(listener2, 2);
    }

    @Test
    public void shouldDispatchEventsIfWorkspaceNameIsMissing() throws Exception {
        TestListener listener = new TestListener();
        changeBus.register(listener);

        changeBus.notify(new TestChangeSet(null));
        changeBus.notify(new TestChangeSet(null));

        waitForChangesToArrive();

        assertChangesDispatched(listener, 2);
    }
    
    @Test
    public void shouldNotDispatchEventsAfterListenerRemoval() throws Exception {
        TestListener listener1 = new TestListener();
        changeBus.register(listener1);
        TestListener listener2 = new TestListener();
        changeBus.register(listener2);
        
        changeBus.notify(new TestChangeSet(WORKSPACE1));
        changeBus.notify(new TestChangeSet(WORKSPACE2));

        changeBus.unregister(listener2);

        changeBus.notify(new TestChangeSet(WORKSPACE2));

        waitForChangesToArrive();

        assertChangesDispatched(listener1, 3);
        assertChangesDispatched(listener2, 2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotDispatchEventsIfShutdown() throws Exception {
        TestListener listener = new TestListener();
        changeBus.register(listener);
        
        changeBus.notify(new TestChangeSet(WORKSPACE1));
        
        changeBus.shutdown();
        
        changeBus.notify(new TestChangeSet(WORKSPACE2));

        waitForChangesToArrive();

        assertChangesDispatched(listener, 1);
    }

    private void waitForChangesToArrive() throws InterruptedException {
        Thread.sleep(500);
    }

    private void assertChangesDispatched( TestListener listener,
                                          int changesCount ) {
        List<TestChangeSet> receivedChanges = listener.getReceivedChanges();
        assertEquals(changesCount, receivedChanges.size());

        Map<String, List<Long>> changeSetsPerWs = new HashMap<String, List<Long>>();

        for (TestChangeSet changeSet : receivedChanges) {
            String wsName = changeSet.getWorkspaceName();
            List<Long> receivedTimes = changeSetsPerWs.get(wsName);
            if (receivedTimes == null) {
                receivedTimes = new ArrayList<Long>();
                changeSetsPerWs.put(wsName, receivedTimes);
            }
            for (Long receivedTime : receivedTimes) {
                assertTrue(receivedTime <= changeSet.getTimestamp().getMilliseconds());
            }
            receivedTimes.add(changeSet.getTimestamp().getMilliseconds());
        }
    }

    private class TestChangeSet implements ChangeSet {

        private final String workspaceName;
        private final DateTime dateTime;

        private TestChangeSet( String workspaceName ) {
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
    }

    private class TestListener implements ChangeSetListener {
        private final List<TestChangeSet> receivedChanges;

        private TestListener() {
            receivedChanges = new ArrayList<TestChangeSet>();
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            assertTrue(changeSet instanceof TestChangeSet);
            TestChangeSet testChangeSet = (TestChangeSet)changeSet;
            receivedChanges.add(testChangeSet);
        }

        public List<TestChangeSet> getReceivedChanges() {
            return receivedChanges;
        }
    }
}
