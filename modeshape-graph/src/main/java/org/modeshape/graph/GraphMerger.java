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
import java.util.Iterator;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.collection.Collections;
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

    private final Graph initialContent;
    private final Set<Name> ignoredProperties = Collections.unmodifiableSet(JcrLexicon.UUID, ModeShapeLexicon.UUID);

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
            Property actual = actualNode.getProperty(desiredProperty.getName());
            boolean performSet = false;
            if (actual == null) {
                performSet = true;
            } else {
                if (ignoredProperties.contains(actual.getName())) continue;
                // the actual property already exists ...
                Iterator<?> actualValues = actual.getValues();
                Iterator<?> desiredValues = desiredProperty.getValues();
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
            }
            if (performSet) {
                batch.set(desiredProperty).on(actualLocation);
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

}
