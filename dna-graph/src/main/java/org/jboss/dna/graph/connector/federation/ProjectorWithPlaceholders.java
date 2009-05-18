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
package org.jboss.dna.graph.connector.federation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;

/**
 * A Projector for federated repository configurations that are an offset, direct one-for-one mirror against a single source
 * repository that is projected below the federated root. In other words, the federated repository has a single projection with a
 * single "/something/below/root => /" rule.
 */
@Immutable
abstract class ProjectorWithPlaceholders implements Projector {

    private Map<Path, PlaceholderNode> placeholderNodesByPath;
    private Map<UUID, PlaceholderNode> placeholderNodesByUuid;

    ProjectorWithPlaceholders( ExecutionContext context,
                               List<Projection> projections ) {
        placeholderNodesByPath = new HashMap<Path, PlaceholderNode>();
        placeholderNodesByUuid = new HashMap<UUID, PlaceholderNode>();
        // Use the execution context of the source to load the projected nodes ...
        Collection<PlaceholderNode> placeholders = new ArrayList<PlaceholderNode>();
        loadPlaceholderNodes(context, projections, placeholders);
        for (PlaceholderNode placeholder : placeholders) {
            placeholderNodesByPath.put(placeholder.location().getPath(), placeholder);
            placeholderNodesByUuid.put(placeholder.location().getUuid(), placeholder);
        }
    }

    /**
     * Determine whether the specified location is a placeholder node.
     * 
     * @param location the location of the node; may not be null
     * @return the placeholder node, or null if the supplied location does not designate a placeholder node
     */
    public PlaceholderNode isPlaceholder( Location location ) {
        Path path = location.getPath();
        if (path != null) {
            return placeholderNodesByPath.get(path);
        }
        UUID uuid = location.getUuid();
        if (uuid != null) {
            return placeholderNodesByUuid.get(uuid);
        }
        return null;
    }

    /**
     * Load the placeholder nodes for this repository. This method does not modify any state of this instance, but instead
     * populates the supplied map.
     * <p>
     * A placeholder is created for each node above the projections' {@link Projection#getTopLevelPathsInRepository(PathFactory)
     * top-level paths} in the federated repository. Thus, the projected node immediately above a top-level path will contain the
     * top-level path as a child.
     * </p>
     * 
     * @param context the context in which the placeholder nodes should be materialized; may not be null
     * @param projections the projections; may not be null
     * @param placeholderNodes the collection into which should be placed the materialized placeholders
     */
    protected static void loadPlaceholderNodes( ExecutionContext context,
                                                Iterable<Projection> projections,
                                                Collection<PlaceholderNode> placeholderNodes ) {
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        TreeMultimap<Path, Location> childrenForParent = Multimaps.newTreeMultimap();
        for (Projection projection : projections) {
            // Collect the paths to all of the top-level nodes under each of their parents ...
            for (Path path : projection.getTopLevelPathsInRepository(pathFactory)) {
                if (path.isRoot()) continue;
                while (!path.isRoot()) {
                    // Create a projected node for the parent of this path ...
                    Path parent = path.getParent();
                    childrenForParent.put(parent, Location.create(path));
                    path = parent;
                }
            }
        }
        // Now, we want to create a placeholder for each parent, with the list of all children ...
        for (Path parentPath : childrenForParent.keySet()) {
            // Get the children for this parent ...
            List<Location> children = new ArrayList<Location>(childrenForParent.get(parentPath));
            Map<Name, Property> properties = Collections.emptyMap();
            Location location = Location.create(parentPath, UUID.randomUUID());
            PlaceholderNode placeholder = new PlaceholderNode(location, properties, children);
            placeholderNodes.add(placeholder);
        }
    }

}
