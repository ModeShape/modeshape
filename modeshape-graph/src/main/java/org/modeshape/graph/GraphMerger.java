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
package org.modeshape.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueComparators;

/**
 * A class that can ensure that specific content either exists within the workspace or is created as required.
 */
@NotThreadSafe
class GraphMerger {

    private static final PropertyMerger SKIP_MERGER = new SkipMerger();
    private static final PropertyMerger UNION_MERGER = new UnionPropertyMerger();
    private static final PropertyMerger DEFAULT_MERGER = new DefaultPropertyMerger();

    private static final Map<Name, PropertyMerger> MERGERS;
    static {
        Map<Name, PropertyMerger> mergers = new HashMap<Name, PropertyMerger>();
        mergers.put(JcrLexicon.NAME, SKIP_MERGER);
        mergers.put(JcrLexicon.UUID, SKIP_MERGER);
        mergers.put(ModeShapeLexicon.UUID, SKIP_MERGER);
        mergers.put(JcrLexicon.MIXIN_TYPES, UNION_MERGER);
        MERGERS = Collections.unmodifiableMap(mergers);
    }

    private final Graph initialContent;

    protected GraphMerger( Graph initialContent ) {
        CheckArg.isNotNull(initialContent, "initialContent");
        this.initialContent = initialContent;
    }

    protected void merge( Graph actualGraph,
                          Graph.Batch batch ) {
        CheckArg.isNotNull(actualGraph, "actualGraph");
        // Read the initial content ...
        Subgraph subgraph = this.initialContent.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
        SubgraphNode desiredNode = subgraph.getRoot();

        // Go through all of the areas and ensure the nodes exist ...
        boolean checkProperties = true;
        if (desiredNode.getLocation().getPath().isRoot()) {
            // Don't need to check the root's properties ...
            checkProperties = false;
        }
        Path path = desiredNode.getLocation().getPath();
        Node actualNode = actualGraph.getNodeAt(path);
        matchNode(batch, actualGraph, actualNode, desiredNode, checkProperties, true);
    }

    protected void createSubgraph( Batch batch,
                                   SubgraphNode initialNode,
                                   Path pathOfInitialNode ) {
        // Create the node with the properties ...
        batch.create(pathOfInitialNode).and(initialNode.getProperties()).ifAbsent().and();
        // And create the children ...
        for (Location childLocation : initialNode.getChildren()) {
            Path path = childLocation.getPath();
            SubgraphNode initialChild = initialNode.getNode(path.getLastSegment());
            createSubgraph(batch, initialChild, path);
        }
    }

    protected void matchProperties( Batch batch,
                                    Node actualNode,
                                    Node desiredNode ) {
        Location actualLocation = actualNode.getLocation();
        assert actualLocation != null;
        Collection<Property> desiredProperties = desiredNode.getProperties();
        if (desiredProperties.isEmpty()) return;
        for (Property desiredProperty : desiredProperties) {
            Name propertyName = desiredProperty.getName();
            Property actual = actualNode.getProperty(propertyName);
            if (actual == null) {
                batch.set(desiredProperty).on(actualLocation);
            } else {
                PropertyMerger merger = MERGERS.get(propertyName);
                if (merger == null) merger = DEFAULT_MERGER;
                merger.mergeProperty(batch, actualLocation.getPath(), actual, desiredProperty);
            }
        }
    }

    protected void matchNode( Batch batch,
                              Graph actualContent,
                              Node actualNode,
                              SubgraphNode desiredNode,
                              boolean matchProperties,
                              boolean matchChildren ) {
        Location actualLocation = actualNode.getLocation();
        if (actualLocation == null) {
            // The node does not yet exist ...
            Path path = desiredNode.getLocation().getPath();
            createSubgraph(batch, desiredNode, path);
            batch.create(path).and(desiredNode.getProperties()).ifAbsent().and();
        } else {
            // The node does exist ...
            if (matchProperties) {
                // So first check the properties ...
                matchProperties(batch, actualNode, desiredNode);
            }

            if (matchChildren) {
                // Check the children ...
                matchChildren(batch, actualContent, actualNode, desiredNode);
            }
        }
    }

    protected void matchChildren( Batch batch,
                                  Graph actualGraph,
                                  Node actualNode,
                                  SubgraphNode desiredNode ) {
        // Go through the initial node and make sure all children exist under the actual ...
        for (Location childLocation : desiredNode.getChildren()) {
            Path path = childLocation.getPath();
            SubgraphNode desiredChild = desiredNode.getNode(path.getLastSegment());
            try {
                Node actualChild = actualGraph.getNodeAt(childLocation);
                // The child exists, so match up the node properties and children ...
                matchNode(batch, actualGraph, actualChild, desiredChild, true, true);
            } catch (PathNotFoundException e) {
                // The node does not exist ...
                createSubgraph(batch, desiredChild, path);
            }
        }
    }

    protected static interface PropertyMerger {
        public void mergeProperty( Batch batch,
                                   Path path,
                                   Property actual,
                                   Property desired );
    }

    protected static class SkipMerger implements PropertyMerger {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.GraphMerger.PropertyMerger#mergeProperty(org.modeshape.graph.Graph.Batch,
         *      org.modeshape.graph.property.Path, org.modeshape.graph.property.Property, org.modeshape.graph.property.Property)
         */
        public void mergeProperty( Batch batch,
                                   Path path,
                                   Property actual,
                                   Property desired ) {
            // do nothing ...
        }
    }

    protected static class UnionPropertyMerger implements PropertyMerger {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.GraphMerger.PropertyMerger#mergeProperty(org.modeshape.graph.Graph.Batch,
         *      org.modeshape.graph.property.Path, org.modeshape.graph.property.Property, org.modeshape.graph.property.Property)
         */
        public void mergeProperty( Batch batch,
                                   Path path,
                                   Property actual,
                                   Property desired ) {
            // the actual property already exists ...
            if (desired.size() == 0) {
                // nothing in the desired property ...
                return;
            }

            Set<Object> unionedValues = new HashSet<Object>();
            Iterator<?> actualValues = actual.getValues();
            while (actualValues.hasNext()) {
                Object value = actualValues.next();
                if (value == null) continue;
                unionedValues.add(value);
            }
            int actualSize = unionedValues.size();
            Iterator<?> desiredValues = desired.getValues();
            while (desiredValues.hasNext()) {
                Object value = desiredValues.next();
                if (value == null) continue;
                unionedValues.add(value);
            }
            if (actualSize == unionedValues.size()) {
                // The desired property adds nothing ...
                return;
            }

            batch.set(actual.getName()).on(path).to(unionedValues).and();
        }
    }

    protected static class DefaultPropertyMerger implements PropertyMerger {

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.GraphMerger.PropertyMerger#mergeProperty(Batch, Path, Property, Property)
         */
        public void mergeProperty( Batch batch,
                                   Path path,
                                   Property actual,
                                   Property desired ) {
            // the actual property already exists ...
            Iterator<?> actualValues = actual.getValues();
            Iterator<?> desiredValues = desired.getValues();
            boolean performSet = false;
            while (actualValues.hasNext() && desiredValues.hasNext()) {
                Object actualValue = actualValues.next();
                Object desiredValue = desiredValues.next();
                if (ValueComparators.OBJECT_COMPARATOR.compare(actualValue, desiredValue) != 0) {
                    performSet = true;
                    break;
                }
            }
            if (!performSet && (actualValues.hasNext() || desiredValues.hasNext())) {
                performSet = true;
            }
            if (performSet) {
                batch.set(desired).on(path);
            }
        }
    }

}
