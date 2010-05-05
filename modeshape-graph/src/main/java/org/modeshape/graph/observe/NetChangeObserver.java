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
package org.modeshape.graph.observe;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.DeleteChildrenRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RenameNodeRequest;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;

/**
 * A specialized {@link Observer} that figures out the net changes made during a single {@link Changes set of changes}. For
 * example, if a property is updated and then updated again, the net change will be a single change. Or, if a node is created and
 * then deleted, no net change will be observed.
 */
@ThreadSafe
public abstract class NetChangeObserver extends ChangeObserver {

    public enum ChangeType {
        NODE_ADDED,
        NODE_REMOVED,
        PROPERTY_ADDED,
        PROPERTY_REMOVED,
        PROPERTY_CHANGED,
        NODE_LOCKED,
        NODE_UNLOCKED;
    }

    protected NetChangeObserver() {
    }

    /**
     * @param workspace the workspace of the location (never <code>null</code>)
     * @param location the location whose details are being deleted (never <code>null</code>)
     * @param workspaceLocationMap the map where the details are stored (never <code>null</code>)
     */
    private void deleteLocationDetails( String workspace,
                                        Location location,
                                        Map<String, Map<Location, NetChangeDetails>> workspaceLocationMap ) {
        Map<Location, NetChangeDetails> detailsByLocation = workspaceLocationMap.get(workspace);
        assert (detailsByLocation != null);
        detailsByLocation.remove(location);
    }

