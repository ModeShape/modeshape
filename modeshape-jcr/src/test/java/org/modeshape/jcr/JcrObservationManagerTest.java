/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.api.observation.AddEventListenerTest;
import org.apache.jackrabbit.test.api.observation.EventIteratorTest;
import org.apache.jackrabbit.test.api.observation.EventTest;
import org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest;
import org.apache.jackrabbit.test.api.observation.LockingTest;
import org.apache.jackrabbit.test.api.observation.NodeAddedTest;
import org.apache.jackrabbit.test.api.observation.NodeMovedTest;
import org.apache.jackrabbit.test.api.observation.NodeRemovedTest;
import org.apache.jackrabbit.test.api.observation.NodeReorderTest;
import org.apache.jackrabbit.test.api.observation.PropertyAddedTest;
import org.apache.jackrabbit.test.api.observation.PropertyChangedTest;
import org.apache.jackrabbit.test.api.observation.PropertyRemovedTest;
import org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.DateTime;
import org.modeshape.jcr.JcrObservationManager.JcrEventBundle;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.JcrRepository.VersionHistoryOption;

/**
 * The {@link JcrObservationManager} test class.
 */
public final class JcrObservationManagerTest extends TestSuite {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                                          | Event.PROPERTY_REMOVED;

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    private static final String LOCK_MIXIN = "mix:lockable"; // extends referenceable
    private static final String LOCK_OWNER = "jcr:lockOwner"; // property
    private static final String LOCK_IS_DEEP = "jcr:lockIsDeep"; // property
    // private static final String NT_BASE = "nt:base";
    private static final String REF_MIXIN = "mix:referenceable";
    private static final String UNSTRUCTURED = "nt:unstructured";
    private static final String USER_ID = "superuser";

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    @BeforeClass
    public static void beforeClass() {
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================
    protected static final String WORKSPACE = "ws1";
    protected static final String WORKSPACE2 = "ws2";
    protected static final String REPOSITORY = "r1";
    protected static final String SOURCE = "store";

    private JcrEngine engine;
    private Session session;
    private Node testRootNode;
    private List<Session> sessions;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    TestListener addListener( int eventsExpected,
                              int eventTypes,
                              String absPath,
                              boolean isDeep,
                              String[] uuids,
                              String[] nodeTypeNames,
                              boolean noLocal ) throws Exception {
        return addListener(eventsExpected, 1, eventTypes, absPath, isDeep, uuids, nodeTypeNames, noLocal);
    }

    TestListener addListener( int eventsExpected,
                              int numIterators,
                              int eventTypes,
                              String absPath,
                              boolean isDeep,
                              String[] uuids,
                              String[] nodeTypeNames,
                              boolean noLocal ) throws Exception {
        return addListener(this.session, eventsExpected, numIterators, eventTypes, absPath, isDeep, uuids, nodeTypeNames, noLocal);
    }

    TestListener addListener( Session session,
                              int eventsExpected,
                              int eventTypes,
                              String absPath,
                              boolean isDeep,
                              String[] uuids,
                              String[] nodeTypeNames,
                              boolean noLocal ) throws Exception {
        return addListener(session, eventsExpected, 1, eventTypes, absPath, isDeep, uuids, nodeTypeNames, noLocal);
    }

    TestListener addListener( Session session,
                              int eventsExpected,
                              int numIterators,
                              int eventTypes,
                              String absPath,
                              boolean isDeep,
                              String[] uuids,
                              String[] nodeTypeNames,
                              boolean noLocal ) throws Exception {
        TestListener listener = new TestListener(eventsExpected, numIterators, eventTypes);
        session.getWorkspace().getObservationManager().addEventListener(listener,
                                                                        eventTypes,
                                                                        absPath,
                                                                        isDeep,
                                                                        uuids,
                                                                        nodeTypeNames,
                                                                        noLocal);
        return listener;
    }

    @After
    public void afterEach() {
        try {
            for (Session session : sessions) {
                if (session != null && session.isLive()) session.logout();
            }
        } finally {
            this.session = null;
            this.sessions.clear();
            try {
                this.engine.shutdown();
            } finally {
                this.engine = null;
            }
        }
    }

    @Before
    public void beforeEach() throws RepositoryException {
        sessions = new ArrayList<Session>();

        JcrConfiguration config = new JcrConfiguration();
        config.repositorySource("store")
              .usingClass(InMemoryRepositorySource.class)
              .setRetryLimit(100)
              .setProperty("predefinedWorkspaceNames", new String[] {WORKSPACE, WORKSPACE2})
              .setProperty("defaultWorkspaceName", WORKSPACE);
        config.repository(REPOSITORY).setSource(SOURCE).setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        config.save();

        // Create and start the engine ...
        this.engine = config.build();
        this.engine.start();

        // Create repository and session
        this.session = login(REPOSITORY, WORKSPACE, USER_ID, USER_ID.toCharArray());

        this.testRootNode = this.session.getRootNode().addNode("testroot", UNSTRUCTURED);
        save();
    }

    protected Session login( String repositoryName,
                             String workspaceName,
                             String userId,
                             char[] password ) throws RepositoryException {
        Repository repository = this.engine.getRepository(repositoryName);
        Credentials credentials = new SimpleCredentials(userId, password);
        Session session = repository.login(credentials, workspaceName);
        if (session != null) {
            sessions.add(session);
        }
        return session;
    }

    void checkResults( TestListener listener ) {
        if (listener.getActualEventCount() != listener.getExpectedEventCount()) {
            // Wrong number ...
            StringBuilder sb = new StringBuilder(" Actual events were: ");
            for (Event event : listener.getEvents()) {
                sb.append('\n').append(event);
            }
            assertThat("Received incorrect number of events." + sb.toString(),
                       listener.getActualEventCount(),
                       is(listener.getExpectedEventCount()));
            assertThat(listener.getErrorMessage(), listener.getErrorMessage(), is(nullValue()));
        }
    }

    boolean containsPath( TestListener listener,
                          String path ) throws Exception {
        for (Event event : listener.getEvents()) {
            if (event.getPath().equals(path)) return true;
        }

        return false;
    }

    ObservationManager getObservationManager() throws RepositoryException {
        return this.session.getWorkspace().getObservationManager();
    }

    Node getRoot() {
        return this.testRootNode;
    }

    Workspace getWorkspace() {
        return this.session.getWorkspace();
    }

    void removeListener( TestListener listener ) throws Exception {
        this.session.getWorkspace().getObservationManager().removeEventListener(listener);
    }

    void save() throws RepositoryException {
        this.session.save();
    }

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see AddEventListenerTest#testUUID()
     */
    @Test
    public void shouldNotReceiveEventIfUuidDoesNotMatch() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        n1.addMixin(REF_MIXIN);
        save();

        // register listener
        TestListener listener = addListener(0,
                                            Event.PROPERTY_ADDED,
                                            getRoot().getPath(),
                                            true,
                                            new String[] {UUID.randomUUID().toString()},
                                            null,
                                            false);

        // create properties
        n1.setProperty("prop1", "foo");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
    }

