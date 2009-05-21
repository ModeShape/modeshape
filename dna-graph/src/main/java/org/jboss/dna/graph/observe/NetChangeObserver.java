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
package org.jboss.dna.graph.observe;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.RemovePropertyRequest;
import org.jboss.dna.graph.request.SetPropertyRequest;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;

/**
 * A specialized {@link Observer} that figures out the net changes made during a single {@link Changes set of changes}. For
 * example, if a property is updated and then updated again, the net change will be a single change. Or, if a node is created and
 * then deleted, no net change will be observed.
 */
public abstract class NetChangeObserver extends ChangeObserver {

    public enum ChangeType {
        NODE_ADDED,
        NODE_REMOVED,
        PROPERTY_ADDED,
        PROPERTY_REMOVED,
        PROPERTY_CHANGED;
    }

    protected NetChangeObserver() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.ChangeObserver#notify(org.jboss.dna.graph.observe.Changes)
     */
    @Override
    public void notify( Changes changes ) {
        Map<String, Map<Location, NetChangeDetails>> detailsByLocationByWorkspace = new HashMap<String, Map<Location, NetChangeDetails>>();
        // Process each of the events, extracting the node path and property details for each ...
        for (ChangeRequest change : changes) {
            Location location = change.changedLocation();
            String workspace = change.changedWorkspace();

            // Find the NetChangeDetails for this node ...
            Map<Location, NetChangeDetails> detailsByLocation = detailsByLocationByWorkspace.get(workspace);
            NetChangeDetails details = null;
            if (detailsByLocation == null) {
                detailsByLocation = new TreeMap<Location, NetChangeDetails>();
                detailsByLocationByWorkspace.put(workspace, detailsByLocation);
                details = new NetChangeDetails();
                detailsByLocation.put(location, details);
            } else {
                details = detailsByLocation.get(location);
                if (details == null) {
                    details = new NetChangeDetails();
                    detailsByLocation.put(location, details);
                }
            }

            // Process the specific kind of change ...
            if (change instanceof CreateNodeRequest) {
                CreateNodeRequest create = (CreateNodeRequest)change;
                details.addEventType(ChangeType.NODE_ADDED);
                for (Property property : create) {
                    details.addProperty(property);
                }
            } else if (change instanceof UpdatePropertiesRequest) {
                UpdatePropertiesRequest update = (UpdatePropertiesRequest)change;
                for (Map.Entry<Name, Property> entry : update.properties().entrySet()) {
                    Property property = entry.getValue();
                    if (property != null) {
                        details.changeProperty(property);
                    } else {
                        details.removeProperty(entry.getKey());
                    }
                }
            } else if (change instanceof SetPropertyRequest) {
                SetPropertyRequest set = (SetPropertyRequest)change;
                details.changeProperty(set.property());
            } else if (change instanceof RemovePropertyRequest) {
                RemovePropertyRequest remove = (RemovePropertyRequest)change;
                details.removeProperty(remove.propertyName());
            } else if (change instanceof DeleteBranchRequest) {
                details.addEventType(ChangeType.NODE_REMOVED);
            } else if (change instanceof DeleteChildrenRequest) {
                DeleteChildrenRequest delete = (DeleteChildrenRequest)change;
                for (Location deletedChild : delete.getActualChildrenDeleted()) {
                    NetChangeDetails childDetails = detailsByLocation.get(location);
                    if (childDetails == null) {
                        childDetails = new NetChangeDetails();
                        detailsByLocation.put(deletedChild, childDetails);
                    }
                    childDetails.addEventType(ChangeType.NODE_REMOVED);
                }
            }
        }

        // Walk through the net changes ...
        String sourceName = changes.getSourceName();
        for (Map.Entry<String, Map<Location, NetChangeDetails>> byWorkspaceEntry : detailsByLocationByWorkspace.entrySet()) {
            String workspaceName = byWorkspaceEntry.getKey();
            // Iterate over the entries. Since we've used a TreeSet, we'll get these with the lower paths first ...
            for (Map.Entry<Location, NetChangeDetails> entry : byWorkspaceEntry.getValue().entrySet()) {
                Location location = entry.getKey();
                NetChangeDetails details = entry.getValue();
                notify(new NetChange(sourceName, workspaceName, location, details.getEventTypes(),
                                     details.getModifiedProperties(), details.getRemovedProperties()));
            }
        }
    }

