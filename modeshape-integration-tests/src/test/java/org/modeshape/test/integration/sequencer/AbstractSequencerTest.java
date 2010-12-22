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
package org.modeshape.test.integration.sequencer;

import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.text.Inflector;
import org.modeshape.test.integration.AbstractSingleUseModeShapeTest;

public abstract class AbstractSequencerTest extends AbstractSingleUseModeShapeTest {

    /**
     * Return a new listener that accumulates the nodes that have been deleted.
     * 
     * @return the new listener
     * @throws RepositoryException
     */
    public DeleteListener registerListenerForDeletes() throws RepositoryException {
        return registerListenerForDeletesBelow("/");
    }

    public DeleteListener registerListenerForDeletesBelow( String path ) throws RepositoryException {
        DeleteListener listener = new DeleteListener(session(), path);
        listener.register();
        return listener;
    }

    public static class DeleteListener implements EventListener {

        private final Session session;
        private final String path;
        private List<List<String>> deletedPaths = new ArrayList<List<String>>();
        private boolean isRegistered = false;

        protected DeleteListener( Session session,
                                  String path ) {
            this.session = session;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        @Override
        public void onEvent( EventIterator events ) {
            try {
                List<String> deleted = new ArrayList<String>();
                while (events.hasNext()) {
                    Event event = events.nextEvent();
                    deleted.add(event.getPath());
                }
                deletedPaths.add(deleted);
            } catch (RepositoryException e) {
                throw new SystemFailureException(e);
            }
        }

        public void clear() {
            this.deletedPaths.clear();
        }

        public int size() {
            int count = 0;
            for (List<String> list : deletedPaths) {
                count += list.size();
            }
            return count;
        }

        /**
         * @return deletedPaths
         */
        public List<List<String>> getDeletedPaths() {
            return deletedPaths;
        }

        public void register() throws RepositoryException {
            if (isRegistered) return;
            session.getWorkspace().getObservationManager().addEventListener(this,
                                                                            Event.NODE_REMOVED,
                                                                            path,
                                                                            true,
                                                                            null,
                                                                            null,
                                                                            false);
            isRegistered = true;
        }

        public void unregister() throws RepositoryException {
            this.session.getWorkspace().getObservationManager().removeEventListener(this);
            isRegistered = false;
        }

        /**
         * Wait at most for the specified time until delete events for nodes the supplied paths have been received. If not all
         * events are seen after the supplied time, this method will cause a test failure.
         * 
         * @param maxTimeToWait the maximum time to wait
         * @param unit the time unit for the maximum time to wait
         * @param paths the paths that are to be deleted (must not be descendants of the deleted nodes)
         * @return the set of paths that were not found after timeout; if empty, then all expected events were found
         */
        public Set<String> waitForDeleted( long maxTimeToWait,
                                           TimeUnit unit,
                                           String... paths ) {
            return waitForDeleted(maxTimeToWait, unit, true, paths);
        }

        /**
         * Wait at most for the specified time until delete events for nodes the supplied paths have been received.
         * 
         * @param maxTimeToWait the maximum time to wait
         * @param unit the time unit for the maximum time to wait
         * @param failIfNotAllFound true if this method should cause a test failure should not all events be found, or false if
         *        this method should return upon failure
         * @param paths the paths that are to be deleted (must not be descendants of the deleted nodes)
         * @return the set of paths that were not found after timeout; if empty, then all expected events were found
         */
        public Set<String> waitForDeleted( long maxTimeToWait,
                                           TimeUnit unit,
                                           boolean failIfNotAllFound,
                                           String... paths ) {
            assert paths != null;
            Set<String> remainingPaths = new HashSet<String>();
            for (String path : paths) {
                if (path != null) remainingPaths.add(path);
            }
            if (remainingPaths.isEmpty()) return remainingPaths;

            long maxTimeToWaitInMillis = unit.toMillis(maxTimeToWait);
            long startTime = System.currentTimeMillis();
            long waitedInMillis = 0L;
            while (waitedInMillis < maxTimeToWaitInMillis) {
                for (List<String> list : deletedPaths) {
                    remainingPaths.removeAll(list);
                    if (remainingPaths.isEmpty()) return remainingPaths;
                }
                waitedInMillis = System.currentTimeMillis() - startTime;
                if (waitedInMillis < maxTimeToWaitInMillis) {
                    // We haven't reached the tim limit, so wait ...
                    try {
                        Thread.sleep(Math.min(50L, maxTimeToWaitInMillis - waitedInMillis));
                    } catch (InterruptedException e) {
                        break;
                    }
                    waitedInMillis = System.currentTimeMillis() - startTime;
                }
            }
            if (failIfNotAllFound && !remainingPaths.isEmpty()) {
                Inflector inflector = new Inflector();
                String unitName = inflector.pluralize(unit.toString().toLowerCase(), (int)maxTimeToWait);
                fail("Waited for " + maxTimeToWait + " " + unitName + " but didn't see events for deletion of: " + remainingPaths
                     + ". Did see these deletions: " + deletedPaths);
            }
            return remainingPaths;
        }

    }

}
