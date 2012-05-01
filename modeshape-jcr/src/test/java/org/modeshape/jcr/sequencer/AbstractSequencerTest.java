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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.jcr.sequencer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import org.junit.Assert;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.observation.Event;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCING_FAILURE;
import static org.modeshape.jcr.api.observation.Event.Sequencing.OUTPUT_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SELECTED_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCED_NODE_ID;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCED_NODE_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCER_NAME;
import static org.modeshape.jcr.api.observation.Event.Sequencing.USER_ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Class which serves as base for various sequencer unit tests. In addition to this, it uses the sequencing events fired by the
 * {@link org.modeshape.jcr.JcrObservationManager} to perform various assertions and therefore, acts as a test for those as well.
 *
 * @author Horia Chiorean
 */
public abstract class AbstractSequencerTest extends SingleUseAbstractTest {

    protected Node rootNode;

    private ObservationManager observationManager;

    /**
     * A [node path, node instance] map which is populated by the listener, once each sequencing event is received
     */
    private final Map<String, Node> sequencedNodes = new HashMap<String, Node>();

    /**
     * A [node path, latch] map which is used to block tests waiting for sequenced output, until either the node has been
     * sequenced or a timeout occurs
     */
    private final ConcurrentHashMap<String, CountDownLatch> nodeSequencedLatches = new ConcurrentHashMap<String, CountDownLatch>();

    /**
     * A [node path, latch] map which is used to block tests waiting for a sequencing failure, until either the failure has occurred
     * or a timeout occurs
     */
    private final ConcurrentHashMap<String, CountDownLatch> sequencingFailureLatches = new ConcurrentHashMap<String, CountDownLatch>();