    /**
     * Method that is called for each net change.
     * 
     * @param change
     */
    protected abstract void notify( NetChange change );

    /**
     * A notification of changes to a node.
     */
    @Immutable
    public static class NetChange {

        private final String sourceName;
        private final String workspaceName;
        private final Location location;
        private final EnumSet<ChangeType> eventTypes;
        private final Set<Property> modifiedProperties;
        private final Set<Name> removedProperties;
        private final int hc;

        public NetChange( String sourceName,
                          String workspaceName,
                          Location location,
                          EnumSet<ChangeType> eventTypes,
                          Set<Property> modifiedProperties,
                          Set<Name> removedProperties ) {
            assert sourceName != null;
            assert workspaceName != null;
            assert location != null;
            this.sourceName = sourceName;
            this.workspaceName = workspaceName;
            this.location = location;
            this.hc = HashCode.compute(this.workspaceName, this.location);
            this.eventTypes = eventTypes;
            if (modifiedProperties == null) modifiedProperties = Collections.emptySet();
            if (removedProperties == null) removedProperties = Collections.emptySet();
            this.modifiedProperties = Collections.unmodifiableSet(modifiedProperties);
            this.removedProperties = Collections.unmodifiableSet(removedProperties);
        }

        /**
         * @return absolutePath
         */
        public Path getPath() {
            return this.location.getPath();
        }

        /**
         * @return repositorySourceName
         */
        public String getRepositorySourceName() {
            return this.sourceName;
        }

        /**
         * @return repositoryWorkspaceName
         */
        public String getRepositoryWorkspaceName() {
            return this.workspaceName;
        }

        /**
         * @return modifiedProperties
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        public boolean includesAll( ChangeType... jcrEventTypes ) {
            for (ChangeType jcrEventType : jcrEventTypes) {
                if (!this.eventTypes.contains(jcrEventType)) return false;
            }
            return true;
        }

        public boolean includes( ChangeType... jcrEventTypes ) {
            for (ChangeType jcrEventType : jcrEventTypes) {
                if (this.eventTypes.contains(jcrEventType)) return true;
            }
            return false;
        }

        public boolean is( ChangeType jcrEventTypes ) {
            return this.eventTypes.contains(jcrEventTypes);
        }

        public boolean isSameNode( NetChange that ) {
            if (that == this) return true;
            if (this.hc != that.hc) return false;
            if (!this.workspaceName.equals(that.workspaceName)) return false;
            if (!this.location.equals(that.location)) return false;
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

    /**
     * Internal utility class used in the computation of the net changes.
     */
    @NotThreadSafe
    private static class NetChangeDetails {

        private final Set<Property> modifiedProperties = new HashSet<Property>();
        private final Set<Name> removedProperties = new HashSet<Name>();
        private EnumSet<ChangeType> eventTypes = EnumSet.noneOf(ChangeType.class);

        protected NetChangeDetails() {
        }

        public void addEventType( ChangeType eventType ) {
            this.eventTypes.add(eventType);
        }

        public void addProperty( Property property ) {
            this.modifiedProperties.add(property);
            this.eventTypes.add(ChangeType.PROPERTY_ADDED);
        }

        public void changeProperty( Property property ) {
            this.modifiedProperties.add(property);
            this.eventTypes.add(ChangeType.PROPERTY_CHANGED);
        }

        public void removeProperty( Name propertyName ) {
            this.removedProperties.add(propertyName);
            this.eventTypes.add(ChangeType.PROPERTY_REMOVED);
        }

        /**
         * @return nodeAction
         */
        public EnumSet<ChangeType> getEventTypes() {
            return this.eventTypes;
        }

        /**
         * @return addedProperties
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
