/*
 *
 */
package org.jboss.dna.services.sequencers;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import org.jboss.dna.common.util.Logger;

/**
 * A sequencing system is used to monitor changes in the content of {@link Repository JCR repositories} and to sequence the
 * content to extract or to generate structured information.
 * @author Randall Hauch
 */
public class SequencingSystem {

    /**
     * Interface that is used by the {@link SequencingSystem#setEventReviewer(SequencingSystem.EventReviewer) SequencingSystem} to
     * review observed events
     * @author Randall Hauch
     */
    public static interface EventReviewer {

        /**
         * Determine whether the supplied event is considered interesting and signals that the node should be sequenced.
         * @param event the event being considered; never null
         * @return true if the event is interesting and should be sequenced
         */
        public boolean isEventInteresting( SequencingEvent event );
    }

    /**
     * The default {@link EventReviewer} implementation that considers every event to be interesting.
     * @author Randall Hauch
     */
    protected static class DefaultEventReviewer implements EventReviewer {

        public boolean isEventInteresting( SequencingEvent event ) {
            return true;
        }
    }

    /**
     * Interface to which problems with particular events are logged.
     * @author Randall Hauch
     */
    public static interface ProblemLog {

        public void error( Event event, Throwable t );
    }

    protected class DefaultProblemLog implements ProblemLog {

        /**
         * {@inheritDoc}
         */
        public void error( Event event, Throwable t ) {
            String type = (event instanceof SequencingEvent) ? ((SequencingEvent)event).getTypeString() : Integer.toString(event.getType());
            String path = null;
            try {
                path = event.getPath();
            } catch (RepositoryException err) {
                path = err.getMessage();
            }
            getLogger().error(t, "Error processing {} event on node '{}'", type, path);
        }
    }

    /**
     * The default {@link EventReviewer} that considers every event to be interesting.
     * @see SequencingSystem#setEventReviewer(SequencingSystem.EventReviewer)
     */
    public static final EventReviewer DEFAULT_EVENT_REVIEWER = new DefaultEventReviewer();

    public static enum State {
        STARTED,
        PAUSED,
        SHUTDOWN;
    }

    private State state = State.PAUSED;
    private EventReviewer reviewer = DEFAULT_EVENT_REVIEWER;
    private ClassLoader parentClassLoader;
    private ISequencerProvider sequencerProvider;
    private ExecutorService executorService;
    private Logger logger = Logger.getLogger(this.getClass());
    private ProblemLog problemLog = new DefaultProblemLog();

    /**
     * Create a new sequencing system, configured with no sequencers and not {@link #monitor(Workspace) monitoring} any
     * workspaces. Upon construction, the system is {@link #isPaused() paused} and must be configured and then
     * {@link #start() started}.
     */
    public SequencingSystem() {
        this.setParentClassLoader(null);
    }

    /**
     * Get the parent class loader that's used when obtaining the class loader for each sequencer.
     * @return the parent class loader; never null
     */
    public ClassLoader getParentClassLoader() {
        return this.parentClassLoader;
    }

