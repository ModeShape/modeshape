/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.repository.util.SessionFactory;

/**
 * @author Randall Hauch
 */
public class ObservationService implements AdministeredService {

    /**
     * Interface to which problems with particular events are logged.
     * @author Randall Hauch
     */
    public static interface ProblemLog {

        void error( String repositoryWorkspaceName, Throwable t );
    }

    /**
     * Problem log implementation that records problems in the log.
     * @author Randall Hauch
     */
    public class DefaultProblemLog implements ProblemLog {

        /**
         * {@inheritDoc}
         */
        public void error( String repositoryWorkspaceName, Throwable t ) {
            getLogger().error(t, RepositoryI18n.errorProcessingEvents, repositoryWorkspaceName);
        }
    }

    protected static class NoOpProblemLog implements ProblemLog {

        /**
         * {@inheritDoc}
         */
        public void error( String repositoryWorkspaceName, Throwable t ) {
        }
    }

    public static final ProblemLog NO_OP_PROBLEM_LOG = new NoOpProblemLog();

    /**
     * The administrative component for this service.
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(State.STARTED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String serviceName() {
            return "ObservationService";
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout, TimeUnit unit ) {
            return true;
        }

    }

    private Logger logger = Logger.getLogger(this.getClass());
    private ProblemLog problemLog = new DefaultProblemLog();
    private final Statistics statistics = new Statistics();
    private final SessionFactory sessionFactory;
    private final CopyOnWriteArrayList<EventListener> eventListeners = new CopyOnWriteArrayList<EventListener>();
    private final CopyOnWriteArrayList<NodeChangeListener> nodeChangeListeners = new CopyOnWriteArrayList<NodeChangeListener>();
    private final Administrator administrator = new Administrator();

    public ObservationService( SessionFactory sessionFactory ) {
        ArgCheck.isNotNull(sessionFactory, "session factory");
        this.sessionFactory = sessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * Get the statistics for this system.
     * @return the statistics, which are updated as the system is used
     */
    public Statistics getStatistics() {
        return this.statistics;
    }

    /**
     * Get the logger for this system
     * @return the logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Set the logger for this system.
     * @param logger the logger, or null if the standard logging should be used
     */
    public void setLogger( Logger logger ) {
        this.logger = logger != null ? logger : Logger.getLogger(this.getClass());
    }

    /**
     * @return problemLog
     */
    public ProblemLog getProblemLog() {
        return this.problemLog;
    }

    /**
     * Set the problem log that will be notified of problems handling events. By default, such problems are sent to the log.
     * @param problemLog the new problem log implementation; if null, then the default problem log is used
     */
    public void setProblemLog( ProblemLog problemLog ) {
        this.problemLog = problemLog != null ? problemLog : new DefaultProblemLog();
    }

    public boolean addListener( EventListener listener ) {
        if (listener == null) return false;
        return this.eventListeners.addIfAbsent(listener);
    }

    public boolean removeListener( EventListener listener ) {
        if (listener == null) return false;
        return this.eventListeners.remove(listener);
    }

    public boolean addListener( NodeChangeListener listener ) {
        return this.nodeChangeListeners.addIfAbsent(listener);
    }

    public boolean removeListener( NodeChangeListener listener ) {
        if (listener == null) return false;
        return this.nodeChangeListeners.remove(listener);
    }

