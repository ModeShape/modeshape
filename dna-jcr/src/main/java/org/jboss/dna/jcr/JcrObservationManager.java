/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.jboss.dna.graph.JcrLexicon.MIXIN_TYPES;
import static org.jboss.dna.graph.JcrLexicon.PRIMARY_TYPE;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.observe.Changes;
import org.jboss.dna.graph.observe.NetChangeObserver;
import org.jboss.dna.graph.observe.Observable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.session.InvalidStateException;

/**
 * The implementation of JCR {@link ObservationManager}.
 */
final class JcrObservationManager implements ObservationManager {

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

    /**
     * @param session the owning session (never <code>null</code>)
     * @param repositoryObservable the repository observable used to register JCR listeners (never <code>null</code>)
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     */
    public JcrObservationManager( JcrSession session,
                                  Observable repositoryObservable ) {
        CheckArg.isNotNull(session, "session");
        CheckArg.isNotNull(repositoryObservable, "repositoryObservable");

        this.session = session;
        this.repositoryObservable = repositoryObservable;
        this.listeners = new ConcurrentHashMap<EventListener, JcrListenerAdapter>();
        this.namespaceRegistry = this.session.getExecutionContext().getNamespaceRegistry();
        this.valueFactories = this.session.getExecutionContext().getValueFactories();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.observation.ObservationManager#addEventListener(javax.jcr.observation.EventListener, int, java.lang.String,
     *      boolean, java.lang.String[], java.lang.String[], boolean)
     * @throws IllegalArgumentException if <code>listener</code> is <code>null</code>
     */
    public synchronized void addEventListener( EventListener listener,
                                               int eventTypes,
                                               String absPath,
                                               boolean isDeep,
                                               String[] uuid,
                                               String[] nodeTypeName,
                                               boolean noLocal ) {
        checkSession(); // make sure session is still active
        CheckArg.isNotNull(listener, "listener");

        // create wrapper and register
        JcrListenerAdapter adapter = new JcrListenerAdapter(listener, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
        // unregister if already registered
        this.repositoryObservable.unregister(adapter);
        this.repositoryObservable.register(adapter);
        this.listeners.put(listener, adapter);
    }

    /**
     * @throws InvalidStateException if session is not active
     */
    void checkSession() throws InvalidStateException {
        if (!this.session.isLive()) {
            throw new InvalidStateException(JcrI18n.sessionIsNotActive.text(this.session.sessionId()));
        }
    }

    /**
     * @return the namespace registry used by listeners when handling events
     */
    NamespaceRegistry geNamespaceRegistry() {
        return this.namespaceRegistry;
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
    public EventListenerIterator getRegisteredEventListeners() {
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
    public synchronized void removeEventListener( EventListener listener ) {
        checkSession(); // make sure session is still active
        CheckArg.isNotNull(listener, "listener");

        JcrListenerAdapter jcrListener = this.listeners.remove(listener);

        if (jcrListener != null) {
            this.repositoryObservable.unregister(jcrListener);
        }
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
     * An implementation of JCR {@link Event}.
     */
    class JcrEvent implements Event {

        /**
         * The node path.
         */
        private final String path;

        /**
         * The event type.
         */
        private final int type;

        /**
         * The user ID.
         */
        private final String userId;

        /**
         * @param type the event type
         * @param path the node path
         * @param userId the user ID
         */
        public JcrEvent( int type,
                         String path,
                         String userId ) {
            this.type = type;
            this.path = path;
            this.userId = userId;
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
            return this.userId;
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
            }
            sb.append(" at ").append(path).append(" by ").append(userId);
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
                Location parentLocation = Location.create(change.getLocation().getPath().getParent());
                Map<Name, Property> propMap = this.propertiesByLocation.get(parentLocation);
                assert (propMap != null);

                try {
                    String primaryTypeName = stringFactory.create(propMap.get(PRIMARY_TYPE).getFirstValue());
                    String[] mixinNames = null;

                    if (propMap.get(MIXIN_TYPES) != null) {
                        mixinNames = stringFactory.create(propMap.get(MIXIN_TYPES).getValuesAsArray());
                    }

                    return getNodeTypeManager().isDerivedFrom(primaryTypeName, mixinNames, this.nodeTypeNames);
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

                if (this.isDeep) {
                    return matchPath.isAtOrAbove(change.getPath().getParent());
                }

                return matchPath.equals(change.getPath().getParent());
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
         * @see org.jboss.dna.graph.observe.NetChangeObserver#notify(org.jboss.dna.graph.observe.Changes)
         */
        @Override
        public void notify( Changes changes ) {
            // check source first
            if (!acceptBasedOnEventSource(changes)) {
                return;
            }

            try {
                if (shouldCheckNodeType()) {
                    List<Location> changedLocations = new ArrayList<Location>();

                    // loop through changes saving the parent locations of the changed locations
                    for (ChangeRequest request : changes.getChangeRequests()) {
                        Path changedPath = request.changedLocation().getPath();
                        Path parentPath = changedPath.getParent();
                        changedLocations.add(Location.create(parentPath));
                    }

                    // more efficient to get all of the locations at once then it is one at a time using the NetChange
                    Graph graph = getGraph();
                    this.propertiesByLocation = graph.getProperties(PRIMARY_TYPE, MIXIN_TYPES).on(changedLocations);
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
         * @see org.jboss.dna.graph.observe.NetChangeObserver#notify(org.jboss.dna.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( NetChanges netChanges ) {
            Collection<Event> events = new ArrayList<Event>();

            for (NetChange change : netChanges.getNetChanges()) {
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
                String userId = getUserId();

                if (change.includes(ChangeType.NODE_ADDED) && ((this.eventTypes & Event.NODE_ADDED) == Event.NODE_ADDED)) {
                    // create event for added node
                    events.add(new JcrEvent(Event.NODE_ADDED, path.getString(geNamespaceRegistry()), userId));
                } else if (change.includes(ChangeType.NODE_REMOVED)
                           && ((this.eventTypes & Event.NODE_REMOVED) == Event.NODE_REMOVED)) {
                    // create event for removed node
                    events.add(new JcrEvent(Event.NODE_REMOVED, path.getString(geNamespaceRegistry()), userId));
                }

                if (change.includes(ChangeType.PROPERTY_CHANGED)
                    && ((this.eventTypes & Event.PROPERTY_CHANGED) == Event.PROPERTY_CHANGED)) {
                    for (Property property : change.getModifiedProperties()) {
                        // create event for changed property
                        Path propertyPath = pathFactory.create(path, property.getName().getString(geNamespaceRegistry()));
                        events.add(new JcrEvent(Event.PROPERTY_CHANGED, propertyPath.getString(geNamespaceRegistry()), userId));
                    }
                }

                // properties have changed
                if (change.includes(ChangeType.PROPERTY_ADDED)
                    && ((this.eventTypes & Event.PROPERTY_ADDED) == Event.PROPERTY_ADDED)) {
                    for (Property property : change.getAddedProperties()) {
                        // create event for added property
                        Path propertyPath = pathFactory.create(path, property.getName().getString(geNamespaceRegistry()));
                        events.add(new JcrEvent(Event.PROPERTY_ADDED, propertyPath.getString(geNamespaceRegistry()), userId));
                    }
                }

                if (change.includes(ChangeType.PROPERTY_REMOVED)
                    && ((this.eventTypes & Event.PROPERTY_REMOVED) == Event.PROPERTY_REMOVED)) {
                    for (Name name : change.getRemovedProperties()) {
                        // create event for removed property
                        Path propertyPath = pathFactory.create(path, name);
                        events.add(new JcrEvent(Event.PROPERTY_REMOVED, propertyPath.getString(geNamespaceRegistry()), userId));
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
