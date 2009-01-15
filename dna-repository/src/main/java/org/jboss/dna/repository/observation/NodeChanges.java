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
package org.jboss.dna.repository.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * A utility class that builds node changes from a sequence of events.
 * @author Randall Hauch
 */
public class NodeChanges implements Iterable<NodeChange> {

    public static NodeChanges create( final String repositoryWorkspaceName, Iterable<Event> events ) throws RepositoryException {
        Map<String, NodeChangeDetails> detailsByNodePath = new HashMap<String, NodeChangeDetails>();
        // Process each of the events, extracting the node path and property details for each ...
        for (Event event : events) {
            final int eventType = event.getType();
            final String eventPath = event.getPath();
            if (eventType == Event.PROPERTY_ADDED || eventType == Event.PROPERTY_CHANGED || eventType == Event.PROPERTY_REMOVED) {
                // Extract the node's path and property name from the even path ...
                int lastDelim = eventPath.lastIndexOf('/');
                if (lastDelim < 1 || lastDelim == (eventPath.length() - 1)) {
                    // The last delimiter doesn't exist, is the first character, or is the last character...
                    I18n msg =
                        eventType == Event.PROPERTY_ADDED ? RepositoryI18n.errorFindingPropertyNameInPropertyAddedEvent : eventType == Event.PROPERTY_CHANGED ? RepositoryI18n.errorFindingPropertyNameInPropertyChangedEvent : RepositoryI18n.errorFindingPropertyNameInPropertyRemovedEvent;
                    Logger.getLogger(NodeChanges.class).error(msg, eventPath);
                    continue;
                }
                String nodePath = eventPath.substring(0, lastDelim); // excludes the last delim
                String propertyName = eventPath.substring(lastDelim + 1);
                // Record the details ...
                NodeChangeDetails details = detailsByNodePath.get(nodePath);
                if (details == null) {
                    details = new NodeChangeDetails(nodePath);
                    detailsByNodePath.put(nodePath, details);
                }
                switch (eventType) {
                    case Event.PROPERTY_ADDED: {
                        details.addProperty(propertyName);
                        break;
                    }
                    case Event.PROPERTY_CHANGED: {
                        details.changeProperty(propertyName);
                        break;
                    }
                    case Event.PROPERTY_REMOVED: {
                        details.removeProperty(propertyName);
                        break;
                    }
                }
            } else if (eventType == Event.NODE_ADDED || eventType == Event.NODE_REMOVED) {
                // Remove the last delimiter if it appears at the end of the path ...
                String nodePath = eventPath;
                if (nodePath.length() > 1 && nodePath.charAt(nodePath.length() - 1) == '/') {
                    nodePath = nodePath.substring(0, nodePath.length() - 1);
                }
                // Record the details ...
                NodeChangeDetails details = detailsByNodePath.get(nodePath);
                if (details == null) {
                    details = new NodeChangeDetails(nodePath);
                    detailsByNodePath.put(nodePath, details);
                }
                details.addEventType(eventType);
            }
        }

        // Create the node changes ...
        List<NodeChange> result = new ArrayList<NodeChange>(detailsByNodePath.size());
        for (NodeChangeDetails detail : detailsByNodePath.values()) {
            NodeChange change = new NodeChange(repositoryWorkspaceName, detail.getNodePath(), detail.getEventTypes(), detail.getModifiedProperties(), detail.getRemovedProperties());
            result.add(change);
        }
        return new NodeChanges(result);
    }

    protected static class NodeChangeDetails {

        private final String nodePath;
        private final Set<String> modifiedProperties = new HashSet<String>();
        private final Set<String> removedProperties = new HashSet<String>();
        private int eventTypes;

        protected NodeChangeDetails( String nodePath ) {
            this.nodePath = nodePath;
        }

        public void addEventType( int eventType ) {
            this.eventTypes |= eventType;
        }

        public void addProperty( String propertyName ) {
            this.modifiedProperties.add(propertyName);
            this.eventTypes |= Event.PROPERTY_ADDED;
        }

        public void changeProperty( String propertyName ) {
            this.modifiedProperties.add(propertyName);
            this.eventTypes |= Event.PROPERTY_CHANGED;
        }

        public void removeProperty( String propertyName ) {
            this.removedProperties.add(propertyName);
            this.eventTypes |= Event.PROPERTY_REMOVED;
        }

        /**
         * @return nodeAction
         */
        public int getEventTypes() {
            return this.eventTypes;
        }

        /**
         * @return nodePath
         */
        public String getNodePath() {
            return this.nodePath;
        }

        /**
         * @return addedProperties
         */
        public Set<String> getModifiedProperties() {
            return this.modifiedProperties;
        }

        /**
         * @return removedProperties
         */
        public Set<String> getRemovedProperties() {
            return this.removedProperties;
        }
    }

    protected static final Comparator<NodeChange> PRE_ORDER = new Comparator<NodeChange>() {

        public int compare( NodeChange change1, NodeChange change2 ) {
            return change1.getAbsolutePath().compareTo(change2.getAbsolutePath());
        }
    };

    private final List<NodeChange> changesInPreOrder;

    protected NodeChanges( List<NodeChange> changes ) {
        this.changesInPreOrder = Collections.unmodifiableList(changes);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<NodeChange> iterator() {
        return this.changesInPreOrder.iterator();
    }

    public Iterator<NodeChange> getPreOrder() {
        return this.changesInPreOrder.iterator();
    }

    public int size() {
        return this.changesInPreOrder.size();
    }

}