    /**
     * Monitor the supplied workspace for events of the given type on any node at or under the supplied path.
     * <p>
     * Monitoring is accomplished by registering a listener on the workspace, so this monitoring only has access to the
     * information that visible to the session created by the {@link #getSessionFactory() session factory} for the given
     * repository and workspace name.
     * </p>
     * <p>
     * The listener returned from this method is not managed by this SequencingService instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected. If this service is {@link ServiceAdministrator#shutdown() shutdown} while there are still
     * active listeners, those listeners will disconnect themselves from this service and the workspace with which they're
     * registered when they attempt to forward the next events.
     * </p>
     * <p>
     * The set of events that are monitored can be filtered by specifying restrictions based on characteristics of the node
     * associated with the event. In the case of event types {@link Event#NODE_ADDED NODE_ADDED} and
     * {@link Event#NODE_REMOVED NODE_REMOVED}, the node associated with an event is the node at (or formerly at) the path
     * returned by {@link Event#getPath() Event.getPath()}. In the case of event types
     * {@link Event#PROPERTY_ADDED PROPERTY_ADDED}, {@link Event#PROPERTY_REMOVED PROPERTY_REMOVED} and
     * {@link Event#PROPERTY_CHANGED PROPERTY_CHANGED}, the node associated with an event is the parent node of the property at
     * (or formerly at) the path returned by <code>Event.getPath</code>:
     * <ul>
     * <li> <code>absolutePath</code>, <code>isDeep</code>: Only events whose associated node is at
     * <code>absolutePath</code> (or within its subtree, if <code>isDeep</code> is <code>true</code>) will be received. It
     * is permissible to register a listener for a path where no node currently exists. </li>
     * <li> <code>uuids</code>: Only events whose associated node has one of the UUIDs in this list will be received. If his
     * parameter is <code>null</code> then no UUID-related restriction is placed on events received. </li>
     * <li> <code>nodeTypeNames</code>: Only events whose associated node has one of the node types (or a subtype of one of the
     * node types) in this list will be received. If this parameter is <code>null</code> then no node type-related restriction
     * is placed on events received. </li>
     * </ul>
     * The restrictions are "ANDed" together. In other words, for a particular node to be "listened to" it must meet all the
     * restrictions.
     * </p>
     * <p>
     * Additionally, if <code>noLocal</code> is <code>true</code>, then events generated by the session through which the
     * listener was registered are ignored. Otherwise, they are not ignored.
     * </p>
     * <p>
     * The filters of an already-registered {@link WorkspaceListener} can be changed at runtime by changing the attributes and
     * {@link WorkspaceListener#reregister() registering}.
     * </p>
     * @param repositoryWorkspaceName the name to be used with the session factory to obtain a session to the repository and
     * workspace that is to be monitored
     * @param absolutePath the absolute path of the node at or below which changes are to be monitored; may be null if all nodes
     * in the workspace are to be monitored
     * @param eventTypes the bitmask of the {@link Event} types that are to be monitored
     * @param isDeep true if events below the node given by the <code>absolutePath</code> or by the <code>uuids</code> are to
     * be processed, or false if only the events at the node
     * @param uuids array of UUIDs of nodes that are to be monitored; may be null or empty if the UUIDs are not known
     * @param nodeTypeNames array of node type names that are to be monitored; may be null or empty if the monitoring has no node
     * type restrictions
     * @param noLocal true if the events originating in the supplied workspace are to be ignored, or false if they are also to be
     * processed.
     * @return the listener that was created and registered to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( String repositoryWorkspaceName, String absolutePath, int eventTypes, boolean isDeep, String[] uuids, String[] nodeTypeNames, boolean noLocal )
        throws RepositoryException {
        WorkspaceListener listener = new WorkspaceListener(repositoryWorkspaceName, eventTypes, absolutePath, isDeep, uuids, nodeTypeNames, noLocal);
        listener.register();
        return listener;
    }

    /**
     * Monitor the supplied workspace for {@link WorkspaceListener#DEFAULT_EVENT_TYPES default event types} on any node at or
     * under the supplied path.
     * <p>
     * Monitoring is accomplished by registering a listener on the workspace, so this monitoring only has access to the
     * information that visible to the session created by the {@link #getSessionFactory() session factory} for the given
     * repository and workspace name.
     * </p>
     * <p>
     * The listener returned from this method is not managed by this SequencingService instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected.
     * </p>
     * @param repositoryWorkspaceName the name to be used with the session factory to obtain a session to the repository and
     * workspace that is to be monitored
     * @param absolutePath the absolute path of the node at or below which changes are to be monitored; may be null if all nodes
     * in the workspace are to be monitored
     * @param nodeTypeNames the names of the node types that are to be monitored; may be null or empty if the monitoring has no
     * node type restrictions
     * @return the listener that was created and registered to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( String repositoryWorkspaceName, String absolutePath, String... nodeTypeNames ) throws RepositoryException {
        return monitor(repositoryWorkspaceName, absolutePath, WorkspaceListener.DEFAULT_EVENT_TYPES, WorkspaceListener.DEFAULT_IS_DEEP, null, nodeTypeNames, WorkspaceListener.DEFAULT_NO_LOCAL);
    }

    /**
     * Monitor the supplied workspace for the supplied event types on any node in the workspace.
     * <p>
     * Monitoring is accomplished by registering a listener on the workspace, so this monitoring only has access to the
     * information that visible to the session created by the {@link #getSessionFactory() session factory} for the given
     * repository and workspace name.
     * </p>
     * <p>
     * The listener returned from this method is not managed by this SequencingService instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected.
     * </p>
     * @param repositoryWorkspaceName the name to be used with the session factory to obtain a session to the repository and
     * workspace that is to be monitored
     * @param eventTypes the bitmask of the {@link Event} types that are to be monitored
     * @param nodeTypeNames the names of the node types that are to be monitored; may be null or empty if the monitoring has no
     * node type restrictions
     * @return the listener that was created and registered to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( String repositoryWorkspaceName, int eventTypes, String... nodeTypeNames ) throws RepositoryException {
        return monitor(repositoryWorkspaceName, WorkspaceListener.DEFAULT_ABSOLUTE_PATH, eventTypes, WorkspaceListener.DEFAULT_IS_DEEP, null, nodeTypeNames, WorkspaceListener.DEFAULT_NO_LOCAL);
    }

    /**
     * From section 2.8.8 of the JSR-170 specification:
     * <p>
     * On each persistent change, those listeners that are entitled to receive one or more events will have their onEvent method
     * called and be passed an EventIterator. The EventIterator will contain the event bundle reflecting the persistent changes
     * made but excluding those to which that particular listener is not entitled, according to the listeners access permissions
     * and filters.
     * </p>
     * @param events
     * @param listener
     */
    protected void processEvents( EventIterator eventIterator, WorkspaceListener listener ) {
        if (eventIterator == null) return;
        List<Event> events = new ArrayList<Event>();
        // Copy the events ...
        while (eventIterator.hasNext()) {
            events.add((Event)eventIterator.next());
        }
        if (!getAdministrator().isStarted()) {
            this.statistics.recordIgnoredEventSet(events.size());
            return;
        }

        // Notify the event listeners ...
        boolean notifiedSomebody = false;
        List<EventListener> eventListeners = this.eventListeners; // use one consistent snapshot
        if (!eventListeners.isEmpty()) {
            DelegatingEventIterator eventIter = new DelegatingEventIterator(events.iterator(), events.size());
            for (EventListener eventListener : eventListeners) {
                eventListener.onEvent(eventIter);
            }
            notifiedSomebody = true;
        }

        // Now create the node change events ...
        List<NodeChangeListener> nodeChangeListeners = this.nodeChangeListeners; // use one consistent snapshot
        if (!nodeChangeListeners.isEmpty()) {
            final String repositoryWorkspaceName = listener.getRepositoryWorkspaceName();
            try {
                NodeChanges nodeChanges = NodeChanges.create(repositoryWorkspaceName, events);

                // And notify the node change listeners ...
                int nodeChangeCount = nodeChanges.size();
                this.statistics.recordNodesChanged(nodeChangeCount);
                for (NodeChangeListener nodeChangeListener : nodeChangeListeners) {
                    nodeChangeListener.onNodeChanges(nodeChanges);
                }
            } catch (Throwable t) {
                getProblemLog().error(repositoryWorkspaceName, t);
            }
            notifiedSomebody = true;
        }

        if (notifiedSomebody) {
            this.statistics.recordEventSet(events.size());
        } else {
            this.statistics.recordIgnoredEventSet(events.size());
        }
    }