    @Test
    public void shouldNotReceiveEventIfNodeTypeDoesNotMatch() throws Exception {
        // setup
        Node node1 = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(0, ALL_EVENTS, null, false, null, new String[] {REF_MIXIN}, false);

        // create event triggers
        node1.setProperty("newProperty", "newValue"); // node1 is NOT referenceable
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
    }

    @Test
    public void shouldReceiveNodeAddedEventWhenRegisteredToReceiveAllEvents() throws Exception {
        // register listener (add + 3 property events)
        Node root = getRoot();
        save();
        TestListener listener = addListener(4, ALL_EVENTS, null, false, null, null, false);

        // add node
        Node addedNode = root.addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + addedNode.getPath(), containsPath(listener, addedNode.getPath()));
    }

    @Test
    public void shouldReceiveNodeRemovedEventWhenRegisteredToReceiveAllEvents() throws Exception {
        // add the node that will be removed
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // register listener (add + 3 property events)
        TestListener listener = addListener(1, ALL_EVENTS, null, false, null, null, false);

        // remove node
        String path = addedNode.getPath();
        addedNode.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for removed node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected=" + path,
                   containsPath(listener, path));
    }

    @Test
    public void shouldReceivePropertyAddedEventWhenRegisteredToReceiveAllEvents() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(1, ALL_EVENTS, null, false, null, null, false);

        // add the property
        Property prop1 = node.setProperty("prop1", "prop1 content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + prop1.getPath(), containsPath(listener, prop1.getPath()));
    }

    @Test
    public void shouldReceivePropertyChangedEventWhenRegisteredToReceiveAllEvents() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop1 = node.setProperty("prop1", "prop1 content");
        save();

        // register listener
        TestListener listener = addListener(1, ALL_EVENTS, null, false, null, null, false);

        // change the property
        prop1.setValue("prop1 modified content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for changed property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + prop1.getPath(), containsPath(listener, prop1.getPath()));
    }

    @Test
    public void shouldReceivePropertyRemovedEventWhenRegisteredToReceiveAllEvents() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop = node.setProperty("prop1", "prop1 content");
        String propPath = prop.getPath();
        save();

        // register listener
        TestListener listener = addListener(1, ALL_EVENTS, null, false, null, null, false);

        // remove the property
        prop.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for removed property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + propPath, containsPath(listener, propPath));
    }

    @Test
    public void shouldReceivePropertyAddedEventWhenRegisteredToReceiveEventsBasedUponNodeTypeName() throws Exception {
        // register listener
        String[] nodeTypeNames = {"mode:root"};
        TestListener listener = addListener(1, ALL_EVENTS, null, true, null, nodeTypeNames, false);

        // add the property
        this.session.getRootNode().setProperty("fooProp", "bar");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        String propPath = "/fooProp";
        assertTrue("Path for added property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected=" + propPath,
                   containsPath(listener, propPath));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.EventIteratorTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see EventIteratorTest#testGetPosition()
     */
    @Test
    public void shouldTestEventIteratorTest_testGetPosition() throws Exception {
        // register listener
        TestListener listener = addListener(3, Event.NODE_ADDED, null, false, null, null, false);

        // add nodes to generate events
        getRoot().addNode("node1", UNSTRUCTURED);
        getRoot().addNode("node2", UNSTRUCTURED);
        getRoot().addNode("node3", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
    }

    /**
     * @throws Exception
     * @see EventIteratorTest#testGetSize()
     */
    @Test
    public void shouldTestEventIteratorTest_testGetSize() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add node to generate event
        getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
    }

    /**
     * @throws Exception
     * @see EventIteratorTest#testSkip()
     */
    @Test
    public void shouldTestEventIteratorTest_testSkip() throws Exception {
        // create events
        List<Event> events = new ArrayList<Event>();
        DateTime now = engine.getExecutionContext().getValueFactories().getDateFactory().create();
        JcrEventBundle bundle = ((JcrObservationManager)getObservationManager()).new JcrEventBundle(now, "userId", null);
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        String id3 = UUID.randomUUID().toString();
        events.add(((JcrObservationManager)getObservationManager()).new JcrEvent(bundle, Event.NODE_ADDED, "/testroot/node1", id1));
        events.add(((JcrObservationManager)getObservationManager()).new JcrEvent(bundle, Event.NODE_ADDED, "/testroot/node2", id2));
        events.add(((JcrObservationManager)getObservationManager()).new JcrEvent(bundle, Event.NODE_ADDED, "/testroot/node3", id3));

        // create iterator
        EventIterator itr = ((JcrObservationManager)getObservationManager()).new JcrEventIterator(events);

        // tests
        itr.skip(0); // skip zero elements
        assertThat("getPosition() for first element should return 0.", itr.getPosition(), is(0L));

        itr.skip(2); // skip one element
        assertThat("Wrong value when skipping ", itr.getPosition(), is(2L));

        try {
            itr.skip(2); // skip past end
            fail("EventIterator must throw NoSuchElementException when skipping past the end");
        } catch (NoSuchElementException e) {
            // success
        }
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.EventTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see EventTest#testGetNodePath()
     */
    @Test
    public void shouldTestEventTest_testGetNodePath() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add node to generate event
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path added node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + addedNode.getPath(), containsPath(listener, addedNode.getPath()));
    }

    /**
     * @throws Exception
     * @see EventTest#testGetType()
     */
    @Test
    public void shouldTestEventTest_testGetType() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add node to generate event
        getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertThat("Event did not return correct event type", listener.getEvents().get(0).getType(), is(Event.NODE_ADDED));
    }

    /**
     * @throws Exception
     * @see EventTest#testGetUserId()
     */
    @Test
    public void shouldTestEventTest_testGetUserId() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add a node to generate event
        getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertThat("UserId of event is not equal to userId of session", listener.getEvents().get(0).getUserID(), is(USER_ID));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see GetRegisteredEventListenersTest#testGetSize()
     */
    @Test
    public void shouldTestGetRegisteredEventListenersTest_testGetSize() throws Exception {
        assertThat("A new session must not have any event listeners registered.",
                   getObservationManager().getRegisteredEventListeners().getSize(),
                   is(0L));

        // register listener
        TestListener listener = addListener(0, ALL_EVENTS, null, false, null, null, false);
        addListener(0, ALL_EVENTS, null, false, null, null, false);
        assertThat("Wrong number of event listeners.", getObservationManager().getRegisteredEventListeners().getSize(), is(2L));

        // make sure same listener isn't added again
        getObservationManager().addEventListener(listener, ALL_EVENTS, null, false, null, null, false);
        assertThat("The same listener should not be added more than once.", getObservationManager().getRegisteredEventListeners()
                                                                                                   .getSize(), is(2L));
    }

    /**
     * @throws Exception
     * @see GetRegisteredEventListenersTest#testRemoveEventListener()
     */
    @Test
    public void shouldTestGetRegisteredEventListenersTest_testRemoveEventListener() throws Exception {
        TestListener listener1 = addListener(0, ALL_EVENTS, null, false, null, null, false);
        EventListener listener2 = addListener(0, ALL_EVENTS, null, false, null, null, false);
        assertThat("Wrong number of event listeners.", getObservationManager().getRegisteredEventListeners().getSize(), is(2L));

        // now remove
        removeListener(listener1);
        assertThat("Wrong number of event listeners after removing a listener.",
                   getObservationManager().getRegisteredEventListeners().getSize(),
                   is(1L));
        assertThat("Wrong number of event listeners after removing a listener.",
                   getObservationManager().getRegisteredEventListeners().nextEventListener(),
                   is(listener2));

    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.LockingTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see LockingTest#testAddLockToNode()
     */
    @Test
    public void shouldTestLockingTest_testAddLockToNode() throws Exception {
        // setup
        String node1 = "node1";
        Node lockable = getRoot().addNode(node1, UNSTRUCTURED);
        lockable.addMixin(LOCK_MIXIN);
        save();

        // register listener
        TestListener listener = addListener(2, 2, Event.PROPERTY_ADDED, testRootNode.getPath(), true, null, null, false);

        // lock node (no save needed)
        session.getWorkspace().getLockManager().lock(lockable.getPath(), false, true, 1L, "me");

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("No event created for " + LOCK_OWNER, containsPath(listener, lockable.getPath() + '/' + LOCK_OWNER));
        assertTrue("No event created for " + LOCK_IS_DEEP, containsPath(listener, lockable.getPath() + '/' + LOCK_IS_DEEP));
    }

    /**
     * @throws Exception
     * @see LockingTest#testRemoveLockFromNode()
     */
    @Test
    public void shouldTestLockingTest_testRemoveLockFromNode() throws Exception {
        // setup
        String node1 = "node1";
        Node lockable = getRoot().addNode(node1, UNSTRUCTURED);
        lockable.addMixin(LOCK_MIXIN);
        save();
        session.getWorkspace().getLockManager().lock(lockable.getPath(), false, true, 1L, "me");

        // register listener
        TestListener listener = addListener(2, Event.PROPERTY_REMOVED, null, false, null, null, false);

        // lock node (no save needed)
        session.getWorkspace().getLockManager().unlock(lockable.getPath());

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("No event created for " + LOCK_OWNER, containsPath(listener, lockable.getPath() + '/' + LOCK_OWNER));
        assertTrue("No event created for " + LOCK_IS_DEEP, containsPath(listener, lockable.getPath() + '/' + LOCK_IS_DEEP));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.NodeAddedTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see NodeAddedTest#testMultipleNodeAdded1()
     */
    @Test
    public void shouldTestNodeAddedTest_testMultipleNodeAdded1() throws Exception {
        // register listener
        TestListener listener = addListener(2, Event.NODE_ADDED, null, false, null, null, false);

        // add a couple sibling nodes
        Node addedNode1 = getRoot().addNode("node1", UNSTRUCTURED);
        Node addedNode2 = getRoot().addNode("node2", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for first added node is wrong", containsPath(listener, addedNode1.getPath()));
        assertTrue("Path for second added node is wrong", containsPath(listener, addedNode2.getPath()));
    }

    /**
     * @throws Exception
     * @see NodeAddedTest#testMultipleNodeAdded2()
     */
    @Test
    public void shouldTestNodeAddedTest_testMultipleNodeAdded2() throws Exception {
        // register listener
        TestListener listener = addListener(2, Event.NODE_ADDED, null, false, null, null, false);

        // add node and child node
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        Node addedChildNode = addedNode.addNode("node2", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added node is wrong", containsPath(listener, addedNode.getPath()));
        assertTrue("Path for added child node is wrong", containsPath(listener, addedChildNode.getPath()));
    }

    /**
     * @throws Exception
     * @see NodeAddedTest#testSingleNodeAdded()
     */
    @Test
    public void shouldTestNodeAddedTest_testSingleNodeAdded() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add node
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + addedNode.getPath(), containsPath(listener, addedNode.getPath()));
    }

    /**
     * @throws Exception
     * @see NodeAddedTest#testTransientNodeAddedRemoved()
     */
    @Test
    public void shouldTestNodeAddedTest_testTransientNodeAddedRemoved() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);

        // add a child node and immediately remove it
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        Node transientNode = addedNode.addNode("node2", UNSTRUCTURED); // should not get this event because of the following
        // remove
        transientNode.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + addedNode.getPath(), containsPath(listener, addedNode.getPath()));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.NodeRemovedTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see NodeRemovedTest#testMultiNodesRemoved()
     */
    @Test
    @Ignore
    public void shouldTestNodeRemovedTest_testMultiNodesRemoved() throws Exception {
        // register listener
        TestListener listener = addListener(2, Event.NODE_REMOVED, null, false, null, null, false);

        // add nodes to be removed
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        Node childNode = addedNode.addNode("node2", UNSTRUCTURED);
        save();

        // remove parent node which removes child node
        String parentPath = addedNode.getPath();
        String childPath = childNode.getPath();
        addedNode.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for removed node is wrong", containsPath(listener, parentPath));
        assertTrue("Path for removed child node is wrong", containsPath(listener, childPath));
    }

    /**
     * @throws Exception
     * @see NodeRemovedTest#testSingleNodeRemoved()
     */
    @Test
    public void shouldTestNodeRemovedTest_testSingleNodeRemoved() throws Exception {
        // register listener
        TestListener listener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // add the node that will be removed
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // remove node
        String path = addedNode.getPath();
        addedNode.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for removed node is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected=" + path,
                   containsPath(listener, path));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.NodeMovedTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see NodeMovedTest#testMoveNode()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeMovedTest_testMoveNode() throws Exception {
        // setup
        String node1 = "node1";
        String node2 = "node2";
        Node n1 = getRoot().addNode(node1, UNSTRUCTURED);
        Node n2 = n1.addNode(node2, UNSTRUCTURED);
        String oldPath = n2.getPath();
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // move node
        String newPath = getRoot().getPath() + '/' + node2;
        getWorkspace().move(oldPath, newPath);
        save();

        // event handling
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(oldPath));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(newPath));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(moveNodeListener, newPath));
        assertTrue("Path for new location of added node is wrong: actual=" + addNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(addNodeListener, newPath));
        assertTrue("Path for new location of removed node is wrong: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + oldPath, containsPath(removeNodeListener, oldPath));
    }

    /**
     * @throws Exception
     * @see NodeMovedTest#testMoveTree()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeMovedTest_testMoveTree() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        String oldPath = n1.getPath();
        n1.addNode("node2", UNSTRUCTURED);
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // move node
        String newPath = getRoot().getPath() + "/node3";
        getWorkspace().move(oldPath, newPath);
        save();

        // event handling
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(oldPath));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(newPath));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(moveNodeListener, newPath));
        assertTrue("Path for new location of added node is wrong: actual=" + addNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(addNodeListener, newPath));
        assertTrue("Path for new location of removed node is wrong: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + oldPath, containsPath(removeNodeListener, oldPath));
    }

    /**
     * @throws Exception
     * @see NodeMovedTest#testMoveWithRemove()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeMovedTest_testMoveWithRemove() throws Exception {
        // setup
        String node2 = "node2";
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        Node n2 = n1.addNode(node2, UNSTRUCTURED);
        Node n3 = getRoot().addNode("node3", UNSTRUCTURED);
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(2, 2, Event.NODE_REMOVED, null, false, null, null, false);

        // move node
        String oldPath = n2.getPath();
        String newPath = n3.getPath() + '/' + node2;
        getWorkspace().move(oldPath, newPath);

        // remove node
        String removedNodePath = n1.getPath();
        n1.remove();
        save();

        // event handling
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(oldPath));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(newPath));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(moveNodeListener, newPath));
        assertTrue("Path for new location of added node is wrong: actual=" + addNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + newPath, containsPath(addNodeListener, newPath));
        assertTrue("Path for new location of removed node is wrong: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + oldPath, containsPath(removeNodeListener, oldPath));
        assertTrue("Path for removed node is wrong", containsPath(removeNodeListener, removedNodePath));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.NodeReorderTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see NodeReorderTest#testNodeReorder()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeReorderTest_testNodeReorder() throws Exception {
        // setup
        getRoot().addNode("node1", UNSTRUCTURED);
        Node n2 = getRoot().addNode("node2", UNSTRUCTURED);
        Node n3 = getRoot().addNode("node3", UNSTRUCTURED);
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // reorder to trigger events
        getRoot().orderBefore(n3.getName(), n2.getName());
        save();

        // handle events
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is("node3"));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is("node2"));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + n3.getPath(), containsPath(moveNodeListener, n3.getPath()));
        assertTrue("Added reordered node has wrong path: actual=" + addNodeListener.getEvents().get(0).getPath() + ", expected="
                   + n3.getPath(), containsPath(addNodeListener, n3.getPath()));
        assertTrue("Removed reordered node has wrong path: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + n3.getPath(), containsPath(addNodeListener, n3.getPath()));
    }

    /**
     * @throws Exception
     * @see NodeReorderTest#testNodeReorderSameName()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeReorderTest_testNodeReorderSameName() throws Exception {
        // setup
        String node1 = "node1";
        Node n1 = getRoot().addNode(node1, UNSTRUCTURED);
        getRoot().addNode(node1, UNSTRUCTURED);
        getRoot().addNode(node1, UNSTRUCTURED);
        save();

        // register listeners + "[2]"
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // reorder to trigger events
        getRoot().orderBefore(node1 + "[3]", node1 + "[2]");
        save();

        // handle events
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(node1 + "[3]"));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(node1 + "[2]"));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + getRoot().getPath() + "/" + node1 + "[2]", containsPath(moveNodeListener,
                                                                                             getRoot().getPath() + "/" + node1
                                                                                             + "[2]"));
        assertTrue("Added reordered node has wrong path: actual=" + addNodeListener.getEvents().get(0).getPath() + ", expected="
                   + n1.getPath() + "[2]", containsPath(addNodeListener, n1.getPath() + "[2]"));
        assertTrue("Removed reordered node has wrong path: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + n1.getPath() + "[3]", containsPath(removeNodeListener, n1.getPath() + "[3]"));
    }

    /**
     * @throws Exception
     * @see NodeReorderTest#testNodeReorderSameNameWithRemove()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestNodeReorderTest_testNodeReorderSameNameWithRemove() throws Exception {
        // setup
        String node1 = "node1";
        Node n1 = getRoot().addNode(node1, UNSTRUCTURED);
        getRoot().addNode("node2", UNSTRUCTURED);
        getRoot().addNode(node1, UNSTRUCTURED);
        getRoot().addNode(node1, UNSTRUCTURED);
        Node n3 = getRoot().addNode("node3", UNSTRUCTURED);
        save();

        // register listeners + "[2]"
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(2, Event.NODE_REMOVED, null, false, null, null, false);

        // trigger events
        getRoot().orderBefore(node1 + "[2]", null);
        String removedPath = n3.getPath();
        n3.remove();
        save();

        // handle events
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(node1 + "[2]"));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + getRoot().getPath() + "/" + node1 + "[3]", containsPath(moveNodeListener,
                                                                                             getRoot().getPath() + "/" + node1
                                                                                             + "[3]"));
        assertTrue("Added reordered node has wrong path: actual=" + addNodeListener.getEvents().get(0).getPath() + ", expected="
                   + n1.getPath() + "[3]", containsPath(addNodeListener, n1.getPath() + "[3]"));
        assertTrue("Removed reordered node path not found: " + n1.getPath() + "[2]", containsPath(removeNodeListener,
                                                                                                  n1.getPath() + "[2]"));
        assertTrue("Removed node path not found: " + removedPath, containsPath(removeNodeListener, removedPath));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.PropertyAddedTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see PropertyAddedTest#testMultiPropertyAdded()
     */
    @Test
    public void shouldTestPropertyAddedTest_testMultiPropertyAdded() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(2, Event.PROPERTY_ADDED, null, false, null, null, false);

        // add multiple properties
        Property prop1 = node.setProperty("prop1", "prop1 content");
        Property prop2 = node.setProperty("prop2", "prop2 content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for first added property not found: " + prop1.getPath(), containsPath(listener, prop1.getPath()));
        assertTrue("Path for second added property not found: " + prop2.getPath(), containsPath(listener, prop2.getPath()));
    }

    /**
     * @throws Exception
     * @see PropertyAddedTest#testSinglePropertyAdded()
     */
    @Test
    public void shouldTestPropertyAddedTest_testSinglePropertyAdded() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(1, Event.PROPERTY_ADDED, null, false, null, null, false);

        // add the property
        Property prop1 = node.setProperty("prop1", "prop1 content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + prop1.getPath(), containsPath(listener, prop1.getPath()));
    }

    /**
     * @throws Exception
     * @see PropertyAddedTest#testSystemGenerated()
     */
    @Test
    public void shouldTestPropertyAddedTest_testSystemGenerated() throws Exception {
        // register listener
        TestListener listener = addListener(3, Event.PROPERTY_ADDED, null, false, null, null, false);

        // create node (which adds 3 properties)
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for jrc:primaryType property was not found.", containsPath(listener, node.getProperty("jcr:primaryType")
                                                                                                  .getPath()));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.PropertyChangedTests
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see PropertyChangedTest#testMultiPropertyChanged()
     */
    @Test
    public void shouldTestPropertyChangedTests_testMultiPropertyChanged() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop1 = node.setProperty("prop1", "prop1 content");
        Property prop2 = node.setProperty("prop2", "prop2 content");
        save();

        // register listener
        TestListener listener = addListener(2, Event.PROPERTY_CHANGED, null, false, null, null, false);

        // add multiple properties
        prop1.setValue("prop1 modified content");
        prop2.setValue("prop2 modified content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for first changed property not found: " + prop1.getPath(), containsPath(listener, prop1.getPath()));
        assertTrue("Path for second changed property not found: " + prop2.getPath(), containsPath(listener, prop2.getPath()));
    }

    /**
     * @throws Exception
     * @see PropertyChangedTest#testPropertyRemoveCreate()
     */
    @Test
    public void shouldTestPropertyChangedTests_testPropertyRemoveCreate() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        String propName = "prop1";
        Property prop = node.setProperty(propName, propName + " content");
        String propPath = prop.getPath();
        save();

        // register listeners
        TestListener listener1 = addListener(1, Event.PROPERTY_CHANGED, null, false, null, null, false);
        TestListener listener2 = addListener(2, Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED, null, false, null, null, false);

        // trigger events
        prop.remove();
        node.setProperty(propName, true);
        save();

        // event handling
        listener1.waitForEvents();
        removeListener(listener1);
        listener2.waitForEvents();
        removeListener(listener2);

        // tests
        if (listener1.getEvents().size() == 1) {
            checkResults(listener1);
            assertTrue("Path for removed then added property is wrong: actual=" + listener1.getEvents().get(0).getPath()
                       + ", expected=" + propPath, containsPath(listener1, propPath));
        } else {
            checkResults(listener2);
            assertTrue("Path for removed then added property is wrong: actual=" + listener2.getEvents().get(0).getPath()
                       + ", expected=" + propPath, containsPath(listener2, propPath));
            assertTrue("Path for removed then added property is wrong: actual=" + listener2.getEvents().get(1).getPath()
                       + ", expected=" + propPath, containsPath(listener2, propPath));
        }
    }

    /**
     * @throws Exception
     * @see PropertyChangedTest#testSinglePropertyChanged()
     */
    @Test
    public void shouldTestPropertyChangedTests_testSinglePropertyChanged() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop1 = node.setProperty("prop1", "prop1 content");
        save();

        // register listener
        TestListener listener = addListener(1, Event.PROPERTY_CHANGED, null, false, null, null, false);

        // change the property
        prop1.setValue("prop1 modified content");
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for changed property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + prop1.getPath(), containsPath(listener, prop1.getPath()));
    }

    /**
     * @throws Exception
     * @see PropertyChangedTest#testSinglePropertyChangedWithAdded()
     */
    @Test
    public void shouldTestPropertyChangedTests_testSinglePropertyChangedWithAdded() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop1 = node.setProperty("prop1", "prop1 content");
        save();

        // register listener
        TestListener listener = addListener(1, Event.PROPERTY_CHANGED, null, false, null, null, false);

        // change the property
        prop1.setValue("prop1 modified content");
        node.setProperty("prop2", "prop2 content"); // property added event should not be received
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for changed property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + prop1.getPath(), containsPath(listener, prop1.getPath()));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.PropertyRemovedTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see PropertyRemovedTest#testMultiPropertyRemoved()
     */
    @Test
    public void shouldTestPropertyRemovedTest_testMultiPropertyRemoved() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop1 = node.setProperty("prop1", "prop1 content");

        Property prop2 = node.setProperty("prop2", "prop2 content");
        save();

        // register listener
        TestListener listener = addListener(2, Event.PROPERTY_REMOVED, null, false, null, null, false);

        // remove the property
        String prop1Path = prop1.getPath();
        prop1.remove();
        String prop2Path = prop2.getPath();
        prop2.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for first removed property not found: " + prop1Path, containsPath(listener, prop1Path));
        assertTrue("Path for second removed property not found: " + prop2Path, containsPath(listener, prop2Path));
    }

    /**
     * @throws Exception
     * @see PropertyRemovedTest#testSinglePropertyRemoved()
     */
    @Test
    public void shouldTestPropertyRemovedTest_testSinglePropertyRemoved() throws Exception {
        // setup
        Node node = getRoot().addNode("node1", UNSTRUCTURED);
        Property prop = node.setProperty("prop1", "prop1 content");
        String propPath = prop.getPath();
        save();

        // register listener
        TestListener listener = addListener(1, Event.PROPERTY_REMOVED, null, false, null, null, false);

        // remove the property
        prop.remove();
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for removed property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + propPath, containsPath(listener, propPath));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.AddEventListenerTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see AddEventListenerTest#testIsDeepFalseNodeAdded()
     */
    @Test
    public void shouldTestAddEventListenerTest_testIsDeepFalseNodeAdded() throws Exception {
        // setup
        String node1 = "node1";
        String path = getRoot().getPath() + '/' + node1;

        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, path, false, null, null, false);

        // add child node under the path we care about
        Node n1 = getRoot().addNode(node1, UNSTRUCTURED);
        Node childNode = n1.addNode("node2", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        checkResults(listener);
        assertTrue("Child node path is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + childNode.getPath(), containsPath(listener, childNode.getPath()));
    }

    /**
     * @throws Exception
     * @see AddEventListenerTest#testIsDeepFalsePropertyAdded()
     */
    @Test
    public void shouldTestAddEventListenerTest_testIsDeepFalsePropertyAdded() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        Node n2 = getRoot().addNode("node2", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(1, Event.PROPERTY_ADDED, n1.getPath(), false, null, null, false);

        // add property
        String prop = "prop";
        Property n1Prop = n1.setProperty(prop, "foo");
        n2.setProperty(prop, "foo"); // should not receive event for this
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for added property is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + n1Prop.getPath(), containsPath(listener, n1Prop.getPath()));
    }

    /**
     * @throws Exception
     * @see AddEventListenerTest#testNodeType()
     */
    @Test
    public void shouldTestAddEventListenerTest_testNodeType() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        n1.addMixin(LOCK_MIXIN);
        Node n2 = getRoot().addNode("node2", UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(1,
                                            Event.NODE_ADDED,
                                            getRoot().getPath(),
                                            true,
                                            null,
                                            new String[] {LOCK_MIXIN},
                                            false);

        // trigger events
        String node3 = "node3";
        Node n3 = n1.addNode(node3, UNSTRUCTURED);
        n2.addNode(node3, UNSTRUCTURED);
        save();

        // handle events
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Wrong path: actual=" + listener.getEvents().get(0).getPath() + ", expected=" + n3.getPath(),
                   containsPath(listener, n3.getPath()));
    }

    /**
     * @throws Exception
     * @see AddEventListenerTest#testNoLocalTrue()
     */
    @Test
    public void shouldTestAddEventListenerTest_testNoLocalTrue() throws Exception {
        // register listener
        TestListener listener = addListener(0, Event.NODE_ADDED, getRoot().getPath(), true, null, null, true);

        // trigger events
        getRoot().addNode("node1", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
    }

    /**
     * @throws Exception
     * @see AddEventListenerTest#testPath()
     */
    @Test
    public void shouldTestAddEventListenerTest_testPath() throws Exception {
        // setup
        String node1 = "node1";
        String path = getRoot().getPath() + '/' + node1;

        // register listener
        TestListener listener = addListener(1, Event.NODE_ADDED, path, true, null, null, false);

        // add child node under the path we care about
        Node n1 = getRoot().addNode(node1, UNSTRUCTURED);
        Node childNode = n1.addNode("node2", UNSTRUCTURED);
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Child node path is wrong: actual=" + listener.getEvents().get(0).getPath() + ", expected="
                   + childNode.getPath(), containsPath(listener, childNode.getPath()));
    }

    /**
     * @throws Exception
     * @see AddEventListenerTest#testUUID()
     */
    @Test
    public void shouldTestAddEventListenerTest_testUUID() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        n1.addMixin(REF_MIXIN);
        Node n2 = getRoot().addNode("node2", UNSTRUCTURED);
        n2.addMixin(REF_MIXIN);
        save();

        // register listener
        TestListener listener = addListener(1,
                                            Event.PROPERTY_ADDED,
                                            getRoot().getPath(),
                                            true,
                                            new String[] {n1.getIdentifier()},
                                            null,
                                            false);

        // create properties
        String prop1 = "prop1";
        Property n1Prop = n1.setProperty(prop1, "foo");
        n2.setProperty(prop1, "foo"); // should not get an event for this
        save();

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Wrong path: actual=" + listener.getEvents().get(0).getPath() + ", expected=" + n1Prop.getPath(),
                   containsPath(listener, n1Prop.getPath()));
    }

    // ===========================================================================================================================
    // @see org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest
    // ===========================================================================================================================

    /**
     * @throws Exception
     * @see WorkspaceOperationTest#testCopy()
     */
    @Test
    @Ignore
    public void shouldTestWorkspaceOperationTest_testCopy() throws Exception {
        // setup
        Node addedNode = getRoot().addNode("node1", UNSTRUCTURED);
        String node2 = "node2";
        addedNode.addNode(node2, UNSTRUCTURED);
        save();

        // register listener
        TestListener listener = addListener(2, Event.NODE_ADDED, null, false, null, null, false);

        // perform copy
        String targetPath = getRoot().getPath() + "/node3";
        getWorkspace().copy(addedNode.getPath(), targetPath);

        // event handling
        listener.waitForEvents();
        removeListener(listener);

        // tests
        checkResults(listener);
        assertTrue("Path for copied node not found: " + targetPath, containsPath(listener, targetPath));
        assertTrue("Path for copied child node not found: " + (targetPath + '/' + node2),
                   containsPath(listener, (targetPath + '/' + node2)));
    }

    /**
     * @throws Exception
     * @see WorkspaceOperationTest#testMove()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestWorkspaceOperationTest_testMove() throws Exception {
        // setup
        String node2 = "node2";
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        n1.addNode(node2, UNSTRUCTURED);
        Node n3 = getRoot().addNode("node3", UNSTRUCTURED);
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // perform move
        String oldPath = n1.getPath();
        String targetPath = n3.getPath() + "/node4";
        getWorkspace().move(oldPath, targetPath);
        save();

        // event handling
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(oldPath));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(targetPath));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + targetPath, containsPath(moveNodeListener, targetPath));
        assertTrue("Path for new location of moved node is wrong: actual=" + addNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + targetPath, containsPath(addNodeListener, targetPath));
        assertTrue("Path for old location of moved node is wrong: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + oldPath, containsPath(removeNodeListener, oldPath));
    }

    /**
     * @throws Exception
     * @see WorkspaceOperationTest#testRename()
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldTestWorkspaceOperationTest_testRename() throws Exception {
        // setup
        Node n1 = getRoot().addNode("node1", UNSTRUCTURED);
        n1.addNode("node2", UNSTRUCTURED);
        save();

        // register listeners
        TestListener moveNodeListener = addListener(1, Event.NODE_MOVED, null, false, null, null, false);
        TestListener addNodeListener = addListener(1, Event.NODE_ADDED, null, false, null, null, false);
        TestListener removeNodeListener = addListener(1, Event.NODE_REMOVED, null, false, null, null, false);

        // rename node
        String oldPath = n1.getPath();
        String renamedPath = getRoot().getPath() + "/node3";
        getWorkspace().move(oldPath, renamedPath);
        save();

        // event handling
        moveNodeListener.waitForEvents();
        removeListener(moveNodeListener);
        addNodeListener.waitForEvents();
        removeListener(addNodeListener);
        removeNodeListener.waitForEvents();
        removeListener(removeNodeListener);

        // tests
        checkResults(moveNodeListener);
        checkResults(addNodeListener);
        checkResults(removeNodeListener);
        Map<String, String> info = moveNodeListener.getEvents().get(0).getInfo();
        assertThat(info.get(JcrObservationManager.MOVE_FROM_KEY), is(oldPath));
        assertThat(info.get(JcrObservationManager.MOVE_TO_KEY), is(renamedPath));
        assertThat(info.get(JcrObservationManager.ORDER_CHILD_KEY), is(nullValue()));
        assertThat(info.get(JcrObservationManager.ORDER_BEFORE_KEY), is(nullValue()));
        assertTrue("Path for new location of moved node is wrong: actual=" + moveNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + renamedPath, containsPath(moveNodeListener, renamedPath));
        assertTrue("Path for renamed node is wrong: actual=" + addNodeListener.getEvents().get(0).getPath() + ", expected="
                   + renamedPath, containsPath(addNodeListener, renamedPath));
        assertTrue("Path for old name of renamed node is wrong: actual=" + removeNodeListener.getEvents().get(0).getPath()
                   + ", expected=" + oldPath, containsPath(removeNodeListener, oldPath));
    }

    @Test
    public void shouldNotReceiveEventsFromOtherWorkspaces() throws Exception {
        // Log into a second workspace ...
        Session session2 = login(REPOSITORY, "ws2", USER_ID, USER_ID.toCharArray());

        // Register 2 listeners in the first session ...
        TestListener listener1 = addListener(session, 4, ALL_EVENTS, "/", true, null, null, false);
        TestListener addListener1 = addListener(session, 1, Event.NODE_ADDED, "/", true, null, null, false);

        // Register 2 listeners in the second session ...
        TestListener listener2 = addListener(session2, 0, ALL_EVENTS, "/", true, null, null, false);
        TestListener addListener2 = addListener(session2, 0, Event.NODE_ADDED, "/", true, null, null, false);

        // Add a node to the first session ...
        session.getRootNode().addNode("nodeA", "nt:unstructured");
        session.save();

        // Wait for the events on the first session's listeners (that should get the events) ...
        listener1.waitForEvents();
        addListener1.waitForEvents();
        removeListener(listener1);
        removeListener(addListener1);

        // Wait for the events on the second session's listeners (that should NOT get the events) ...
        listener2.waitForEvents();
        // addListener2.waitForEvents();
        removeListener(listener2);
        removeListener(addListener2);

        // Verify the expected events were received ...
        checkResults(listener1);
        checkResults(addListener1);
        checkResults(listener2);
        checkResults(addListener2);
    }

    @FixFor( "MODE-786" )
    @Test
    public void shouldReceiveEventsForChangesToSessionNamespacesInSystemContent() throws Exception {
        String uri = "http://acme.com/example/foobar/";
        String prefix = "foobar";
        assertNoSessionNamespace(uri, prefix);

        TestListener listener = addListener(session, 0, ALL_EVENTS, "/jcr:system", true, null, null, false);
        session.setNamespacePrefix(prefix, uri);

        // Wait for the events on the session's listeners (that should NOT get the events) ...
        listener.waitForEvents();
        removeListener(listener);

        // Verify the expected events were received ...
        checkResults(listener);
    }

    @FixFor( "MODE-786" )
    @Test
    public void shouldReceiveEventsForChangesToRepositoryNamespacesInSystemContent() throws Exception {
        String uri = "http://acme.com/example/foobar/";
        String prefix = "foobar";
        assertNoRepositoryNamespace(uri, prefix);

        Session session2 = login(REPOSITORY, "ws2", USER_ID, USER_ID.toCharArray());

        TestListener listener = addListener(session, 3, ALL_EVENTS, "/jcr:system", true, null, null, false);
        TestListener listener2 = addListener(session2, 3, ALL_EVENTS, "/jcr:system", true, null, null, false);

        session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);

        // Wait for the events on the session's listeners (that should get the events) ...
        listener.waitForEvents();
        listener2.waitForEvents();
        removeListener(listener);
        removeListener(listener2);

        assertThat(session.getWorkspace().getNamespaceRegistry().getPrefix(uri), is(prefix));
        assertThat(session.getWorkspace().getNamespaceRegistry().getURI(prefix), is(uri));

        // Verify the expected events were received ...
        checkResults(listener);
        checkResults(listener2);
    }

    @FixFor( "MODE-786" )
    @Test
    public void shouldReceiveEventsForChangesToLocksInSystemContent() throws Exception {
        Node root = session.getRootNode();
        Node parentNode = root.addNode("lockedPropParent");
        parentNode.addMixin("mix:lockable");

        Node targetNode = parentNode.addNode("lockedTarget");
        targetNode.setProperty("foo", "bar");
        session.save();

        TestListener listener = addListener(session, 11, 2, ALL_EVENTS, "/jcr:system", true, null, null, false);

        lock(parentNode, true, true); // SHOULD GENERATE AN EVENT TO CREATE A LOCK

        // Wait for the events on the session's listeners (that should get the events) ...
        listener.waitForEvents();
        removeListener(listener);

        // Verify the expected events were received ...
        checkResults(listener);
    }

    @FixFor( "MODE-786" )
    @Test
    public void shouldReceiveEventsForChangesToVersionsInSystemContent() throws Exception {
        boolean hiearchical = engine.getRepository(REPOSITORY)
                                    .getOptions()
                                    .get(JcrRepository.Option.VERSION_HISTORY_STRUCTURE)
                                    .equalsIgnoreCase(VersionHistoryOption.HIERARCHICAL);
        int numEvents = hiearchical ? 23 : 15;
        TestListener listener = addListener(session, numEvents, ALL_EVENTS, "/jcr:system", true, null, null, false);

        Node node = session.getRootNode().addNode("/test", "nt:unstructured");
        node.addMixin("mix:versionable");
        session.save(); // SHOULD GENERATE AN EVENT TO CREATE VERSION HISTORY FOR THE NODE

        // Wait for the events on the session's listeners (that should get the events) ...
        listener.waitForEvents();
        removeListener(listener);

        Node history = node.getProperty("jcr:versionHistory").getNode();
        assertThat(history, is(notNullValue()));

        assertThat(node.hasProperty("jcr:baseVersion"), is(true));
        Node version = node.getProperty("jcr:baseVersion").getNode();
        assertThat(version, is(notNullValue()));

        assertThat(version.getParent(), is(history));

        assertThat(node.hasProperty("jcr:uuid"), is(true));
        assertThat(node.getProperty("jcr:uuid").getString(), is(history.getProperty("jcr:versionableUuid").getString()));

        assertThat(versionHistory(node).getIdentifier(), is(history.getIdentifier()));
        assertThat(versionHistory(node).getPath(), is(history.getPath()));

        assertThat(baseVersion(node).getIdentifier(), is(version.getIdentifier()));
        assertThat(baseVersion(node).getPath(), is(version.getPath()));

        // Verify the expected events were received ...
        checkResults(listener);
    }

    protected void assertNoRepositoryNamespace( String uri,
                                                String prefix ) throws RepositoryException {
        NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
        for (String existingPrefix : registry.getPrefixes()) {
            assertThat(existingPrefix.equals(prefix), is(false));
        }
        for (String existingUri : registry.getURIs()) {
            assertThat(existingUri.equals(uri), is(false));
        }
    }

    protected void assertNoSessionNamespace( String uri,
                                             String prefix ) throws RepositoryException {
        for (String existingPrefix : session.getNamespacePrefixes()) {
            assertThat(existingPrefix.equals(prefix), is(false));
            String existingUri = session.getNamespaceURI(existingPrefix);
            assertThat(existingUri.equals(uri), is(false));
        }
    }

    protected VersionHistory versionHistory( Node node ) throws RepositoryException {
        return session.getWorkspace().getVersionManager().getVersionHistory(node.getPath());
    }

    protected Version baseVersion( Node node ) throws RepositoryException {
        return session.getWorkspace().getVersionManager().getBaseVersion(node.getPath());
    }

    protected void lock( Node node,
                         boolean isDeep,
                         boolean isSessionScoped ) throws RepositoryException {
        session.getWorkspace().getLockManager().lock(node.getPath(), isDeep, isSessionScoped, 1L, "owner");
    }

    // ===========================================================================================================================
    // Inner Class
    // ===========================================================================================================================

    private class TestListener implements EventListener {

        private String errorMessage;
        private final List<Event> events;
        private int eventsProcessed = 0;
        private final int eventTypes;
        private final int expectedEvents;
        private final CountDownLatch latch;

        public TestListener( int expectedEvents,
                             int numIterators,
                             int eventTypes ) {
            this.eventTypes = eventTypes;
            this.expectedEvents = expectedEvents;
            this.events = new ArrayList<Event>();
            this.latch = new CountDownLatch(numIterators);
        }

        public int getActualEventCount() {
            return this.eventsProcessed;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }

        public List<Event> getEvents() {
            return this.events;
        }

        public int getExpectedEventCount() {
            return this.expectedEvents;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        public void onEvent( EventIterator itr ) {
            // this is called each time a "transaction" is committed. Most times this means after a session.save. But there are
            // other times, like a workspace.move and a node.lock
            try {
                long position = itr.getPosition();

                // iterator position must be set initially zero
                if (position == 0) {
                    while (itr.hasNext()) {
                        Event event = itr.nextEvent();

                        // check iterator position
                        if (++position != itr.getPosition()) {
                            this.errorMessage = "EventIterator position was " + itr.getPosition() + " and should be " + position;
                            break;
                        }

                        // add event to collection and increment total
                        this.events.add(event);
                        ++this.eventsProcessed;

                        // check to make sure we haven't received too many events
                        if (this.eventsProcessed > this.expectedEvents) {
                            break;
                        }

                        // check event type
                        int eventType = event.getType();

                        if ((this.eventTypes & eventType) == 0) {
                            this.errorMessage = "Received a wrong event type of " + eventType;
                            break;
                        }
                    }
                } else {
                    this.errorMessage = "EventIterator position was not initially set to zero";
                }
            } finally {
                // This has to be done LAST, otherwise waitForEvents() will return before the above stuff is done
                this.latch.countDown();
            }
        }

        public void waitForEvents() throws Exception {
            long millis = this.expectedEvents == 0 ? 50 : 250;
            this.latch.await(millis, TimeUnit.MILLISECONDS);
        }
    }

}
