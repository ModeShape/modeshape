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

import static org.modeshape.graph.JcrLexicon.MIXIN_TYPES;
import static org.modeshape.graph.JcrLexicon.PRIMARY_TYPE;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.NetChangeObserver;
import org.modeshape.graph.observe.Observable;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.PropertyChangeRequest;

/**
 * The implementation of JCR {@link ObservationManager}.
 */
final class JcrObservationManager implements ObservationManager {

    /**
     * The key for storing the {@link JcrObservationManager#setUserData(String) observation user data} in the
     * {@link ExecutionContext}'s {@link ExecutionContext#getData() data}.
     */
    static final String OBSERVATION_USER_DATA_KEY = "org.modeshape.jcr.observation.userdata";

    static final String MOVE_FROM_KEY = "srcAbsPath";
    static final String MOVE_TO_KEY = "destAbsPath";
    static final String ORDER_CHILD_KEY = "srcChildRelPath";
    static final String ORDER_BEFORE_KEY = "destChildRelPath";

    /**
     * The repository observable the JCR listeners will be registered with.
     */
    private final Observable repositoryObservable;

    /**
     * The map of the JCR repository listeners and their associated wrapped class.
     */
    private final Map<EventListener, JcrListenerAdapter> listeners;

    /**
     * The session's namespace registry used when handling events.
     */
    private final NamespaceRegistry namespaceRegistry;

    /**
     * The associated session.
     */
    private final JcrSession session;

    /**
     * The session's value factories.
     */
    private final ValueFactories valueFactories;

    private final ValueFactory<String> stringFactory;

    /**
     * The name of the session's workspace; cached for performance reasons.
     */
    private final String workspaceName;

    /**
     * The name of the workspace used for this session's system content; cached for performance reasons.
     */
    private final String systemWorkspaceName;

    /**
     * The name of the repository source used for this session's system content; cached for performance reasons.
     */
    private final String systemSourceName;