    protected class DelegatingEventIterator implements EventIterator {

        private final Iterator<Event> events;
        private final int size;
        private int position = 0;

        protected DelegatingEventIterator( Iterator<Event> events, int size ) {
            this.events = events;
            this.size = size;
        }

        /**
         * {@inheritDoc}
         */
        public Event nextEvent() {
            ++position;
            return events.next();
        }

        /**
         * {@inheritDoc}
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            return size;
        }

        /**
         * {@inheritDoc}
         */
        public void skip( long skipNum ) {
            for (int i = 0; i != skipNum; ++i) {
                next();
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return events.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return events.next();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            // does nothing
        }

    }

    /**
     * Implementation of the {@link EventListener JCR EventListener} interface, returned by the sequencing system.
     * @author Randall Hauch
     */
    @ThreadSafe
    public class WorkspaceListener implements EventListener {

        public static final boolean DEFAULT_IS_DEEP = true;
        public static final boolean DEFAULT_NO_LOCAL = false;
        public static final int DEFAULT_EVENT_TYPES = Event.NODE_ADDED | /* Event.NODE_REMOVED| */Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED /* |Event.PROPERTY_REMOVED */;
        public static final String DEFAULT_ABSOLUTE_PATH = "/";

        private final String repositoryWorkspaceName;
        private final Set<String> uuids;
        private final Set<String> nodeTypeNames;
        private final int eventTypes;
        private final String absolutePath;
        private final boolean deep;
        private final boolean noLocal;
        @GuardedBy( "this" )
        private transient Session session;

