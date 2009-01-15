/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.store.jpa.util;

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
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;

/**
 * Represents a cache of the known node information, including a node's actual {@link Location} and the complete set of children.
 * 
 * @author Randall Hauch
 */
public class RequestProcessorCache {

    private final PathFactory pathFactory;
    private final Map<Path, Location> locationByPath = new HashMap<Path, Location>();
    private final Map<Path, LinkedList<Location>> childrenByParentPath = new HashMap<Path, LinkedList<Location>>();

    public RequestProcessorCache( PathFactory pathFactory ) {
        assert pathFactory != null;
        this.pathFactory = pathFactory;
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
        assert newLocation != null;
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

        // If the children are cached for the new parent ...
        Path newPath = newLocation.getPath();
        assert newPath != null;
        LinkedList<Location> newSiblings = childrenByParentPath.get(newPath.getParent());
        if (newSiblings != null) {
            newSiblings.add(newLocation);
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
