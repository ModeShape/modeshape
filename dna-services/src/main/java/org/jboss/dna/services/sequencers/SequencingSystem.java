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
package org.jboss.dna.services.sequencers;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.HashCodeUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.services.util.SessionFactory;

/**
 * A sequencing system is used to monitor changes in the content of {@link Repository JCR repositories} and to sequence the
 * content to extract or to generate structured information.
 * @author Randall Hauch
 */
public class SequencingSystem {

    /**
     * Interface used to select the set of {@link Sequencer} instances that should be run.
     * @author Randall Hauch
     */
    public static interface Selector {

        /**
         * Select the sequencers that should be used to sequence the supplied node.
         * @param sequencers the list of all sequencers available at the moment; never null
         * @param node the node to be sequenced; never null
         * @return the list of sequencers that should be used; may not be null
         */
        List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node );
    }

    /**
     * Interface that is used by the {@link SequencingSystem#setEventFilter(SequencingSystem.EventFilter) SequencingSystem} to
     * filter observed events
     * @author Randall Hauch
     */
    public static interface EventFilter {

        /**
         * Determine whether the supplied event is considered interesting and signals that the node should be sequenced.
         * @param event the event being considered; never null
         * @return true if the event is interesting and should be sequenced
         */
        boolean includeEvent( Event event );
    }

    /**
     * The default {@link Selector} implementation that selects every sequencer every time it's called, regardless of the node (or
     * logger) supplied.
     * @author Randall Hauch
     */
    protected static class DefaultSelector implements Selector {

        public List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node ) {
            return sequencers;
        }
    }

    /**
     * The default {@link EventFilter} implementation that considers every event to be interesting.
     * @author Randall Hauch
     */
    protected static class DefaultEventFilter implements EventFilter {

        public boolean includeEvent( Event event ) {
            return true;
        }
    }

    /**
     * Interface to which problems with particular events are logged.
     * @author Randall Hauch
     */
    public static interface ProblemLog {

        void error( String repositoryWorkspaceName, Event event, Throwable t );
    }

    public class DefaultProblemLog implements ProblemLog {

        /**
         * {@inheritDoc}
         */
        public void error( String repositoryWorkspaceName, Event event, Throwable t ) {
            String type = getEventTypeString(event);
            String path = "<unable-to-get-path>";
            try {
                path = event.getPath();
            } catch (RepositoryException e) {
                getLogger().error(e, "Unable to get node path from {} event from repository workspace {}", type, repositoryWorkspaceName);
            }
            getLogger().error(t, "Error processing {} event on node {}=>{}", type, repositoryWorkspaceName, path);
        }

        protected String getEventTypeString( Event event ) {
            assert event != null;
            switch (event.getType()) {
                case Event.NODE_ADDED:
                    return "NODE_ADDED";
                case Event.NODE_REMOVED:
                    return "NODE_REMOVED";
                case Event.PROPERTY_ADDED:
                    return "PROPERTY_ADDED";
                case Event.PROPERTY_CHANGED:
                    return "PROPERTY_CHANGED";
                case Event.PROPERTY_REMOVED:
                    return "PROPERTY_REMOVED";
            }
            return "unknown event type " + event.getType();
        }
    }

    /**
     * The default {@link Selector} that considers every {@link Sequencer} to be used for every node.
     * @see SequencingSystem#setSequencerSelector(org.jboss.dna.services.sequencers.SequencingSystem.Selector)
     */
    public static final Selector DEFAULT_SEQUENCER_SELECTOR = new DefaultSelector();

    /**
     * The default {@link EventFilter} that considers every event to be interesting.
     * @see SequencingSystem#setEventFilter(SequencingSystem.EventFilter)
     */
    public static final EventFilter DEFAULT_EVENT_FILTER = new DefaultEventFilter();

    public static enum State {
        STARTED,
        PAUSED,
        SHUTDOWN;
    }

    private State state = State.PAUSED;
    private SessionFactory sessionFactory;
    private EventFilter eventFilter = DEFAULT_EVENT_FILTER;
    private SequencerLibrary sequencerLibrary = new SequencerLibrary();
    private Selector sequencerSelector = DEFAULT_SEQUENCER_SELECTOR;
    private ExecutorService executorService;
    private Logger logger = Logger.getLogger(this.getClass());
    private ProblemLog problemLog = new DefaultProblemLog();
    private final Statistics statistics = new Statistics();

    /**
     * Create a new sequencing system, configured with no sequencers and not monitoring any workspaces. Upon construction, the
     * system is {@link #isPaused() paused} and must be configured and then {@link #start() started}.
     */
    public SequencingSystem() {
    }

    /**
     * Get the statistics for this system.
     * @return statistics
     */
    public Statistics getStatistics() {
        return this.statistics;
    }

    /**
     * Get the library that is managing the {@link Sequencer sequencer} instances.
     * @return the sequencer library
     */
    public SequencerLibrary getSequencerLibrary() {
        return this.sequencerLibrary;
    }

    /**
     * @param sequencerLibrary Sets sequencerLibrary to the specified value.
     */
    public void setSequencerLibrary( SequencerLibrary sequencerLibrary ) {
        this.sequencerLibrary = sequencerLibrary != null ? sequencerLibrary : new SequencerLibrary();
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
     * @return sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * @param sessionFactory Sets sessionFactory to the specified value.
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        if (sessionFactory == null) {
            throw new IllegalArgumentException("The session factory parameter may not be null");
        }
        if (this.isStarted()) {
            throw new IllegalStateException("Unable to change the session factory while running");
        }
        this.sessionFactory = sessionFactory;
    }

    /**
     * Get the executor service used to run the sequencers.
     * @return the executor service
     * @see #setExecutorService(ExecutorService)
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Set the executor service that should be used by this system. By default, the system is set up with a
     * {@link Executors#newSingleThreadExecutor() executor that uses a single thread}.
     * @param executorService the executor service
     * @see #getExecutorService()
     * @see Executors#newCachedThreadPool()
     * @see Executors#newCachedThreadPool(java.util.concurrent.ThreadFactory)
     * @see Executors#newFixedThreadPool(int)
     * @see Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newScheduledThreadPool(int)
     * @see Executors#newScheduledThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadExecutor()
     * @see Executors#newSingleThreadExecutor(java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadScheduledExecutor()
     * @see Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory)
     */
    public void setExecutorService( ExecutorService executorService ) {
        if (sequencerLibrary == null) {
            throw new IllegalArgumentException("The executor service parameter may not be null");
        }
        if (this.isStarted()) {
            throw new IllegalStateException("Unable to change the executor service while running");
        }
        this.executorService = executorService;
    }

    /**
     * Override this method to creates a different kind of default executor service. This method is called when the system is
     * {@link #start() started} without an executor service being {@link #setExecutorService(ExecutorService) set}.
     * <p>
     * This method creates a {@link Executors#newSingleThreadExecutor() single-threaded executor}.
     * </p>
     * @return
     */
    protected ExecutorService createDefaultExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * Return the current state of this system.
     * @return the current state
     */
    public State getState() {
        return this.state;
    }

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * @param state the desired state
     * @return this object for method chaining purposes
     * @see #setState(String)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public SequencingSystem setState( State state ) {
        switch (state) {
            case STARTED:
                return start();
            case PAUSED:
                return pause();
            case SHUTDOWN:
                return shutdown();
        }
        return this;
    }

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * @param state the desired state in string form
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if the specified state string is null or does not match one of the predefined
     * {@link State predefined enumerated values}
     * @see #setState(org.jboss.dna.services.sequencers.SequencingSystem.State)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public SequencingSystem setState( String state ) {
        State newState = state == null ? null : State.valueOf(state.toUpperCase());
        if (newState == null) {
            throw new IllegalArgumentException("Invalid state parameter");
        }
        return setState(newState);
    }

    /**
     * Start monitoring and sequence the events. This method can be called multiple times, including after the system is
     * {@link #pause() paused}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #pause()
     * @see #shutdown()
     * @see #isStarted()
     */
    public synchronized SequencingSystem start() {
        if (this.state != State.STARTED) {
            if (this.executorService == null) {
                this.executorService = createDefaultExecutorService();
            }
            if (this.getSessionFactory() == null) {
                throw new IllegalStateException("Unable to start the sequencing system without a session factory");
            }
            assert this.executorService != null;
            assert this.eventFilter != null;
            assert this.sequencerSelector != null;
            assert this.sequencerLibrary != null;
            this.state = State.STARTED;
        }
        return this;
    }

    /**
     * Temporarily stop monitoring and sequencing events. This method can be called multiple times, including after the system is
     * {@link #start() started}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #start()
     * @see #shutdown()
     * @see #isPaused()
     */
    public synchronized SequencingSystem pause() {
        this.state = State.PAUSED;
        return this;
    }

    /**
     * Permanently stop monitoring and sequencing events. This method can be called multiple times, but only the first call has an
     * effect. Once the system has been shutdown, it may not be {@link #start() restarted} or {@link #pause() paused}.
     * @return this object for method chaining purposes
     * @see #start()
     * @see #pause()
     * @see #isShutdown()
     */
    public synchronized SequencingSystem shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
        this.state = State.SHUTDOWN;
        return this;
    }

    /**
     * Return whether this system has been started and is currently running.
     * @return true if started and currently running, or false otherwise
     * @see #start()
     * @see #pause()
     * @see #isPaused()
     * @see #isShutdown()
     */
    public boolean isStarted() {
        return this.state == State.STARTED;
    }

    /**
     * Return whether this system is currently paused.
     * @return true if currently paused, or false otherwise
     * @see #pause()
     * @see #start()
     * @see #isStarted()
     * @see #isShutdown()
     */
    public boolean isPaused() {
        return this.state == State.PAUSED;
    }

    /**
     * Return whether this system is stopped and unable to be restarted.
     * @return true if currently shutdown, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     */
    public boolean isShutdown() {
        return this.state == State.SHUTDOWN;
    }

    /**
     * Get the event filter used by this system.
     * @return the event filter
     */
    public EventFilter getEventFilter() {
        return this.eventFilter;
    }

    /**
     * Set the event filter, or null if the {@link #DEFAULT_EVENT_FILTER default event filter} should be used.
     * @param filter the event filter
     */
    public void setEventFilter( EventFilter filter ) {
        this.eventFilter = filter != null ? filter : DEFAULT_EVENT_FILTER;
    }

    /**
     * Get the sequencing selector used by this system.
     * @return sequencerSelector
     */
    public Selector getSequencerSelector() {
        return this.sequencerSelector;
    }

    /**
     * Set the sequencer selector, or null if the {@link #DEFAULT_SEQUENCER_SELECTOR default sequencer selector} should be used.
     * @param sequencerSelector the selector
     */
    public void setSequencerSelector( Selector sequencerSelector ) {
        this.sequencerSelector = sequencerSelector != null ? sequencerSelector : DEFAULT_SEQUENCER_SELECTOR;
    }

    /**
     * Monitor the supplied workspace for events of the given type on any node at or under the supplied path.
     * <p>
     * Monitoring is accomplished by registering a listener on the workspace, so this monitoring only has access to the
     * information that visible to the session created by the {@link #getSessionFactory() session factory} for the given
     * repository and workspace name.
     * </p>
     * <p>
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected. If this service is {@link #shutdown() shutdown} while there are still active listeners,
     * those listeners will disconnect themselves from this service and the workspace with which they're registered when they
     * attempt to forward the next events.
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
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
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
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
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
    protected void enqueueEvents( EventIterator eventIterator, WorkspaceListener listener ) {
        if (eventIterator == null) return;
        if (this.state != State.STARTED) {
            int count = 0;
            // HACK: getSize() appears to return -1, so we have to loop ...
            // count = eventIterator.getSize();
            while (eventIterator.hasNext()) {
                eventIterator.next();
                ++count;
            }
            this.statistics.recordEventsIgnored(count);
            return;
        }
        assert this.executorService != null;
        final String repositoryWorkspaceName = listener.getRepositoryWorkspaceName();

        // Accumulate the list of changed nodes. Any event which has a path to a node that has already been seen in this
        // transaction or event list (according to the ChangedNode.hashCode() and ChangedNode.equals() methods) will not be added
        // to this set.
        long interestingCount = 0l;
        long uninterestingCount = 0l;
        Set<ChangedNode> nodesToProcess = new LinkedHashSet<ChangedNode>();
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                if (this.eventFilter.includeEvent(event)) {
                    final String absolutePath = event.getPath();
                    nodesToProcess.add(new ChangedNode(repositoryWorkspaceName, absolutePath));
                    ++interestingCount;
                } else {
                    ++uninterestingCount;
                }
            } catch (Throwable t) {
                this.problemLog.error(repositoryWorkspaceName, event, t);
            }
        }
        this.statistics.recordEvents(interestingCount, uninterestingCount);

        for (ChangedNode changedNode : nodesToProcess) {
            this.executorService.execute(changedNode);
        }
    }

    /**
     * Do the work of processing by sequencing the node. This method is called by the {@link #executorService executor service}
     * when it performs it's work on the enqueued {@link ChangedNode ChangedNode runnable objects}.
     * @param node the node to be processed.
     */
    protected void processChangedNode( ChangedNode changedNode ) {
        try {
            // Create a session that we'll use for all sequencing ...
            final Session session = this.getSessionFactory().createSession(changedNode.getRepositoryWorkspaceName());

            try {
                // Find the changed node ...
                String relPath = changedNode.getAbsolutePath().replaceAll("^/+", "");
                Node node = session.getRootNode().getNode(relPath);

                // Figure out which sequencers should run ...
                List<Sequencer> sequencers = this.sequencerLibrary.getSequencers();
                sequencers = this.sequencerSelector.selectSequencers(sequencers, node);
                // Run each of those sequencers ...
                if (sequencers.isEmpty()) {
                    this.statistics.recordNodeSkipped();
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Skipping '{}': no sequencers matched this condition", changedNode);
                    }
                } else {
                    for (Sequencer sequencer : sequencers) {
                        if (this.logger.isDebugEnabled()) {
                            String sequencerName = sequencer.getClass().getName();
                            SequencerConfig config = sequencer.getConfiguration();
                            if (config != null) {
                                sequencerName = config.getName();
                            }
                            String sequencerClassname = sequencer.getClass().getName();
                            this.logger.debug("Sequencing '{}' with {} ({})", changedNode, sequencerName, sequencerClassname);
                        }
                        sequencer.execute(node);

                        // Save the changes made by the sequencer ...
                        session.save();
                    }
                    this.statistics.recordNodeSequenced();
                }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            String msg = "Error while sequencing {}";
            this.logger.error(e, msg, changedNode);
        } catch (Exception e) {
            String msg = "Error while finding sequencers to run against {}";
            this.logger.error(e, msg, changedNode);
        }
    }

    /**
     * An enqueued notification that a node has changed.
     * @author Randall Hauch
     */
    @Immutable
    public class ChangedNode implements Runnable {

        private final String repositoryWorkspaceName;
        private final String absolutePath;
        private final int hc;

        protected ChangedNode( String repositoryWorkspaceName, String absolutePath ) {
            assert repositoryWorkspaceName != null;
            assert absolutePath != null;
            this.repositoryWorkspaceName = repositoryWorkspaceName;
            this.absolutePath = absolutePath.trim();
            this.hc = HashCodeUtil.computeHash(this.repositoryWorkspaceName, this.absolutePath);
        }

        /**
         * @return absolutePath
         */
        public String getAbsolutePath() {
            return this.absolutePath;
        }

        /**
         * @return repositoryWorkspaceName
         */
        public String getRepositoryWorkspaceName() {
            return this.repositoryWorkspaceName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ChangedNode) {
                ChangedNode that = (ChangedNode)obj;
                if (this.hc != that.hc) return false;
                if (!this.repositoryWorkspaceName.equals(that.repositoryWorkspaceName)) return false;
                if (!this.absolutePath.equals(that.absolutePath)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            processChangedNode(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return this.repositoryWorkspaceName + "=>" + this.absolutePath;
        }
    }

    // /**
    // * An event that has been observed that may be interesting for sequencing.
    // * @author Randall Hauch
    // */
    // @Immutable
    // public static class SequencingEvent implements Event {
    //
    // private final Event wrappedEvent;
    // private final String repositoryWorkspaceName;
    // private final String absolutePath;
    //
    // protected SequencingEvent( Event wrappedEvent, String repositoryWorkspaceName, String absolutePath ) {
    // assert wrappedEvent != null;
    // assert repositoryWorkspaceName != null;
    // assert absolutePath != null;
    // this.wrappedEvent = wrappedEvent;
    // this.repositoryWorkspaceName = repositoryWorkspaceName;
    // this.absolutePath = absolutePath.trim();
    // }
    //        
    // public
    //        
    // /**
    // * @return absolutePath
    // */
    // public String getAbsolutePath() {
    // return this.absolutePath;
    // }
    //
    // /**
    // * {@inheritDoc}
    // */
    // public String getPath() throws RepositoryException {
    // return this.wrappedEvent.getPath();
    // }
    //
    // /**
    // * {@inheritDoc}
    // */
    // public int getType() {
    // return this.wrappedEvent.getType();
    // }
    //
    // public String getTypeString() {
    // final int type = this.wrappedEvent.getType();
    // switch (type) {
    // case Event.NODE_ADDED:
    // return "NODE_ADDED";
    // case Event.NODE_REMOVED:
    // return "NODE_REMOVED";
    // case Event.PROPERTY_ADDED:
    // return "PROPERTY_ADDED";
    // case Event.PROPERTY_CHANGED:
    // return "PROPERTY_CHANGED";
    // case Event.PROPERTY_REMOVED:
    // return "PROPERTY_REMOVED";
    // }
    // return "unknown event type " + type;
    // }
    //
    // /**
    // * {@inheritDoc}
    // */
    // public String getUserID() {
    // return this.wrappedEvent.getUserID();
    // }
    // }
    //
    /**
     * Implementation of the {@link EventListener JCR EventListener} interface, returned by the sequencing system.
     * @author Randall Hauch
     */
    @ThreadSafe
    public class WorkspaceListener implements EventListener {

        public static final boolean DEFAULT_IS_DEEP = true;
        public static final boolean DEFAULT_NO_LOCAL = false;
        public static final int DEFAULT_EVENT_TYPES = Event.NODE_ADDED | /* Event.NODE_REMOVED | */Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED /*
         * |
         * Event.PROPERTY_REMOVED
         */;
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

        /**
         * @return sequencingSystem
         */
        public SequencingSystem getSequencingSystem() {
            return SequencingSystem.this;
        }

        public synchronized boolean isRegistered() {
            return this.session != null;
        }

        public synchronized WorkspaceListener register() throws UnsupportedRepositoryOperationException, RepositoryException {
            if (this.isRegistered()) return this;
            this.session = SequencingSystem.this.getSessionFactory().createSession(this.repositoryWorkspaceName);
            String[] uuids = this.uuids.isEmpty() ? null : this.uuids.toArray(new String[this.uuids.size()]);
            String[] nodeTypeNames = this.nodeTypeNames.isEmpty() ? null : this.nodeTypeNames.toArray(new String[this.nodeTypeNames.size()]);
            this.session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, absolutePath, deep, uuids, nodeTypeNames, noLocal);
            return this;
        }

        public synchronized WorkspaceListener unregister() throws UnsupportedRepositoryOperationException, RepositoryException {
            if (!this.isRegistered()) return this;
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
                if (SequencingSystem.this.isShutdown()) {
                    // This sequencing system has been shutdown, so unregister this listener
                    try {
                        unregister();
                    } catch (RepositoryException re) {
                        String msg = "Error unregistering workspace listener after sequencing system has been shutdow.";
                        Logger.getLogger(this.getClass()).debug(re, msg);
                    }
                } else {
                    SequencingSystem.this.enqueueEvents(events, this);
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
        private long numberOfEventsSkipped;
        @GuardedBy( "lock" )
        private long numberOfEventSetsIgnored;
        @GuardedBy( "lock" )
        private long numberOfEventSetsEnqueued;
        @GuardedBy( "lock" )
        private long numberOfNodesSequenced;
        @GuardedBy( "lock" )
        private long numberOfNodesSkipped;
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
                this.numberOfEventsSkipped = 0;
                this.numberOfEventSetsIgnored = 0;
                this.numberOfEventSetsEnqueued = 0;
                this.numberOfNodesSequenced = 0;
                this.numberOfNodesSkipped = 0;
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
         * @return the number of nodes that were sequenced
         */
        public long getNumberOfNodesSequenced() {
            try {
                lock.readLock().lock();
                return this.numberOfNodesSequenced;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * @return the number of nodes that were skipped because no sequencers applied
         */
        public long getNumberOfNodesSkipped() {
            try {
                lock.readLock().lock();
                return this.numberOfNodesSkipped;
            } finally {
                lock.readLock().unlock();
            }
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
         * @return the number of events that were skipped (not enqueued) because they were not
         * {@link SequencingSystem#getEventFilter() interesting}
         */
        public long getNumberOfEventsSkipped() {
            try {
                lock.readLock().lock();
                return this.numberOfEventsSkipped;
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

        protected void recordNodeSequenced() {
            try {
                lock.writeLock().lock();
                ++this.numberOfNodesSequenced;
            } finally {
                lock.writeLock().unlock();
            }
        }

        protected void recordNodeSkipped() {
            try {
                lock.writeLock().lock();
                ++this.numberOfNodesSkipped;
            } finally {
                lock.writeLock().unlock();
            }
        }

        protected void recordEvents( long enqueued, long skipped ) {
            try {
                lock.writeLock().lock();
                this.numberOfEventsEnqueued += enqueued;
                this.numberOfEventsSkipped += enqueued;
                ++this.numberOfEventSetsEnqueued;
            } finally {
                lock.writeLock().unlock();
            }
        }

        protected void recordEventsIgnored( long count ) {
            try {
                lock.writeLock().lock();
                this.numberOfEventsIgnored += count;
                this.numberOfEventSetsIgnored += 1;
                ++this.numberOfEventSetsEnqueued;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
