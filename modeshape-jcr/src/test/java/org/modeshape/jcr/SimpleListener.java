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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * Test implementation of an {@link javax.jcr.observation.EventListener}
 */
public class SimpleListener implements EventListener {

    private String errorMessage;
    protected final List<Event> events;
    protected final List<String> userData;
    private int eventsProcessed = 0;
    protected final int eventTypes;
    protected final int expectedEventsCount;
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

    @Override
    public void onEvent( EventIterator itr ) {
        // this is called each time a "transaction" is committed. Most times this means after a session.save. But there are
        // other times, like a workspace.move and a node.lock
        try {
            long position = itr.getPosition();

            // iterator position must be set initially zero
            if (position == 0) {
                while (itr.hasNext()) {
                    Event event = itr.nextEvent();
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