        protected WorkspaceListener( String repositoryWorkspaceName, int eventTypes, String absPath, boolean isDeep, String[] uuids, String[] nodeTypeNames, boolean noLocal ) {
            this.repositoryWorkspaceName = repositoryWorkspaceName;
            this.eventTypes = eventTypes;
            this.deep = isDeep;
            this.noLocal = noLocal;
            this.absolutePath = absPath != null && absPath.trim().length() != 0 ? absPath.trim() : null;
            // Set the UUIDs ...
            Set<String> newUuids = new HashSet<String>();
            if (uuids != null) {
                for (String uuid : uuids) {
                    if (uuid != null && uuid.trim().length() != 0) newUuids.add(uuid.trim());
                }
            }
            this.uuids = Collections.unmodifiableSet(newUuids);
            // Set the node type names
            Set<String> newNodeTypeNames = new HashSet<String>();
            if (nodeTypeNames != null) {
                for (String nodeTypeName : nodeTypeNames) {
                    if (nodeTypeName != null && nodeTypeName.trim().length() != 0) newNodeTypeNames.add(nodeTypeName.trim());
                }
            }
            this.nodeTypeNames = Collections.unmodifiableSet(newNodeTypeNames);
        }

        /**
         * @return repositoryWorkspaceName
         */
        public String getRepositoryWorkspaceName() {
            return this.repositoryWorkspaceName;
        }

        /**
         * @return eventTypes
         */
        public int getEventTypes() {
            return this.eventTypes;
        }

        /**
         * @return absolutePath
         */
        public String getAbsolutePath() {
            return this.absolutePath;
        }

        /**
         * @return deep
         */
        public boolean isDeep() {
            return this.deep;
        }

        /**
         * @return noLocal
         */
        public boolean isNoLocal() {
            return this.noLocal;
        }

        /**
         * @return uuids
         */
        public Set<String> getUuids() {
            return this.uuids;
        }

        /**
         * @return nodeTypeNames
         */
        public Set<String> getNodeTypeNames() {
            return this.nodeTypeNames;
        }

        public synchronized boolean isRegistered() {
            if (this.session != null && getAdministrator().isShutdown()) {
                // This sequencing system has been shutdown, so unregister this listener
                try {
                    unregister();
                } catch (RepositoryException re) {
                    String msg = "Error unregistering workspace listener after sequencing system has been shutdow.";
                    Logger.getLogger(this.getClass()).debug(re, msg);
                }
            }
            return this.session != null;
        }

        public synchronized WorkspaceListener register() throws UnsupportedRepositoryOperationException, RepositoryException {
            if (this.session != null) return this;
            this.session = ObservationService.this.getSessionFactory().createSession(this.repositoryWorkspaceName);
            String[] uuids = this.uuids.isEmpty() ? null : this.uuids.toArray(new String[this.uuids.size()]);
            String[] nodeTypeNames = this.nodeTypeNames.isEmpty() ? null : this.nodeTypeNames.toArray(new String[this.nodeTypeNames.size()]);
            this.session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, absolutePath, deep, uuids, nodeTypeNames, noLocal);
            return this;
        }