    /**
     * Set the parent class loader that's used when obtaining the class loader for each sequencer.
     * @param parentClassLoader the parent class loader, or null if the class loader should be obtained from the
     * {@link Thread#getContextClassLoader() current thread's context class loader} or, if that's null, this class'
     * {@link Class#getClassLoader() class loader}.
     */
    public void setParentClassLoader( ClassLoader parentClassLoader ) {
        if (this.isPaused()) {
            throw new IllegalStateException("Unable to set the class loader when the sequencing system is running");
        }
        if (parentClassLoader == null) parentClassLoader = Thread.currentThread().getContextClassLoader();
        if (parentClassLoader == null) parentClassLoader = this.getClass().getClassLoader();
        assert parentClassLoader != null;
        this.parentClassLoader = parentClassLoader;
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
     * Get the executor service used to run the sequencers.
     * @return the executor service
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * @param executorService the executor service
     */
    public void setExecutorService( ExecutorService executorService ) {
        if (this.isPaused()) {
            throw new IllegalStateException("Unable to set the executor service when the sequencing system is running");
        }
        this.executorService = executorService;
    }

    /**
     * @return the provider of sequencers
     */
    public ISequencerProvider getSequencerSelectors() {
        return this.sequencerProvider;
    }

    /**
     * @param sequencerProvider the provider of sequencers
     */
    public void setSequencerSelectors( ISequencerProvider sequencerProvider ) {
        if (this.isPaused()) {
            throw new IllegalStateException("Unable to set the sequencer selector when the sequencing system is running");
        }
        this.sequencerProvider = sequencerProvider;
    }

    /**
     * @return state
     */
    public State getState() {
        return this.state;
    }

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

    public SequencingSystem setState( String state ) {
        State newState = State.valueOf(state);
        setState(newState);
        return this;
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
    public SequencingSystem start() {
        if (this.state == State.SHUTDOWN) {
            String msg = "The sequencing system cannot be restarted after being shutdown";
            throw new IllegalStateException(msg);
        }
        if (this.executorService == null) {
            throw new IllegalStateException("The sequencing system cannot be started without an executor service");
        }
        if (this.reviewer == null) {
            throw new IllegalStateException("The sequencing system cannot be started without a reviewer");
        }
        if (this.sequencerProvider == null) {
            throw new IllegalStateException("The sequencing system cannot be started without a sequencer provider");
        }
        this.state = State.STARTED;
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
    public SequencingSystem pause() {
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
    public SequencingSystem shutdown() {
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
     * @return reviewer
     */
    public EventReviewer getEventReviewer() {
        return this.reviewer;
    }

    /**
     * @param reviewer Sets reviewer to the specified value.
     */
    public void setEventReviewer( EventReviewer reviewer ) {
        this.reviewer = reviewer != null ? reviewer : DEFAULT_EVENT_REVIEWER;
    }

    /**
     * Monitor the supplied workspace for all events on any node. Monitoring is accomplished by registering a listener on the
     * workspace, so this monitoring only has access to the information that the {@link Session#getUserID() workspace user} has
     * permission and authorization to see. For more information, see {@link ObservationManager}.
     * <p>
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected.
     * </p>
     * @param workspace the workspace
     * @return the listener that was created and registered to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( Workspace workspace ) throws RepositoryException {
        if (workspace == null) throw new IllegalArgumentException("The workspace parameter may not be null");
        WorkspaceListener listener = new WorkspaceListener(workspace);
        listener.register();
        return listener;
    }

    /**
     * Monitor the supplied workspace for events of the given type on any node at or under the supplied path. Monitoring is
     * accomplished by registering a listener on the workspace, so this monitoring only has access to the information that the
     * {@link Session#getUserID() workspace user} has permission and authorization to see. For more information, see
     * {@link ObservationManager}.
     * <p>
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected.
     * </p>
     * @param workspace the workspace
     * @param eventTypes the bitmask of the {@link Event} types that are to be monitored
     * @param absolutePath the absolute path of the node at or below which changes are to be monitored; may be null if all nodes
     * in the workspace are to be monitored
     * @return the listener that was created and registered to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( Workspace workspace, int eventTypes, String absolutePath ) throws RepositoryException {
        if (workspace == null) throw new IllegalArgumentException("The workspace parameter may not be null");
        WorkspaceListener listener = new WorkspaceListener(workspace);
        listener.setEventTypes(eventTypes);
        listener.setAbsolutePath(absolutePath);
        listener.register();
        return listener;
    }

    /**
     * Monitor the supplied workspace for events of the given type on any node at or under the supplied path. Monitoring is
     * accomplished by registering a listener on the workspace, so this monitoring only has access to the information that the
     * {@link Session#getUserID() workspace user} has permission and authorization to see. For more information, see
     * {@link ObservationManager}.
     * <p>
     * The listener returned from this method is not managed by this SequencingSystem instance. If the listener is no longer
     * needed, it simply must be {@link ObservationManager#removeEventListener(EventListener) removed} as a listener of the
     * workspace and garbage collected.
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
     * @param workspace the workspace
     * @param eventTypes the bitmask of the {@link Event} types that are to be monitored
     * @param absolutePath the absolute path of the node at or below which changes are to be monitored; may be null if all nodes
     * in the workspace are to be monitored
     * @param isDeep true if events below the node given by the <code>absolutePath</code> or by the <code>uuids</code> are to
     * be processed, or false if only the events at the node
     * @param uuids array of UUIDs of nodes that are to be monitored; may be null or empty if the UUIDs are not known
     * @param nodeTypeNames array of node type names that are to be monitored; may be null or empty if the monitoring has no node
     * type restrictions
     * @param noLocal true if the events originating in the supplied workspace are to be ignored, or false if they are also to be
     * processed.
     * @return the listener that was created and registered with the workspace to perform the monitoring
     * @throws RepositoryException if there is a problem registering the listener
     */
    public WorkspaceListener monitor( Workspace workspace, int eventTypes, String absolutePath, boolean isDeep, String[] uuids, String[] nodeTypeNames, boolean noLocal ) throws RepositoryException {
        if (workspace == null) throw new IllegalArgumentException("The workspace parameter may not be null");
        WorkspaceListener listener = new WorkspaceListener(workspace);
        listener.setEventTypes(eventTypes);
        listener.setAbsolutePath(absolutePath);
        listener.setDeep(isDeep);
        listener.setUuids(uuids);
        listener.setNodeTypeNames(nodeTypeNames);
        listener.setNoLocal(noLocal);
        listener.register();
        return listener;
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
        if (eventIterator == null || this.state != State.STARTED) return;
        if (this.state == State.SHUTDOWN) {
            throw new IllegalStateException("The sequencing system has been shutdown and cannot be used");
        }
        if (this.executorService == null) {
            throw new IllegalStateException("The sequencing system does not have an executor service");
        }
        Workspace workspace = listener.getWorkspace();
        // Accumulate the list of changed nodes. Any event which has a path to a node that has already been seen
        // (according to the ChangedNode.hashCode() and ChangedNode.equals() methods) will not be added to this set.
        Set<ChangedNode> nodesToProcess = new LinkedHashSet<ChangedNode>();
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            SequencingEvent seqEvent = new SequencingEvent(event, workspace);
            try {
                if (this.reviewer.isEventInteresting(seqEvent)) {
                    nodesToProcess.add(new ChangedNode(seqEvent.getNode(), seqEvent.getPath(), workspace));
                }
            } catch (Throwable t) {
                this.problemLog.error(seqEvent, t);
            }
        }

        for (ChangedNode changedNode : nodesToProcess) {
            this.executorService.execute(changedNode);
        }
    }

    /**
     * Do the work of processing by sequencing the node. This method is called by the {@link #executorService executor service}
     * when it performs it's work on the enqueued {@link ChangedNode ChangedNode runnable objects}.
     * @param node the node to be processed.
     */
    protected void processNode( Node node ) {
        try {
            // Figure out which sequencers should run ...
            List<ISequencer> sequencers = this.sequencerProvider.getSequencersToProcess(node, this.logger);
            // Run each of those sequencers ...
            if (this.logger.isDebugEnabled()) {
                if (sequencers.isEmpty()) {
                    this.logger.debug("Skipping '{}': no sequencers matched this condition", node.getPath());
                }
            }
            for (ISequencer sequencer : sequencers) {
                if (this.logger.isDebugEnabled()) {
                    String sequencerName = sequencer.getClass().getName();
                    ISequencerConfig config = this.sequencerProvider.getConfigurationFor(sequencer);
                    if (config != null) {
                        sequencerName = config.getName();
                    }
                    this.logger.debug("Sequencing '{}' with ", node.getPath(), sequencerName);
                }
                sequencer.execute(node);
            }
        } catch (RepositoryException e) {
            String msg = "Error while sequencing node '{}'";
            String path = "error";
            try {
                path = node.getPath();
            } catch (RepositoryException re) {
            }
            this.logger.error(e, msg, path);
        } catch (Exception e) {
            String msg = "Error while finding sequencers to run against node '{}'";
            String path = "error";
            try {
                path = node.getPath();
            } catch (RepositoryException re) {
            }
            this.logger.error(e, msg, path);
        }
    }

    public class ChangedNode implements Runnable {

        private final Workspace workspace;
        private final Node node;
        private final String path;

        protected ChangedNode( Node node, String path, Workspace workspace ) {
            this.node = node;
            this.path = path;
            this.workspace = workspace;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ChangedNode) {
                ChangedNode that = (ChangedNode)obj;
                if (this.workspace.equals(that.workspace) && this.path.equals(that.path)) return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            processNode(this.node);
        }
    }

    /**
     * An event that has been observed that may be interesting for sequencing.
     * @author Randall Hauch
     */
    public static class SequencingEvent implements Event {

        private final Event wrappedEvent;
        private final Workspace workspace;
        private Node node;

        protected SequencingEvent( Event wrappedEvent, Workspace workspace ) {
            this.wrappedEvent = wrappedEvent;
            this.workspace = workspace;
        }

        /**
         * {@inheritDoc}
         */
        public String getPath() throws RepositoryException {
            return this.wrappedEvent.getPath();
        }

        /**
         * {@inheritDoc}
         */
        public int getType() {
            return this.wrappedEvent.getType();
        }

        public String getTypeString() {
            final int type = this.wrappedEvent.getType();
            switch (type) {
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
            return "unknown event type " + type;
        }

        /**
         * {@inheritDoc}
         */
        public String getUserID() {
            return this.wrappedEvent.getUserID();
        }

        /**
         * @return workspace
         */
        public Workspace getWorkspace() {
            return this.workspace;
        }

        /**
         * Get the node at this event's {@link #getPath() path}.
         * @return the node
         * @throws PathNotFoundException if the node cannot be found
         * @throws RepositoryException if there is a problem accessing the repository
         */
        public Node getNode() throws PathNotFoundException, RepositoryException {
            if (this.node == null) {
                this.node = this.workspace.getSession().getRootNode().getNode(this.getPath());
            }
            return this.node;
        }

    }

    /**
     * Implementation of the {@link EventListener JCR EventListener} interface, returned by the sequencing system.
     * @author Randall Hauch
     */
    public class WorkspaceListener implements EventListener {

        public static final boolean DEFAULT_IS_DEEP = true;
        public static final boolean DEFAULT_NO_LOCAL = false;
        public static final int DEFAULT_EVENT_TYPES = Event.NODE_ADDED & Event.NODE_REMOVED & Event.PROPERTY_ADDED & Event.PROPERTY_CHANGED & Event.PROPERTY_REMOVED;
        public static final String DEFAULT_ABSOLUTE_PATH = "/";

        private final Workspace workspace;
        private Set<String> uuids = new HashSet<String>();
        private Set<String> nodeTypeNames = new HashSet<String>();
        private int eventTypes = DEFAULT_EVENT_TYPES;
        private String absolutePath = DEFAULT_ABSOLUTE_PATH;
        private boolean deep = DEFAULT_IS_DEEP;
        private boolean noLocal = DEFAULT_NO_LOCAL;

        protected WorkspaceListener( Workspace workspace ) {
            this.workspace = workspace;
        }

        /**
         * @return eventTypes
         */
        public int getEventTypes() {
            return this.eventTypes;
        }

        /**
         * Set the event types for this listener. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param eventTypes Sets eventTypes to the specified value.
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setEventTypes( int eventTypes ) {
            this.eventTypes = eventTypes;
            return this;
        }

        /**
         * @return absolutePath
         */
        public String getAbsolutePath() {
            return this.absolutePath;
        }

        /**
         * Set the absolute path for this listener. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param absolutePath Sets absolutePath to the specified value.
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setAbsolutePath( String absolutePath ) {
            this.absolutePath = absolutePath;
            return this;
        }

        /**
         * @return deep
         */
        public boolean isDeep() {
            return this.deep;
        }

        /**
         * Set whether this listener monitors deep notifications. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param deep Sets deep to the specified value.
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setDeep( boolean deep ) {
            this.deep = deep;
            return this;
        }

        /**
         * @return noLocal
         */
        public boolean isNoLocal() {
            return this.noLocal;
        }

        /**
         * Set whether local events are monitored. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param noLocal Sets noLocal to the specified value.
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setNoLocal( boolean noLocal ) {
            this.noLocal = noLocal;
            return this;
        }

        /**
         * @return uuids
         */
        public Set<String> getUuids() {
            return this.uuids;
        }

        /**
         * Set the UUIDs of the nodes that are to be monitored. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param uuids array of UUIDs of nodes that are to be monitored; may be null or empty if the UUIDs are not known
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setUuids( String... uuids ) {
            Set<String> newUuids = new HashSet<String>();
            if (uuids != null) {
                for (String uuid : uuids) {
                    if (uuid != null) newUuids.add(uuid);
                }
            }
            this.uuids = Collections.unmodifiableSet(newUuids);
            return this;
        }

        /**
         * @return nodeTypeNames
         */
        public Set<String> getNodeTypeNames() {
            return this.nodeTypeNames;
        }

        /**
         * Set the node types of the nodes that are to be monitored. The changes only take effect after the listener is
         * {@link #reregister() reregistered}.
         * @param nodeTypeNames array of node type names that are to be monitored; may be null or empty if the monitoring has no
         * node type restrictions
         * @return this object for method chaining purposes
         */
        public WorkspaceListener setNodeTypeNames( String... nodeTypeNames ) {
            Set<String> newNodeTypeNames = new HashSet<String>();
            if (nodeTypeNames != null) {
                for (String nodeTypeName : nodeTypeNames) {
                    if (nodeTypeName != null) newNodeTypeNames.add(nodeTypeName);
                }
            }
            this.nodeTypeNames = Collections.unmodifiableSet(newNodeTypeNames);
            return this;
        }

        /**
         * @return workspace
         */
        public Workspace getWorkspace() {
            return this.workspace;
        }

        /**
         * @return sequencingSystem
         */
        public SequencingSystem getSequencingSystem() {
            return SequencingSystem.this;
        }

        public boolean isWorkspaceUsable() {
            try {
                return this.workspace.getSession().getRootNode() != null;
            } catch (RepositoryException err) {
                return false;
            }
        }

        public WorkspaceListener register() throws UnsupportedRepositoryOperationException, RepositoryException {
            this.workspace.getObservationManager().addEventListener(this, eventTypes, absolutePath, deep, uuids.toArray(new String[uuids.size()]),
                                                                    nodeTypeNames.toArray(new String[nodeTypeNames.size()]), noLocal);
            return this;
        }

        public WorkspaceListener unregister() throws UnsupportedRepositoryOperationException, RepositoryException {
            this.workspace.getObservationManager().removeEventListener(this);
            return this;
        }

        public WorkspaceListener reregister() throws UnsupportedRepositoryOperationException, RepositoryException {
            unregister();
            register();
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void onEvent( EventIterator events ) {
            if (events != null) {
                try {
                    SequencingSystem.this.enqueueEvents(events, this);
                } catch (IllegalStateException e) {
                    if (SequencingSystem.this.isShutdown()) {
                        // This sequencing system has been shutdown and can't be restarted, so unregister this listener
                        try {
                            unregister();
                        } catch (RepositoryException re) {
                            String msg = "Error unregistering workspace listener after sequencing system has been shutdow.";
                            Logger.getLogger(this.getClass()).debug(re, msg);
                        }
                    }
                }
            }
        }
    }
}
