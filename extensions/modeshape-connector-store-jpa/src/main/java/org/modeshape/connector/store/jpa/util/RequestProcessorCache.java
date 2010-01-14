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
package org.modeshape.connector.store.jpa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;

/**
 * Represents a cache of the known node information, including a node's actual {@link Location} and the complete set of children.
 */
@NotThreadSafe
public class RequestProcessorCache {

    private final Map<Long, WorkspaceCache> workspaceCaches = new HashMap<Long, WorkspaceCache>();
    protected final PathFactory pathFactory;

    public RequestProcessorCache( PathFactory pathFactory ) {
        assert pathFactory != null;
        this.pathFactory = pathFactory;
    }

    public Location getLocationFor( Long workspaceId,
                                    Path node ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        return cache != null ? cache.getLocationFor(node) : null;
    }

    public void addNewNode( Long workspaceId,
                            Location location ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        if (cache == null) {
            cache = new WorkspaceCache(workspaceId);
            workspaceCaches.put(workspaceId, cache);
        }
        cache.addNewNode(location);
    }

    public void clear( Long workspaceId ) {
        workspaceCaches.remove(workspaceId);
    }

    public LinkedList<Location> getAllChildren( Long workspaceId,
                                                Path parent ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        return cache != null ? cache.getAllChildren(parent) : null;
    }

    public void setAllChildren( Long workspaceId,
                                Path parent,
                                LinkedList<Location> children ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        if (children == null) {
            // Remove the children ...
            if (cache != null) cache.setAllChildren(parent, null);
        } else {
            if (cache == null) {
                cache = new WorkspaceCache(workspaceId);
                workspaceCaches.put(workspaceId, cache);
            }
            cache.setAllChildren(parent, children);
        }
    }

    public boolean moveNode( Long workspaceId,
                             Location oldLocation,
                             int oldIndexInParent,
                             Location newLocation ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        if (cache == null) {
            // No content from the workspace was cached, so do nothing ..
            return false;
        }
        // The nodes moved within the 'from' workspace ...
        return cache.moveNode(oldLocation, oldIndexInParent, newLocation);
    }

    public boolean removeBranch( Long workspaceId,
                                 Iterable<Location> locations ) {
        WorkspaceCache cache = workspaceCaches.get(workspaceId);
        return cache != null ? cache.removeBranch(locations) : false;
    }