        public synchronized WorkspaceListener unregister() throws UnsupportedRepositoryOperationException, RepositoryException {
            if (this.session == null) return this;
            try {
                this.session.getWorkspace().getObservationManager().removeEventListener(this);
                this.session.logout();
            } finally {
                this.session = null;
            }
            return this;
        }

        public synchronized WorkspaceListener reregister() throws UnsupportedRepositoryOperationException, RepositoryException {
            unregister();
            register();
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void onEvent( EventIterator events ) {
            if (events != null) {
                if (getAdministrator().isShutdown()) {
                    // This sequencing system has been shutdown, so unregister this listener
                    try {
                        unregister();
                    } catch (RepositoryException re) {
                        String msg = "Error unregistering workspace listener after sequencing system has been shutdow.";
                        Logger.getLogger(this.getClass()).debug(re, msg);
                    }
                } else {
                    ObservationService.this.processEvents(events, this);
                }
            }
        }
    }

    /**
     * The statistics for the system. Each sequencing system has an instance of this class that is updated.
     * @author Randall Hauch
     */
    @ThreadSafe
    public class Statistics {

        @GuardedBy( "lock" )
        private long numberOfEventsIgnored;
        @GuardedBy( "lock" )
        private long numberOfEventsEnqueued;
        @GuardedBy( "lock" )
        private long numberOfEventSetsIgnored;
        @GuardedBy( "lock" )
        private long numberOfEventSetsEnqueued;
        private final AtomicLong numberOfNodeChangesEnqueued = new AtomicLong(0);
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final AtomicLong startTime;

        protected Statistics() {
            startTime = new AtomicLong(System.currentTimeMillis());
        }

        public Statistics reset() {
            try {
                lock.writeLock().lock();
                this.startTime.set(System.currentTimeMillis());
                this.numberOfEventsIgnored = 0;
                this.numberOfEventsEnqueued = 0;
                this.numberOfEventSetsIgnored = 0;
                this.numberOfEventSetsEnqueued = 0;
                this.numberOfNodeChangesEnqueued.set(0);
            } finally {
                lock.writeLock().unlock();
            }
            return this;
        }

        /**
         * @return the system time when the statistics were started
         */
        public long getStartTime() {
            return this.startTime.get();
        }

        /**
         * @return the number of node changes that were processed
         */
        public long getNumberOfNodeChangesEnqueued() {
            return this.numberOfNodeChangesEnqueued.get();
        }

        /**
         * @return the number of events that were ignored because the system was not running
         */
        public long getNumberOfEventsIgnored() {
            try {
                lock.readLock().lock();
                return this.numberOfEventsIgnored;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * @return the number of events that were enqueued for processing
         */
        public long getNumberOfEventsEnqueued() {
            try {
                lock.readLock().lock();
                return this.numberOfEventsEnqueued;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * @return the number of event sets (transactions) that were enqueued for processing
         */
        public long getNumberOfEventSetsEnqueued() {
            try {
                lock.readLock().lock();
                return this.numberOfEventSetsEnqueued;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * @return the number of event sets (transactions) that were ignored because the system was not running
         */
        public long getNumberOfEventSetsIgnored() {
            try {
                lock.readLock().lock();
                return this.numberOfEventSetsIgnored;
            } finally {
                lock.readLock().unlock();
            }
        }

        protected void recordNodesChanged( long changeCount ) {
            this.numberOfNodeChangesEnqueued.addAndGet(changeCount);
        }

        protected void recordEventSet( long eventsInSet ) {
            try {
                lock.writeLock().lock();
                this.numberOfEventsEnqueued += eventsInSet;
                ++this.numberOfEventSetsEnqueued;
            } finally {
                lock.writeLock().unlock();
            }
        }

        protected void recordIgnoredEventSet( long eventsInSet ) {
            try {
                lock.writeLock().lock();
                this.numberOfEventsIgnored += eventsInSet;
                this.numberOfEventSetsIgnored += 1;
                ++this.numberOfEventSetsEnqueued;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
