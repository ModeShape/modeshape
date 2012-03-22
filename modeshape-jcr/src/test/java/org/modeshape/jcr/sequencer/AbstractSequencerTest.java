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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.modeshape.jcr.api.observation.Event.NODE_SEQUENCED;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import org.infinispan.manager.CacheContainer;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.observation.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which serves as base for various sequencer unit tests.
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractSequencerTest extends SingleUseAbstractTest {

    protected Node rootNode;

    /**
     * A [node path, node instance] map which is populated by the listener, once each sequencing event is received
     */
    private ConcurrentHashMap<String, Node> sequencedNodes = new ConcurrentHashMap<String, Node>();

    /**
     * A [node path, latch] map which is used to block tests waiting for sequenced output, until either the node has been
     * sequenced or a timeout occurs
     */
    private ConcurrentHashMap<String, CountDownLatch> waitingLatches = new ConcurrentHashMap<String, CountDownLatch>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = session.getRootNode();
        ((Workspace)session.getWorkspace()).getObservationManager().addEventListener(new SequencingListener(),
                                                                                     NODE_SEQUENCED,
                                                                                     null,
                                                                                     true,
                                                                                     null,
                                                                                     null,
                                                                                     false);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();

        sequencedNodes.clear();
        sequencedNodes = null;

        waitingLatches.clear();
        waitingLatches = null;
    }

    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     CacheContainer cacheContainer ) throws Exception {
        return RepositoryConfiguration.read(getRepositoryConfigStream(), repositoryName).with(cacheContainer);
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
     *        be the path to the "new" node, always.
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
     *        be the path to the "new" node, always.
     * @param waitTimeSeconds the max number of seconds to wait.
     * @return either the sequenced node or null, if something has failed.
     * @throws Exception if anything unexpected happens
     */
    protected Node getSequencedNode( Node parentNode,
                                     String relativePath,
                                     int waitTimeSeconds ) throws Exception {
        String parentNodePath = parentNode.getPath();
        String expectedPath = parentNodePath.endsWith("/") ? parentNodePath + relativePath : parentNodePath + "/" + relativePath;

        if (!sequencedNodes.containsKey(expectedPath)) {
            createWaitingLatchIfNecessary(expectedPath);
            logger.debug("Waiting for sequenced node at: " + expectedPath);
            waitingLatches.get(expectedPath).await(waitTimeSeconds, TimeUnit.SECONDS);
        }
        waitingLatches.remove(expectedPath);
        return sequencedNodes.remove(expectedPath);
    }

    private void createWaitingLatchIfNecessary( String expectedPath ) throws InterruptedException {
        waitingLatches.putIfAbsent(expectedPath, new CountDownLatch(1));
    }

    private void assertSequencingEvent( Event event ) throws RepositoryException {
        assertEquals(NODE_SEQUENCED, event.getType());

        Map info = event.getInfo();
        assertNotNull(info);
        assertNotNull(info.get(Event.Info.SEQUENCED_NODE_ID));
        assertNotNull(info.get(Event.Info.SEQUENCED_NODE_PATH));

        assertNotNull(event.getIdentifier());
        assertNotNull(event.getPath());
    }

    private class SequencingListener implements EventListener {

        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    Event event = (Event)events.nextEvent();
                    assertSequencingEvent(event);

                    String nodePath = event.getPath();
                    logger.debug("New sequenced node at: " + nodePath);
                    sequencedNodes.put(nodePath, session.getNode(nodePath));

                    // signal the node is available
                    createWaitingLatchIfNecessary(nodePath);
                    waitingLatches.get(nodePath).countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
