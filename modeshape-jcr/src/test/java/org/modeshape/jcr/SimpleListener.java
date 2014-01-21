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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;

/**
 * Test implementation of an {@link javax.jcr.observation.EventListener}
 */
public class SimpleListener implements EventListener {

    private static final Logger LOGGER = Logger.getLogger(SimpleListener.class);

    private String errorMessage;
    protected final List<Event> events;
    protected final List<String> userData;
    private int eventsProcessed = 0;
    protected final int eventTypes;
    protected final int expectedEventsCount;
    protected String expectedNodePrimaryType;
    protected TreeSet<String> expectedNodeMixinTypes;
    protected final CountDownLatch latch;

    public SimpleListener( int expectedEventsCount,
                           int numIterators,
                           int eventTypes ) {
        this.eventTypes = eventTypes;
        this.expectedEventsCount = expectedEventsCount;
        this.events = new ArrayList<Event>();
        this.userData = new ArrayList<String>();
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
        return this.expectedEventsCount;
    }

    public SimpleListener withExpectedNodePrimaryType(String nodePrimaryType) {
        this.expectedNodePrimaryType = nodePrimaryType;
        return this;
    }

    public SimpleListener withExpectedNodeMixinTypes(String... mixinTypes) {
        this.expectedNodeMixinTypes = new TreeSet<String>(Arrays.asList(mixinTypes));
        return this;
    }

    @Override
    public void onEvent( EventIterator itr ) {
        // this is called each time a "transaction" is committed. Most times this means after a session.save. But there are
        // other times, like a workspace.move and a node.lock
        try {
            long position = itr.getPosition();

            // iterator position must be set initially zero
            if (position == 0) {
                while (itr.hasNext()) {
                    org.modeshape.jcr.api.observation.Event event = (org.modeshape.jcr.api.observation.Event)itr.nextEvent();
                    // System.out.println(event + " from " + this);

                    // check iterator position
                    if (++position != itr.getPosition()) {
                        this.errorMessage = "EventIterator position was " + itr.getPosition() + " and should be " + position;
                        break;
                    }

                    try {
                        String userData = event.getUserData();
                        this.userData.add(userData);
                    } catch (RepositoryException e) {
                        LOGGER.debug(e, "Listener exception");
                        this.errorMessage = e.getMessage();
                    }

                    // add event to collection and increment total
                    this.events.add(event);
                    ++this.eventsProcessed;

                    // check to make sure we haven't received too many events
                    if (this.eventsProcessed > this.expectedEventsCount) {
                        break;
                    }

                    // check event type
                    int eventType = event.getType();

                    if ((this.eventTypes & eventType) == 0) {
                        this.errorMessage = "Received a wrong event type of " + eventType;
                        break;
                    }

                    if (!StringUtil.isBlank(expectedNodePrimaryType)) {
                        try {
                            String actualNodeType = event.getPrimaryNodeType().getName();
                            if (!actualNodeType.equalsIgnoreCase(expectedNodePrimaryType)) {
                                this.errorMessage = "Incorrect node primary type. Expected " + expectedNodePrimaryType + " but received " + actualNodeType;
                                break;
                            }
                        } catch (RepositoryException e) {
                            LOGGER.debug(e, "Listener exception");
                            this.errorMessage = e.getMessage();
                            break;
                        }
                    }

                    if (expectedNodeMixinTypes != null) {
                        try {
                            Set<String> actualNodeMixins = new TreeSet<String>();
                            for (NodeType mixin : event.getMixinNodeTypes()) {
                                actualNodeMixins.add(mixin.getName());
                            }
                            if (!expectedNodeMixinTypes.equals(actualNodeMixins))  {
                                this.errorMessage = "Incorrect node mixins. Expected " + expectedNodeMixinTypes + " but received " + actualNodeMixins;
                            }
                        } catch (RepositoryException e) {
                            LOGGER.debug(e, "Listener exception");
                            this.errorMessage = e.getMessage();
                            break;
                        }
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
        long millis = this.expectedEventsCount == 0 ? 50 : 500;
        this.latch.await(millis, TimeUnit.MILLISECONDS);
    }
}