    /**
     * @param session the owning session (never <code>null</code>)
     * @param repositoryObservable the repository observable used to register JCR listeners (never <code>null</code>)
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     */
    JcrObservationManager( JcrSession session,
                           Observable repositoryObservable ) {
        CheckArg.isNotNull(session, "session");
        CheckArg.isNotNull(repositoryObservable, "repositoryObservable");

        this.session = session;
        this.repositoryObservable = repositoryObservable;
        this.listeners = new ConcurrentHashMap<EventListener, JcrListenerAdapter>();
        this.namespaceRegistry = this.session.getExecutionContext().getNamespaceRegistry();
        this.valueFactories = this.session.getExecutionContext().getValueFactories();
        this.stringFactory = this.valueFactories.getStringFactory();
        this.workspaceName = this.session.getWorkspace().getName();
        this.systemWorkspaceName = this.session.repository().getSystemWorkspaceName();
        this.systemSourceName = this.session.repository().getSystemSourceName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.observation.ObservationManager#addEventListener(javax.jcr.observation.EventListener, int, java.lang.String,
     *      boolean, java.lang.String[], java.lang.String[], boolean)
     * @throws RepositoryException if the session is no longer live
     * @throws IllegalArgumentException if <code>listener</code> is <code>null</code>
     */
    public synchronized void addEventListener( EventListener listener,
                                               int eventTypes,
                                               String absPath,
                                               boolean isDeep,
                                               String[] uuid,
                                               String[] nodeTypeName,
                                               boolean noLocal ) throws RepositoryException {
        CheckArg.isNotNull(listener, "listener");
        checkSession(); // make sure session is still active

        // create wrapper and register
        JcrListenerAdapter adapter = new JcrListenerAdapter(listener, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
        // unregister if already registered
        this.repositoryObservable.unregister(adapter);
        this.repositoryObservable.register(adapter);
        this.listeners.put(listener, adapter);
    }

    /**
     * @throws RepositoryException if session is not active
     */
    void checkSession() throws RepositoryException {
        session.checkLive();
    }

    /**
     * @return the namespace registry used by listeners when handling events
     */
    NamespaceRegistry namespaceRegistry() {
        return this.namespaceRegistry;
    }

    final String stringFor( Path path ) {
        return this.stringFactory.create(path);
    }

    final String stringFor( Path.Segment segment ) {
        return this.stringFactory.create(segment);
    }

    final String stringFor( Name name ) {
        return this.stringFactory.create(name);
    }

    /**
     * @return the node type manager
     * @throws RepositoryException if there is a problem
     */
    JcrNodeTypeManager getNodeTypeManager() throws RepositoryException {
        return (JcrNodeTypeManager)this.session.getWorkspace().getNodeTypeManager();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.observation.ObservationManager#getRegisteredEventListeners()
     */
    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        checkSession(); // make sure session is still active
        return new JcrEventListenerIterator(this.listeners.keySet());
    }

    /**
     * @return the user ID used by the listeners when handling events
     */
    String getUserId() {
        return this.session.getUserID();
    }

    /**
     * @return the value factories used by listeners when handling events
     */
    ValueFactories getValueFactories() {
        return this.valueFactories;
    }

    /**
     * @return the workspace graph
     */
    Graph getGraph() {
        return ((JcrWorkspace)this.session.getWorkspace()).graph();
    }

    /**
     * @return the session's unique identifier
     */
    String getSessionId() {
        return this.session.sessionId();
    }

    /**
     * @return workspaceName
     */
    final String getWorkspaceName() {
        return workspaceName;
    }

    final String getSystemWorkspaceName() {
        return systemWorkspaceName;
    }

    final String getSystemSourceName() {
        return systemSourceName;
    }

    /**
     * Remove all of the listeners. This is typically called when the {@link JcrSession#logout() session logs out}.
     */
    synchronized void removeAllEventListeners() {
        for (JcrListenerAdapter listener : this.listeners.values()) {
            assert (listener != null);
            this.repositoryObservable.unregister(listener);
        }

        this.listeners.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.observation.ObservationManager#removeEventListener(javax.jcr.observation.EventListener)
     * @throws IllegalArgumentException if <code>listener</code> is <code>null</code>
     */
    public synchronized void removeEventListener( EventListener listener ) throws RepositoryException {
        checkSession(); // make sure session is still active
        CheckArg.isNotNull(listener, "listener");

        JcrListenerAdapter jcrListener = this.listeners.remove(listener);

        if (jcrListener != null) {
            this.repositoryObservable.unregister(jcrListener);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method set's the user data on the {@link #session session's} {@link JcrSession#getExecutionContext() execution
     * context}, under the {@link #OBSERVATION_USER_DATA_KEY} key.
     * </p>
     * 
     * @see javax.jcr.observation.ObservationManager#setUserData(java.lang.String)
     */
    @Override
    public void setUserData( String userData ) {
        // User data value may be null
        session.setSessionData(OBSERVATION_USER_DATA_KEY, userData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since ModeShape does not support journaled observation, this method returns null.
     * </p>
     * 
     * @see javax.jcr.observation.ObservationManager#getEventJournal()
     */
    @Override
    public EventJournal getEventJournal() {
        return null; // per the JavaDoc
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since ModeShape does not support journaled observation, this method returns null.
     * </p>
     * 
     * @see javax.jcr.observation.ObservationManager#getEventJournal(int, java.lang.String, boolean, java.lang.String[],
     *      java.lang.String[])
     */
    @Override
    public EventJournal getEventJournal( int eventTypes,
                                         String absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         String[] nodeTypeName ) {
        return null;
    }

    /**
     * An implementation of JCR {@link RangeIterator} extended by the event and event listener iterators.
     * 
     * @param <E> the type being iterated over
     */
    class JcrRangeIterator<E> implements RangeIterator {

        /**
         * The elements being iterated over.
         */
        private final List<? extends E> elements;

        /**
         * The current position in the iterator.
         */
        private int position = 0;

        /**
         * @param elements the elements to iterator over
         * @throws IllegalArgumentException if <code>elements</code> is <code>null</code>
         */
        public JcrRangeIterator( Collection<? extends E> elements ) {
            CheckArg.isNotNull(elements, "elements");
            this.elements = new ArrayList<E>(elements);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        public long getPosition() {
            return this.position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        public long getSize() {
            return this.elements.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return (getPosition() < getSize());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Object element = this.elements.get(this.position);
            ++this.position;

            return element;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         * @throws UnsupportedOperationException if called
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        public void skip( long skipNum ) {
            this.position += skipNum;

            if (!hasNext()) {
                throw new NoSuchElementException();
            }
        }
    }

    /**
     * An implementation of the JCR {@link EventListenerIterator}.
     */
    class JcrEventListenerIterator extends JcrRangeIterator<EventListener> implements EventListenerIterator {

        /**
         * @param listeners the listeners being iterated over
         * @throws IllegalArgumentException if <code>listeners</code> is <code>null</code>
         */
        public JcrEventListenerIterator( Collection<EventListener> listeners ) {
            super(listeners);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.EventListenerIterator#nextEventListener()
         */
        public EventListener nextEventListener() {
            return (EventListener)next();
        }
    }

    /**
     * An implementation of JCR {@link EventIterator}.
     */
    class JcrEventIterator extends JcrRangeIterator<Event> implements EventIterator {

        /**
         * @param events the events being iterated over
         * @throws IllegalArgumentException if <code>events</code> is <code>null</code>
         */
        public JcrEventIterator( Collection<Event> events ) {
            super(events);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.EventIterator#nextEvent()
         */
        public Event nextEvent() {
            return (Event)next();
        }
    }

    /**
     * The information related to and shared by a set of events that represent a single logical operation.
     */
    @Immutable
    class JcrEventBundle {

        /**
         * The date and time of the event bundle.
         */
        private final DateTime date;

        /**
         * The user ID.
         */
        private final String userId;

        private final String userData;

        public JcrEventBundle( DateTime dateTime,
                               String userId,
                               String userData ) {
            this.userId = userId;
            this.userData = userData;
            this.date = dateTime;
        }

        public String getUserID() {
            return this.userId;
        }

        /**
         * @return date
         */
        public DateTime getDate() {
            return date;
        }

        /**
         * @return userData
         */
        public String getUserData() {
            return userData;
        }
    }

    /**
     * An implementation of JCR {@link Event}.
     */
    @Immutable
    class JcrEvent implements Event {

        private final String id;

        /**
         * The node path.
         */
        private final String path;

        /**
         * The event type.
         */
        private final int type;

        /**
         * The immutable bundle information, which may be shared amongst multiple events.
         */
        private final JcrEventBundle bundle;

        /**
         * @param bundle the event bundle information
         * @param type the event type
         * @param path the node path
         * @param id the node identifier
         */
        public JcrEvent( JcrEventBundle bundle,
                         int type,
                         String path,
                         String id ) {
            this.type = type;
            this.path = path;
            this.bundle = bundle;
            this.id = id;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getPath()
         */
        public String getPath() {
            return this.path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getType()
         */
        public int getType() {
            return this.type;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getUserID()
         */
        public String getUserID() {
            return bundle.getUserID();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getDate()
         */
        @Override
        public long getDate() {
            return bundle.getDate().getMilliseconds();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getIdentifier()
         */
        @Override
        public String getIdentifier() {
            return id;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getUserData()
         */
        @Override
        public String getUserData() {
            return bundle.getUserData();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.observation.Event#getInfo()
         * @see JcrMoveEvent#getInfo()
         */
        @Override
        public Map<String, String> getInfo() {
            return Collections.emptyMap();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            switch (this.type) {
                case Event.NODE_ADDED:
                    sb.append("Node added");
                    break;
                case Event.NODE_REMOVED:
                    sb.append("Node removed");
                    break;
                case Event.PROPERTY_ADDED:
                    sb.append("Property added");
                    break;
                case Event.PROPERTY_CHANGED:
                    sb.append("Property changed");
                    break;
                case Event.PROPERTY_REMOVED:
                    sb.append("Property removed");
                    break;
                case Event.NODE_MOVED:
                    sb.append("Node moved");
                    break;
            }
            sb.append(" at ").append(path).append(" by ").append(getUserID());
            return sb.toString();
        }
    }

    /**
     * An implementation of JCR {@link Event}.
     */
    @Immutable
    class JcrMoveEvent extends JcrEvent {

        private final Map<String, String> info;

        /**
         * @param bundle the event bundle information
         * @param type the event type
         * @param path the node path
         * @param id the node identifier
         * @param info the immutable map containing the source and destination absolute paths for the move
         */
        public JcrMoveEvent( JcrEventBundle bundle,
                             int type,
                             String path,
                             String id,
                             Map<String, String> info ) {
            super(bundle, type, path, id);
            this.info = info;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrObservationManager.JcrEvent#getInfo()
         */
        @Override
        public Map<String, String> getInfo() {
            return this.info;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node moved");
            String from = info.containsKey(MOVE_FROM_KEY) ? info.get(MOVE_FROM_KEY) : info.get(ORDER_CHILD_KEY);
            String to = info.containsKey(MOVE_TO_KEY) ? info.get(MOVE_TO_KEY) : info.get(ORDER_BEFORE_KEY);
            sb.append(" from ").append(from).append(" to ").append(to).append(" by ").append(getUserID());
            return sb.toString();
        }
    }

    /**
     * The <code>JcrListener</code> class wraps JCR {@link EventListener} and is responsible for converting
     * {@link NetChangeObserver.NetChange graph events} into JCR {@link Event events}.
     */
    @NotThreadSafe
    class JcrListenerAdapter extends NetChangeObserver {

        /**
         * The node path whose events should be handled (or <code>null</code>) if all node paths should be handled.
         */
        private final String absPath;

        /**
         * The primary type and mixin types of the locations that have changes. Used only when the node type of the location needs
         * to be checked.
         */
        private Map<Location, Map<Name, Property>> propertiesByLocation;

        /**
         * The JCR event listener.
         */
        private final EventListener delegate;

        /**
         * The event types this listener is interested in handling.
         */
        private final int eventTypes;

        /**
         * A flag indicating if events of child nodes of the <code>absPath</code> should be processed.
         */
        private final boolean isDeep;

        /**
         * The node type names or <code>null</code>. If a node with one of these types is the source node of an event than this
         * listener wants to process that event. If <code>null</code> or empty than this listener wants to handle nodes of any
         * type.
         */
        private final String[] nodeTypeNames;

        /**
         * A flag indicating if events generated by the session that registered this listener should be ignored.
         */
        private final boolean noLocal;

        /**
         * The node UUIDs or <code>null</code>. If a node with one of these UUIDs is the source node of an event than this
         * listener wants to handle this event. If <code>null</code> or empty than this listener wants to handle nodes with any
         * UUID.
         */
        private final String[] uuids;

        /**
         * @param delegate the JCR listener
         * @param eventTypes a combination of one or more JCR event types
         * @param absPath the absolute path of a node or <code>null</code> if all node paths
         * @param isDeep indicates if paths below <code>absPath</code> should be considered
         * @param uuids UUIDs or <code>null</code>
         * @param nodeTypeNames node type names or <code>null</code>
         * @param noLocal indicates if events from this listener's session should be ignored
         */
        public JcrListenerAdapter( EventListener delegate,
                                   int eventTypes,
                                   String absPath,
                                   boolean isDeep,
                                   String[] uuids,
                                   String[] nodeTypeNames,
                                   boolean noLocal ) {
            assert (delegate != null);

            this.delegate = delegate;
            this.eventTypes = eventTypes;
            this.absPath = absPath;
            this.isDeep = isDeep;
            this.uuids = uuids;
            this.nodeTypeNames = nodeTypeNames;
            this.noLocal = noLocal;
        }

        /**
         * @param changes the changes being processed
         * @return <code>true</code> if event occurred in a different session or if events from same session should be processed
         */
        private boolean acceptBasedOnEventSource( Changes changes ) {
            if (this.noLocal) {
                // don't accept unless IDs are different
                return !getSessionId().equals(changes.getContextId());
            }
            return true;
        }

        /**
         * @param change the change being processed
         * @return <code>true</code> if all node types should be processed or if changed node type name matches a specified type
         */
        private boolean acceptBasedOnNodeTypeName( NetChange change ) {
            boolean accept = true;

            if (shouldCheckNodeType()) {
                ValueFactory<String> stringFactory = getValueFactories().getStringFactory();
                Path changePath = null;
                if (change.includes(ChangeType.NODE_ADDED, ChangeType.NODE_REMOVED)) {
                    changePath = change.getPath().getParent();
                } else {
                    changePath = change.getPath();
                }

                Location parentLocation = Location.create(changePath);
                Map<Name, Property> propMap = this.propertiesByLocation.get(parentLocation);
                assert (propMap != null);

                try {
                    String primaryTypeName = stringFactory.create(propMap.get(PRIMARY_TYPE).getFirstValue());
                    String[] mixinNames = null;

                    if (propMap.get(MIXIN_TYPES) != null) {
                        mixinNames = stringFactory.create(propMap.get(MIXIN_TYPES).getValuesAsArray());
                    }

                    return getNodeTypeManager().isDerivedFrom(this.nodeTypeNames, primaryTypeName, mixinNames);
                } catch (RepositoryException e) {
                    accept = false;
                    Logger.getLogger(getClass()).error(e,
                                                       JcrI18n.cannotPerformNodeTypeCheck,
                                                       propMap.get(PRIMARY_TYPE),
                                                       propMap.get(MIXIN_TYPES),
                                                       this.nodeTypeNames);
                }
            }

            return accept;
        }

        /**
         * @param change the change being processed
         * @return <code>true</code> if there is no absolute path or if change path matches or optionally is a deep match
         */
        private boolean acceptBasedOnPath( NetChange change ) {
            if ((this.absPath != null) && (this.absPath.length() != 0)) {
                Path matchPath = getValueFactories().getPathFactory().create(this.absPath);
                Path changePath = null;

                if (change.includes(ChangeType.NODE_ADDED, ChangeType.NODE_REMOVED)) {
                    changePath = change.getPath().getParent();
                } else {
                    changePath = change.getPath();
                }

                if (this.isDeep) {
                    return matchPath.isAtOrAbove(changePath);
                }

                return matchPath.equals(changePath);
            }

            return true;
        }

        /**
         * @param change the change being processed
         * @return <code>true</code> if there are no UUIDs to match or change UUID matches
         */
        private boolean acceptBasedOnUuid( NetChange change ) {
            boolean accept = true;

            if ((this.uuids != null) && (this.uuids.length != 0)) {
                UUID matchUuid = change.getLocation().getUuid();

                if (matchUuid != null) {
                    accept = false;
                    UuidFactory uuidFactory = getValueFactories().getUuidFactory();

                    for (String uuidText : this.uuids) {
                        if ((uuidText != null) && (uuidText.length() != 0)) {
                            try {
                                UUID testUuid = uuidFactory.create(uuidText);

                                if (matchUuid.equals(testUuid)) {
                                    accept = true;
                                    break;
                                }
                            } catch (ValueFormatException e) {
                                Logger.getLogger(getClass()).error(JcrI18n.cannotCreateUuid, uuidText);
                            }
                        }
                    }
                }
            }

            return accept;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if ((obj != null) && (obj instanceof JcrListenerAdapter)) {
                return (this.delegate == ((JcrListenerAdapter)obj).delegate);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.delegate.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.NetChangeObserver#notify(org.modeshape.graph.observe.Changes)
         */
        @Override
        public void notify( Changes changes ) {
            // This method should only be called when the events are coming from one of the repository sources
            // managing content for this JCR Repository instance.
            // check source first
            if (!acceptBasedOnEventSource(changes)) {
                return;
            }

            try {
                if (shouldCheckNodeType()) {
                    List<Location> changedLocations = new ArrayList<Location>();

                    // loop through changes saving the parent locations of the changed locations
                    Location root = null;
                    for (ChangeRequest request : changes.getChangeRequests()) {
                        String changedWorkspaceName = request.changedWorkspace();
                        // If this event is not from this session's workspace ...
                        if (!getWorkspaceName().equals(changedWorkspaceName)) {
                            // And not in the system workspace in the system source ...
                            if (!getSystemWorkspaceName().equals(changedWorkspaceName)
                                && !changes.getSourceName().equals(getSystemSourceName())) {
                                // We ignore it ...
                                continue;
                            }
                        }
                        Path changedPath = request.changedLocation().getPath();
                        if (!(request instanceof PropertyChangeRequest)) {
                            // We want the primary type and mixin types of the parent node ...
                            changedPath = changedPath.getParent();
                        }
                        Location location = Location.create(changedPath);
                        if (root == null && changedPath.isRoot()) root = location;
                        changedLocations.add(location);
                    }
                    if (!changedLocations.isEmpty()) {
                        // more efficient to get all of the locations at once then it is one at a time using the NetChange
                        Graph graph = getGraph();
                        this.propertiesByLocation = graph.getProperties(PRIMARY_TYPE, MIXIN_TYPES).on(changedLocations);
                        if (root != null && !propertiesByLocation.containsKey(root)) {
                            // The connector doesn't actually have any properties for the root node, so assume 'mode:root' ...
                            // For details, see MODE-959.
                            Property primaryType = graph.getContext().getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE,
                                                                                                  ModeShapeLexicon.ROOT);
                            Map<Name, Property> props = Collections.singletonMap(primaryType.getName(), primaryType);
                            this.propertiesByLocation.put(root, props);
                        }
                    }
                }

                // handle events
                super.notify(changes);
            } finally {
                this.propertiesByLocation = null;
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.NetChangeObserver#notify(org.modeshape.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( NetChanges netChanges ) {
            Collection<Event> events = new ArrayList<Event>();

            String userData = netChanges.getData().get(OBSERVATION_USER_DATA_KEY);
            JcrEventBundle bundle = new JcrEventBundle(netChanges.getTimestamp(), netChanges.getUserName(), userData);

            for (NetChange change : netChanges.getNetChanges()) {
                String changedWorkspaceName = change.getRepositoryWorkspaceName();
                // If this event is not from this session's workspace ...
                if (!getWorkspaceName().equals(changedWorkspaceName)) {
                    // And not in the system workspace in the system source ...
                    if (!getSystemWorkspaceName().equals(changedWorkspaceName)
                        && !netChanges.getSourceName().equals(getSystemSourceName())) {
                        // We ignore it ...
                        continue;
                    }
                }

                // ignore if lock/unlock
                if (change.includes(ChangeType.NODE_LOCKED) || change.includes(ChangeType.NODE_UNLOCKED)) {
                    continue;
                }

                // determine if need to process
                if (!acceptBasedOnNodeTypeName(change) || !acceptBasedOnPath(change) || !acceptBasedOnUuid(change)) {
                    continue;
                }

                // process event making sure we have the right event type
                Path path = change.getPath();
                PathFactory pathFactory = getValueFactories().getPathFactory();
                String id = change.getLocation().getUuid().toString();

                if (change.includes(ChangeType.NODE_MOVED)) {
                    Location original = change.getOriginalLocation();
                    Path originalPath = original.getPath();
                    if ((this.eventTypes & Event.NODE_MOVED) == Event.NODE_MOVED) {
                        Location before = change.getMovedBefore();
                        boolean sameParent = !originalPath.isRoot() && !path.isRoot()
                                             && originalPath.getParent().equals(path.getParent());
                        Map<String, String> info = new HashMap<String, String>();
                        if (sameParent && change.isReorder()) {
                            info.put(ORDER_CHILD_KEY, stringFor(originalPath.getLastSegment()));
                            info.put(ORDER_BEFORE_KEY, before != null ? stringFor(before.getPath().getLastSegment()) : null);
                        } else {
                            info.put(MOVE_FROM_KEY, stringFor(originalPath));
                            info.put(MOVE_TO_KEY, stringFor(path));
                        }
                        info = Collections.unmodifiableMap(info);
                        events.add(new JcrMoveEvent(bundle, Event.NODE_MOVED, stringFor(path), id, info));
                    }
                    // For some bizarre reason, JCR 2.0 expects these methods <i>in addition to</i> the NODE_MOVED event
                    if ((this.eventTypes & Event.NODE_ADDED) == Event.NODE_ADDED) {
                        events.add(new JcrEvent(bundle, Event.NODE_ADDED, stringFor(path), id));
                    }
                    if ((this.eventTypes & Event.NODE_REMOVED) == Event.NODE_REMOVED) {
                        events.add(new JcrEvent(bundle, Event.NODE_REMOVED, stringFor(originalPath), id));
                    }
                }
                if (change.includes(ChangeType.NODE_ADDED) && ((this.eventTypes & Event.NODE_ADDED) == Event.NODE_ADDED)) {
                    // create event for added node
                    events.add(new JcrEvent(bundle, Event.NODE_ADDED, stringFor(path), id));
                } else if (change.includes(ChangeType.NODE_REMOVED)
                           && ((this.eventTypes & Event.NODE_REMOVED) == Event.NODE_REMOVED)) {
                    // create event for removed node
                    events.add(new JcrEvent(bundle, Event.NODE_REMOVED, stringFor(path), id));
                }

                if (change.includes(ChangeType.PROPERTY_CHANGED)
                    && ((this.eventTypes & Event.PROPERTY_CHANGED) == Event.PROPERTY_CHANGED)) {
                    for (Property property : change.getModifiedProperties()) {
                        // create event for changed property
                        Path propertyPath = pathFactory.create(path, stringFor(property.getName()));
                        events.add(new JcrEvent(bundle, Event.PROPERTY_CHANGED, stringFor(propertyPath), id));
                    }
                }

                // properties have changed
                if (change.includes(ChangeType.PROPERTY_ADDED)
                    && ((this.eventTypes & Event.PROPERTY_ADDED) == Event.PROPERTY_ADDED)) {
                    for (Property property : change.getAddedProperties()) {
                        // create event for added property
                        Path propertyPath = pathFactory.create(path, stringFor(property.getName()));
                        events.add(new JcrEvent(bundle, Event.PROPERTY_ADDED, stringFor(propertyPath), id));
                    }
                }

                if (change.includes(ChangeType.PROPERTY_REMOVED)
                    && ((this.eventTypes & Event.PROPERTY_REMOVED) == Event.PROPERTY_REMOVED)) {
                    for (Name name : change.getRemovedProperties()) {
                        // create event for removed property
                        Path propertyPath = pathFactory.create(path, name);
                        events.add(new JcrEvent(bundle, Event.PROPERTY_REMOVED, stringFor(propertyPath), id));
                    }
                }
            }

            // notify delegate
            if (!events.isEmpty()) {
                this.delegate.onEvent(new JcrEventIterator(events));
            }
        }

        /**
         * @return <code>true</code> if the node type of the event locations need to be checked
         */
        private boolean shouldCheckNodeType() {
            return ((this.nodeTypeNames != null) && (this.nodeTypeNames.length != 0));
        }
    }

}