    public String getString( NamespaceRegistry namespaces ) {
        StringBuilder sb = new StringBuilder();
        for (WorkspaceCache cache : workspaceCaches.values()) {
            sb.append(cache.getString(namespaces));
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getString(null);
    }

    public class WorkspaceCache {
        private final Long workspaceId;
        private final Map<Path, Location> locationByPath = new HashMap<Path, Location>();
        private final Map<Path, LinkedList<Location>> childrenByParentPath = new HashMap<Path, LinkedList<Location>>();

        WorkspaceCache( Long workspaceId ) {
            this.workspaceId = workspaceId;
        }

        public Location getLocationFor( Path node ) {
            return locationByPath.get(node);
        }

        public void addNewNode( Location location ) {
            assert location != null;
            Path path = location.getPath();
            assert path != null;
            locationByPath.put(path, location);
        }

        public LinkedList<Location> getAllChildren( Path parent ) {
            return childrenByParentPath.get(parent);
        }

        public void setAllChildren( Path parent,
                                    LinkedList<Location> children ) {
            if (children == null) {
                childrenByParentPath.remove(parent);
            } else {
                childrenByParentPath.put(parent, children);
            }
        }

        public boolean moveNode( Location oldLocation,
                                 int oldIndexInParent,
                                 Location newLocation ) {
            assert oldLocation != null;
            Path oldPath = oldLocation.getPath();
            assert oldPath != null;

            // Now the locations of all nodes below the old location are invalid, as are all lists of children ...
            // The easiest and most efficient thing to do would be to simply remove them from the cache ...
            removeNodesBelow(oldPath, true);

            // Remove the node from the list of children for the old parent ...
            LinkedList<Location> siblings = childrenByParentPath.get(oldPath.getParent());
            boolean removed = false;
            if (siblings != null) {
                removed = removeChildFromParentListOfChildren(siblings, oldLocation, -1);
            }

            if (newLocation != null) {
                // If the children are cached for the new parent ...
                Path newPath = newLocation.getPath();
                assert newPath != null;
                LinkedList<Location> newSiblings = childrenByParentPath.get(newPath.getParent());
                if (newSiblings != null) {
                    newSiblings.add(newLocation);
                }
            }

            return removed;
        }

        protected void removeNodesBelow( Path path,
                                         boolean removeNodeAtSuppliedPath ) {
            if (removeNodeAtSuppliedPath) {
                locationByPath.remove(path);
                childrenByParentPath.remove(path);
            }
            for (Iterator<Path> iter = locationByPath.keySet().iterator(); iter.hasNext();) {
                Path nextPath = iter.next();
                if (nextPath.isDecendantOf(path)) iter.remove();
            }
            for (Iterator<Path> iter = childrenByParentPath.keySet().iterator(); iter.hasNext();) {
                Path nextPath = iter.next();
                if (nextPath.isDecendantOf(path)) iter.remove();
            }
        }

        public boolean removeBranch( Iterable<Location> locations ) {
            if (locations == null) return false;
            Iterator<Location> iter = locations.iterator();
            if (!iter.hasNext()) return false;

            Location topNode = iter.next();

            // Now remove all cached locations and child lists for each deleted node ...
            boolean removed = false;
            while (iter.hasNext()) {
                Location location = iter.next();
                Path path = location.getPath();
                assert path != null;
                if (locationByPath.remove(path) != null) removed = true;
                if (childrenByParentPath.remove(path) != null) removed = true;
            }

            // The first node will be the root of the branch, so remove this from the parent's list of children
            // and adjust all SNS indexes of same-name-siblings that appear after the removed node ...
            Path path = topNode.getPath();
            assert path != null;
            LinkedList<Location> siblings = childrenByParentPath.get(path.getParent());
            if (siblings != null) {
                removed = removeChildFromParentListOfChildren(siblings, topNode, -1);
            }
            childrenByParentPath.remove(path);

            return removed;
        }

        protected boolean removeChildFromParentListOfChildren( LinkedList<Location> siblings,
                                                               Location deletedNode,
                                                               int expectedIndex ) {
            assert siblings != null;
            Path path = deletedNode.getPath();
            Path parentPath = path.getParent();

            // Find and remove the deleted node ...
            boolean removed = false;
            int index = 0;
            Path.Segment deletedNodeSegment = path.getLastSegment();
            ListIterator<Location> iter = null;
            if (expectedIndex > -1 && expectedIndex < siblings.size()) {
                Location locationAtExpectedIndex = siblings.get(expectedIndex);
                if (locationAtExpectedIndex.equals(deletedNode)) {
                    siblings.remove(expectedIndex);
                    removed = true;
                    index = expectedIndex;
                }
            }
            if (!removed) {
                iter = siblings.listIterator();
                while (iter.hasNext()) {
                    Location sibling = iter.next();
                    Path.Segment segment = sibling.getPath().getLastSegment();
                    if (segment.equals(deletedNodeSegment)) {
                        iter.remove();
                        removed = true;
                        break;
                    }
                    ++index;
                }
            }

            // Iterate starting at the supplied index, and adjust the locations for same-name-siblings ...
            iter = siblings.listIterator(index);
            Name name = deletedNodeSegment.getName();
            while (iter.hasNext()) {
                Location laterSibling = iter.next();
                Path siblingPath = laterSibling.getPath();
                Path.Segment segment = siblingPath.getLastSegment();
                // If this sibling has the same name and appeared after deleted node, so decrement the SNS index ...
                if (segment.getName().equals(name)) {
                    assert segment.getIndex() > 1;
                    Path newPath = pathFactory.create(parentPath, name, segment.getIndex() - 1);
                    Location newLocation = laterSibling.with(newPath);
                    iter.set(newLocation);

                    // Remove the existing location for the old path ...
                    locationByPath.remove(siblingPath);

                    // Remove all nodes below (not at) this sibling ...
                    removeNodesBelow(siblingPath, false);

                    // Now put in the location for the modified sibling ...
                    locationByPath.put(newPath, newLocation);
                }
            }
            return removed;
        }

        public String getString( NamespaceRegistry namespaces ) {
            StringBuilder sb = new StringBuilder();
            sb.append("Workspace ");
            sb.append(workspaceId);
            sb.append("\n");
            Set<Path> pathSet = new HashSet<Path>();
            pathSet.addAll(locationByPath.keySet());
            pathSet.addAll(childrenByParentPath.keySet());
            List<Path> paths = new ArrayList<Path>(pathSet);
            Collections.sort(paths);

            // For printing purposes, figure out the maximum length needed for the paths ...
            int maxLength = 0;
            for (Path path : paths) {
                String str = pathString(path, namespaces);
                maxLength = Math.max(maxLength, str.length());
            }
            for (Path path : paths) {
                Location loc = locationByPath.get(path);
                String str = pathString(path, namespaces);
                sb.append(StringUtil.justifyLeft(str, maxLength, ' '));
                if (loc != null) {
                    // sb.append("\t\tlocation");
                    sb.append("    @" + loc.getUuid());
                }
                LinkedList<Location> children = childrenByParentPath.get(path);
                if (children != null) {
                    sb.append("\twith children: ");
                    for (int i = 0; i < children.size(); i++) {
                        Location child = children.get(i);
                        String segmentString = pathSegmentString(child.getPath().getLastSegment(), namespaces);
                        sb.append("\n");
                        sb.append(StringUtil.justifyRight(segmentString, maxLength, ' '));
                        sb.append("    @" + child.getUuid());
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        protected String pathString( Path path,
                                     NamespaceRegistry registry ) {
            return path.getString(registry, null, null);
        }

        protected String pathSegmentString( Path.Segment segment,
                                            NamespaceRegistry registry ) {
            return registry != null ? segment.getString(registry) : segment.getString();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getString(null);
        }
    }
}