    /**
     * A [sequenced node path, event] map which will hold all the received sequencing events, both in failure and non-failure cases,
     * using the path of the sequenced node as key.
     */
    private final ConcurrentHashMap<String, Event> sequencingEvents = new ConcurrentHashMap<String, Event>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = session.getRootNode();
        observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        observationManager.addEventListener(new SequencingListener(), NODE_SEQUENCED, null, true, null, null, false);
        observationManager.addEventListener(new SequencingFailureListener(), NODE_SEQUENCING_FAILURE, null, true, null, null,
                                            false);
    }

    @Override
    public void afterEach() throws Exception {
        for (EventListenerIterator it = observationManager.getRegisteredEventListeners(); it.hasNext(); ) {
            observationManager.removeEventListener(it.nextEventListener());
        }
        super.afterEach();
        cleanupData();
    }

    private void cleanupData() {
        sequencedNodes.clear();
        sequencingEvents.clear();
        nodeSequencedLatches.clear();
        sequencingFailureLatches.clear();
    }

    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     Environment environment ) throws Exception {
        return RepositoryConfiguration.read(getRepositoryConfigStream(), repositoryName).with(environment);
    }

    /**
     * Returns an input stream to a JSON file which will be used to configure the repository. By default, this is
     * config/repot-config.json
     *
     * @return a {@code InputStream} instance
     */
    protected InputStream getRepositoryConfigStream() {
        return resourceStream("config/repo-config.json");
    }

    /**
     * Creates a nt:file node, under the root node, at the given path and with the jcr:data property pointing at the filepath.
     *
     * @param nodePath the path under the root node, where the nt:file will be created.
     * @param filePath a path relative to {@link Class#getResourceAsStream(String)} where a file is expected at runtime
     * @return the new node
     * @throws RepositoryException if anything fails
     */
    protected Node createNodeWithContentFromFile( String nodePath,
                                                  String filePath ) throws RepositoryException {
        Node parent = rootNode;
        for (String pathSegment : nodePath.split("/")) {
            parent = parent.addNode(pathSegment);
        }
        Node content = parent.addNode(JcrConstants.JCR_CONTENT);
        content.setProperty(JcrConstants.JCR_DATA,
                            ((javax.jcr.Session)session).getValueFactory().createBinary(resourceStream(filePath)));
        session.save();
        return parent;
    }

    /**
     * Retrieves a sequenced node using 5 seconds as maximum wait time.
     *
     * @param parentNode an existing {@link Node}
     * @param relativePath the path under the parent node at which the sequenced node is expected to appear (note that this must
     * be the path to the "new" node, always.
     * @return either the sequenced node or null, if something has failed.
     * @throws Exception if anything unexpected happens
     * @see AbstractSequencerTest#getSequencedNode(javax.jcr.Node, String, int)
     */
    protected Node getSequencedNode( Node parentNode,
                                     String relativePath ) throws Exception {
        return getSequencedNode(parentNode, relativePath, 5);
    }

    /**
     * Attempts to retrieve a node (which is expected to have been sequenced) under an existing parent node at a relative path.
     * The sequenced node "appears" when the {@link SequencingListener} is notified of the sequencing process. The thread which
     * calls this method either returns immediately if the node has already been sequenced, or waits a number of seconds for it to
     * become available.
     *
     * @param parentNode an existing {@link Node}
     * @param relativePath the path under the parent node at which the sequenced node is expected to appear (note that this must
     * be the path to the "new" node, always.
     * @param waitTimeSeconds the max number of seconds to wait.
     * @return either the sequenced node or null, if something has failed.
     * @throws Exception if anything unexpected happens
     */
    protected Node getSequencedNode( Node parentNode,
                                     String relativePath,
                                     int waitTimeSeconds ) throws Exception {
        String parentNodePath = parentNode.getPath();
        String expectedPath = parentNodePath.endsWith(
                "/") ? parentNodePath + relativePath : parentNodePath + "/" + relativePath;

        if (!sequencedNodes.containsKey(expectedPath)) {
            createWaitingLatchIfNecessary(expectedPath, nodeSequencedLatches);
            logger.debug("Waiting for sequenced node at: " + expectedPath);
            CountDownLatch countDownLatch = nodeSequencedLatches.get(expectedPath);
            countDownLatch.await(waitTimeSeconds, TimeUnit.SECONDS);
        }
        nodeSequencedLatches.remove(expectedPath);
        return sequencedNodes.remove(expectedPath);
    }

    protected void expectSequencingFailure( Node sequencedNode ) throws Exception {
        expectSequencingFailure(sequencedNode, 5);
    }

    protected void expectSequencingFailure( Node sequencedNode,
                                            int waitTimeSeconds ) throws Exception {
        String nodePath = sequencedNode.getPath();
        createWaitingLatchIfNecessary(nodePath, sequencingFailureLatches);
        CountDownLatch countDownLatch = sequencingFailureLatches.get(nodePath);
        assertTrue("Sequencing failure event not received", countDownLatch.await(waitTimeSeconds, TimeUnit.SECONDS));
        sequencingFailureLatches.remove(nodePath);
    }

    private void createWaitingLatchIfNecessary( String expectedPath,
                                                ConcurrentHashMap<String, CountDownLatch> latchesMap )
            throws InterruptedException {
        latchesMap.putIfAbsent(expectedPath, new CountDownLatch(1));
    }

    private void smokeCheckSequencingEvent(Event event, int expectedEventType, String... expectedEventInfoKeys) throws RepositoryException {
        assertEquals(event.getType(), expectedEventType);
        Map info = event.getInfo();
        assertNotNull(info);
        for (String extraInfoKey : expectedEventInfoKeys) {
            assertNotNull(info.get(extraInfoKey));
        }
    }

    protected void assertCreatedBySessionUser( Node node,
                                               Session session ) throws RepositoryException {
        assertEquals(session.getUserID(), node.getProperty(JcrLexicon.CREATED_BY.getString()).getString());
    }

    private Map getSequencingEventInfo( Node sequencedNode ) throws RepositoryException {
        Event receivedEvent = sequencingEvents.get(sequencedNode.getPath());
        assertNotNull(receivedEvent);
        return receivedEvent.getInfo();
    }


    protected Map assertSequencingEventInfo( Node sequencedNode,
                                           String expectedUserId,
                                           String expectedSequencerName,
                                           String expectedSelectedPath,
                                           String expectedOutputPath) throws RepositoryException {
        Map sequencingEventInfo = getSequencingEventInfo(sequencedNode);
        Assert.assertEquals(expectedUserId, sequencingEventInfo.get(Event.Sequencing.USER_ID));
        Assert.assertEquals(expectedSequencerName, sequencingEventInfo.get(Event.Sequencing.SEQUENCER_NAME));
        Assert.assertEquals(sequencedNode.getIdentifier(), sequencingEventInfo.get(Event.Sequencing.SEQUENCED_NODE_ID));

        Assert.assertEquals(sequencedNode.getPath(), sequencingEventInfo.get(Event.Sequencing.SEQUENCED_NODE_PATH));
        Assert.assertEquals(expectedSelectedPath, sequencingEventInfo.get(Event.Sequencing.SELECTED_PATH));
        Assert.assertEquals(expectedOutputPath, sequencingEventInfo.get(Event.Sequencing.OUTPUT_PATH));
        return sequencingEventInfo;
    }

    private class SequencingListener implements EventListener {

        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    Event event = (Event)events.nextEvent();
                    smokeCheckSequencingEvent(event,
                                              NODE_SEQUENCED,
                                              SEQUENCED_NODE_ID,
                                              SEQUENCED_NODE_PATH,
                                              OUTPUT_PATH,
                                              SELECTED_PATH,
                                              SEQUENCER_NAME,
                                              USER_ID);
                    sequencingEvents.putIfAbsent((String)event.getInfo().get(SEQUENCED_NODE_PATH), event);

                    String nodePath = event.getPath();
                    logger.debug("New sequenced node at: " + nodePath);
                    sequencedNodes.put(nodePath, session.getNode(nodePath));

                    // signal the node is available
                    createWaitingLatchIfNecessary(nodePath, nodeSequencedLatches);
                    nodeSequencedLatches.get(nodePath).countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class SequencingFailureListener implements EventListener {
        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    Event event = (Event)events.nextEvent();
                    smokeCheckSequencingEvent(event,
                                              NODE_SEQUENCING_FAILURE,
                                              SEQUENCED_NODE_ID,
                                              SEQUENCED_NODE_PATH,
                                              Event.Sequencing.SEQUENCING_FAILURE_CAUSE,
                                              OUTPUT_PATH,
                                              SELECTED_PATH,
                                              SEQUENCER_NAME,
                                              USER_ID);
                    String nodePath = event.getPath();

                    sequencingEvents.putIfAbsent(nodePath, event);
                    createWaitingLatchIfNecessary(nodePath, sequencingFailureLatches);
                    sequencingFailureLatches.get(nodePath).countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