    /**
     * @param workspace the workspace of the location (never <code>null</code>)
     * @param location the location whose details are being requested (never <code>null</code>)
     * @param workspaceLocationMap the map where the details are stored (never <code>null</code>)
     * @return the found or created details (never <code>null</code>)
     */
    private NetChangeDetails findDetailsByLocation( String workspace,
                                                    Location location,
                                                    Map<String, Map<Location, NetChangeDetails>> workspaceLocationMap ) {
        Map<Location, NetChangeDetails> detailsByLocation = workspaceLocationMap.get(workspace);
        NetChangeDetails details = null;

        if (detailsByLocation == null) {
            detailsByLocation = new TreeMap<Location, NetChangeDetails>();
            workspaceLocationMap.put(workspace, detailsByLocation);
            details = new NetChangeDetails();
            detailsByLocation.put(location, details);
        } else {
            details = detailsByLocation.get(location);

            if (details == null) {
                details = new NetChangeDetails();
                detailsByLocation.put(location, details);
            }
        }

        return details;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.ChangeObserver#notify(org.modeshape.graph.observe.Changes)
     */
    @Override
    public void notify( Changes changes ) {
        Map<String, Map<Location, NetChangeDetails>> detailsByLocationByWorkspace = new HashMap<String, Map<Location, NetChangeDetails>>();
        // Process each of the events, extracting the node path and property details for each ...
        for (ChangeRequest change : changes.getChangeRequests()) {
            Location location = change.changedLocation();
            assert (location.getPath() != null);

            // Find or create the NetChangeDetails for this node ...
            String workspace = change.changedWorkspace();
            NetChangeDetails details = findDetailsByLocation(workspace, location, detailsByLocationByWorkspace);
            Location original;
            NetChangeDetails originalDetails;

            // Process the specific kind of change ...
            switch (change.getType()) {
                case CREATE_NODE:
                    CreateNodeRequest create = (CreateNodeRequest)change;
                    details.addEventType(ChangeType.NODE_ADDED);
                    for (Property property : create) {
                        details.addProperty(property);
                    }
                    break;
                case UPDATE_PROPERTIES:
                    UpdatePropertiesRequest update = (UpdatePropertiesRequest)change;
                    for (Map.Entry<Name, Property> entry : update.properties().entrySet()) {
                        Name propName = entry.getKey();
                        Property property = entry.getValue();

                        if (property != null) {
                            if (update.isNewProperty(propName)) {
                                details.addProperty(property);
                            } else {
                                details.changeProperty(property);
                            }
                        } else {
                            details.removeProperty(propName);
                        }
                    }
                    break;
                case SET_PROPERTY:
                    SetPropertyRequest set = (SetPropertyRequest)change;

                    if (set.isNewProperty()) {
                        details.addProperty(set.property());
                    } else {
                        details.changeProperty(set.property());
                    }
                    break;
                case REMOVE_PROPERTY:
                    RemovePropertyRequest remove = (RemovePropertyRequest)change;
                    details.removeProperty(remove.propertyName());
                    break;
                case DELETE_BRANCH:
                    // if the node was previously added than a remove results in a net no change
                    if (details.getEventTypes().contains(ChangeType.NODE_ADDED)) {
                        deleteLocationDetails(workspace, location, detailsByLocationByWorkspace);
                    } else {
                        details.addEventType(ChangeType.NODE_REMOVED);
                    }
                    break;
                case DELETE_CHILDREN:
                    DeleteChildrenRequest delete = (DeleteChildrenRequest)change;
                    for (Location deletedChild : delete.getActualChildrenDeleted()) {
                        NetChangeDetails childDetails = findDetailsByLocation(workspace,
                                                                              deletedChild,
                                                                              detailsByLocationByWorkspace);
                        // if a child node was previously added than a remove results in a net no change
                        if (childDetails.getEventTypes().contains(ChangeType.NODE_ADDED)) {
                            deleteLocationDetails(workspace, deletedChild, detailsByLocationByWorkspace);
                        } else {
                            childDetails.addEventType(ChangeType.NODE_REMOVED);
                        }
                    }
                    break;
                case LOCK_BRANCH:
                    details.setLockAction(LockAction.LOCKED);
                    break;
                case UNLOCK_BRANCH:
                    details.setLockAction(LockAction.UNLOCKED);
                    break;
                case COPY_BRANCH:
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;
                case MOVE_BRANCH:
                    // the old location is a removed node event and if it is the same location as the original location it is a
                    // reorder
                    original = ((MoveBranchRequest)change).getActualLocationBefore();
                    originalDetails = findDetailsByLocation(workspace, original, detailsByLocationByWorkspace);
                    originalDetails.addEventType(ChangeType.NODE_REMOVED);

                    // the new location is a new node event
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;
                case CLONE_BRANCH:
                    CloneBranchRequest cloneRequest = (CloneBranchRequest)change;

                    // create event details for any nodes that were removed
                    for (Location removed : cloneRequest.getRemovedNodes()) {
                        NetChangeDetails removedDetails = findDetailsByLocation(workspace, removed, detailsByLocationByWorkspace);
                        removedDetails.addEventType(ChangeType.NODE_REMOVED);
                    }

                    // create event details for new node
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;

                case RENAME_NODE:
                    // the old location is a removed node event
                    original = ((RenameNodeRequest)change).getActualLocationBefore();
                    originalDetails = findDetailsByLocation(workspace, original, detailsByLocationByWorkspace);
                    originalDetails.addEventType(ChangeType.NODE_REMOVED);

                    // the new location is a new node event
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;
                case UPDATE_VALUES:
                    UpdateValuesRequest updateValuesRequest = (UpdateValuesRequest)change;

                    if (!updateValuesRequest.addedValues().isEmpty() || !updateValuesRequest.removedValues().isEmpty()) {
                        assert (updateValuesRequest.getActualProperty() != null);

                        if (updateValuesRequest.isNewProperty()) {
                            details.addEventType(ChangeType.PROPERTY_ADDED);
                            details.addProperty(updateValuesRequest.getActualProperty());
                        } else {
                            details.addEventType(ChangeType.PROPERTY_CHANGED);
                            details.changeProperty(updateValuesRequest.getActualProperty());
                        }
                    } else if (details.getEventTypes().isEmpty()) {
                        // details was just created for this request and now it is not needed
                        deleteLocationDetails(workspace, location, detailsByLocationByWorkspace);
                    }
                    break;
                case CREATE_WORKSPACE:
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;
                case DESTROY_WORKSPACE:
                    details.addEventType(ChangeType.NODE_REMOVED);
                    break;
                case CLONE_WORKSPACE:
                    details.addEventType(ChangeType.NODE_ADDED);
                    break;
            }
        }

        // Walk through the net changes ...
        List<NetChange> netChanges = new LinkedList<NetChange>();
        for (Map.Entry<String, Map<Location, NetChangeDetails>> byWorkspaceEntry : detailsByLocationByWorkspace.entrySet()) {
            String workspaceName = byWorkspaceEntry.getKey();
            // Iterate over the entries. Since we've used a TreeSet, we'll get these with the lower paths first ...
            for (Map.Entry<Location, NetChangeDetails> entry : byWorkspaceEntry.getValue().entrySet()) {
                Location location = entry.getKey();
                NetChangeDetails details = entry.getValue();
                netChanges.add(new NetChange(workspaceName, location, details.getEventTypes(), details.getAddedProperties(),
                                             details.getModifiedProperties(), details.getRemovedProperties()));
            }
        }
        // Now notify of all of the changes ...
        notify(new NetChanges(changes, netChanges));
    }

    /**
     * Method that is called for the set of net changes.
     * 
     * @param netChanges the net changes; never null
     */
    protected abstract void notify( NetChanges netChanges );

    /**
     * A set of net changes that were made atomically. Each change is in the form of a frozen {@link ChangeRequest}, and the net
     * change is in the form.
     */
    @Immutable
    public static final class NetChanges extends Changes {
        private static final long serialVersionUID = 1L;

        private final List<NetChange> netChanges;

        public NetChanges( Changes changes,
                           List<NetChange> netChanges ) {
            super(changes);
            assert netChanges != null;
            assert !netChanges.isEmpty();
            this.netChanges = Collections.unmodifiableList(netChanges);
        }

        /**
         * Get the list of net changes.
         * 
         * @return the immutable list of net changes; never null and never empty
         */
        public List<NetChange> getNetChanges() {
            return this.netChanges;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            if (processId.length() != 0) {
                return getTimestamp() + " @" + getUserName() + " [" + getSourceName() + "] - " + netChanges.size() + " changes";
            }
            return getTimestamp() + " @" + getUserName() + " #" + getProcessId() + " [" + getSourceName() + "] - "
                   + netChanges.size() + " changes";
        }
    }

    /**
     * A notification of changes to a node.
     */
    @Immutable
    public static final class NetChange {

        private final String workspaceName;
        private final Location location;
        private final EnumSet<ChangeType> eventTypes;
        private final Set<Property> addedProperties;
        private final Set<Property> modifiedProperties;
        private final Set<Property> addedOrModifiedProperties;
        private final Set<Name> removedProperties;
        private final int hc;

        public NetChange( String workspaceName,
                          Location location,
                          EnumSet<ChangeType> eventTypes,
                          Set<Property> addedProperties,
                          Set<Property> modifiedProperties,
                          Set<Name> removedProperties ) {
            assert workspaceName != null;
            assert location != null;
            this.workspaceName = workspaceName;
            this.location = location;
            this.hc = HashCode.compute(this.workspaceName, this.location);
            this.eventTypes = eventTypes;
            Set<Property> addedOrModified = null;
            if (addedProperties == null) {
                addedProperties = Collections.emptySet();
                addedOrModified = modifiedProperties; // may be null
            } else {
                addedOrModified = addedProperties;
            }
            if (modifiedProperties == null) {
                if (addedOrModified == null) addedOrModified = Collections.emptySet();
                modifiedProperties = Collections.emptySet();
            } else {
                if (addedOrModified == null) {
                    addedOrModified = modifiedProperties;
                } else {
                    addedOrModified = new HashSet<Property>(modifiedProperties);
                    addedOrModified.addAll(addedProperties);
                }
            }
            if (removedProperties == null) removedProperties = Collections.emptySet();
            this.addedProperties = Collections.unmodifiableSet(addedProperties);
            this.modifiedProperties = Collections.unmodifiableSet(modifiedProperties);
            this.removedProperties = Collections.unmodifiableSet(removedProperties);
            this.addedOrModifiedProperties = Collections.unmodifiableSet(addedOrModified);
        }

        /**
         * @return the node location
         */
        public Location getLocation() {
            return this.location;
        }

        /**
         * @return absolutePath
         */
        public Path getPath() {
            return this.location.getPath();
        }

        /**
         * @return repositoryWorkspaceName
         */
        public String getRepositoryWorkspaceName() {
            return this.workspaceName;
        }

        /**
         * @return the added properties
         */
        public Set<Property> getAddedProperties() {
            return this.addedProperties;
        }

        /**
         * @return modifiedProperties
         */
        public Set<Property> getModifiedProperties() {
            return this.modifiedProperties;
        }

        /**
         * Get the combination of {@link #getAddedProperties() added} and {@link #getModifiedProperties() modified} properties.
         * 
         * @return the immutable set of properties that were added or modified; never null but possibly empty
         */
        public Set<Property> getAddedOrModifiedProperties() {
            return this.addedOrModifiedProperties;
        }

        /**
         * @return removedProperties
         */
        public Set<Name> getRemovedProperties() {
            return this.removedProperties;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * Determine whether this net change includes all of the supplied types.
         * 
         * @param jcrEventTypes the types to check for
         * @return true if all of the supplied events are included in this net change, or false otherwise
         */
        public boolean includesAllOf( ChangeType... jcrEventTypes ) {
            for (ChangeType jcrEventType : jcrEventTypes) {
                if (!this.eventTypes.contains(jcrEventType)) return false;
            }
            return true;
        }

        /**
         * Determine whether this net change includes any of the supplied types.
         * 
         * @param jcrEventTypes the types to check for
         * @return true if any of the supplied events are included in this net change, or false otherwise
         */
        public boolean includes( ChangeType... jcrEventTypes ) {
            for (ChangeType jcrEventType : jcrEventTypes) {
                if (this.eventTypes.contains(jcrEventType)) return true;
            }
            return false;
        }

        public boolean isSameNode( NetChange that ) {
            if (that == this) return true;
            if (this.hc != that.hc) return false;
            if (!this.workspaceName.equals(that.workspaceName)) return false;
            if (!this.location.isSame(that.location)) return false;
            return true;
        }

        /**
         * Determine whether this node change includes the setting of new value(s) for the supplied property.
         * 
         * @param property the name of the property
         * @return true if the named property has a new value on this node, or false otherwise
         */
        public boolean isPropertyModified( String property ) {
            return this.modifiedProperties.contains(property);
        }

        /**
         * Determine whether this node change includes the removal of the supplied property.
         * 
         * @param property the name of the property
         * @return true if the named property was removed from this node, or false otherwise
         */
        public boolean isPropertyRemoved( String property ) {
            return this.removedProperties.contains(property);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NetChange) {
                NetChange that = (NetChange)obj;
                if (!this.isSameNode(that)) return false;
                if (this.eventTypes != that.eventTypes) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return this.workspaceName + "=>" + this.location;
        }
    }

    private enum LockAction {
        LOCKED,
        UNLOCKED;
    }

    /**
     * Internal utility class used in the computation of the net changes.
     */
    @NotThreadSafe
    private static class NetChangeDetails {

        private final Set<Property> modifiedProperties = new HashSet<Property>();
        private final Set<Property> addedProperties = new HashSet<Property>();
        private final Set<Name> removedProperties = new HashSet<Name>();
        private EnumSet<ChangeType> eventTypes = EnumSet.noneOf(ChangeType.class);

        protected NetChangeDetails() {
        }

        public void setLockAction( LockAction lockAction ) {
            switch (lockAction) {
                case LOCKED:
                    // always mark as locked and remove any unlocked state
                    eventTypes.add(ChangeType.NODE_LOCKED);
                    eventTypes.remove(ChangeType.NODE_UNLOCKED);
                    break;
                case UNLOCKED:
                    if (!eventTypes.remove(ChangeType.NODE_LOCKED)) {
                        // It was not previously locked by this change set, so we should unlock it ...
                        eventTypes.add(ChangeType.NODE_UNLOCKED);
                    }
                    break;
            }
        }

        public void addEventType( ChangeType eventType ) {
            this.eventTypes.add(eventType);
        }

        public void addProperty( Property property ) {
            this.addedProperties.add(property);
            this.eventTypes.add(ChangeType.PROPERTY_ADDED);
        }

        public void changeProperty( Property property ) {
            // if property was previously added then changed just keep the added
            if (!this.addedProperties.contains(property)) {
                this.modifiedProperties.add(property);
                this.eventTypes.add(ChangeType.PROPERTY_CHANGED);
            }
        }

        public void removeProperty( Name propertyName ) {
            // if property was previously added a remove results in a net no change
            boolean handled = false;

            for (Property property : this.addedProperties) {
                if (property.getName().equals(propertyName)) {
                    handled = true;
                    this.addedProperties.remove(property);

                    // get rid of event type if no longer applicable
                    if (this.addedProperties.isEmpty()) {
                        this.eventTypes.remove(ChangeType.PROPERTY_ADDED);
                    }

                    break;
                }
            }

            if (!handled) {
                // if property was previously changed and now is being removed the change is no longer needed
                for (Property property : this.modifiedProperties) {
                    if (property.getName().equals(propertyName)) {
                        this.modifiedProperties.remove(property);

                        // get rid of event type if no longer applicable
                        if (this.modifiedProperties.isEmpty()) {
                            this.eventTypes.remove(ChangeType.PROPERTY_CHANGED);
                        }

                        break;
                    }
                }

                // now add to removed collection
                this.removedProperties.add(propertyName);
                this.eventTypes.add(ChangeType.PROPERTY_REMOVED);
            }
        }

        /**
         * @return nodeAction
         */
        public EnumSet<ChangeType> getEventTypes() {
            return this.eventTypes;
        }

        /**
         * @return the added properties
         */
        public Set<Property> getAddedProperties() {
            return this.addedProperties;
        }

        /**
         * @return modified properties
         */
        public Set<Property> getModifiedProperties() {
            return this.modifiedProperties;
        }

        /**
         * @return removedProperties
         */
        public Set<Name> getRemovedProperties() {
            return this.removedProperties;
        }
    }
}
