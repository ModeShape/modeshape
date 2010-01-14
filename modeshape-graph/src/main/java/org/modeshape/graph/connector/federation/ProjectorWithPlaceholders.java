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
package org.modeshape.graph.connector.federation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;

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
        Map<Path, ProxyNode> proxyNodesByPath = new HashMap<Path, ProxyNode>();
        Map<Path, PlaceholderNode> placeholdersByPath = new HashMap<Path, PlaceholderNode>();
        for (Projection projection : projections) {
            // Create for all of the top-level nodes ...
            for (Path path : projection.getTopLevelPathsInRepository(pathFactory)) {
                if (path.isRoot()) continue;
                // Create ProxyNodes for each corresponding path-in-source ...
                Location inRepository = Location.create(path);
                ProxyNode previous = null;
                for (Path pathInSource : projection.getPathsInSource(path, pathFactory)) {
                    Location inSource = Location.create(pathInSource);
                    ProxyNode proxy = new ProxyNode(projection, inSource, inRepository);
                    if (previous == null) {
                        previous = proxy;
                        proxyNodesByPath.put(path, proxy);
                    } else {
                        previous.add(proxy);
                    }
                }
                // Walk up the in-repository path to create the placeholder nodes ...
                ProjectedNode child = previous;
                while (!path.isRoot()) {
                    // Create a projected node for the parent of this path ...
                    Path parent = path.getParent();
                    PlaceholderNode parentPlaceholder = placeholdersByPath.get(parent);
                    if (parentPlaceholder == null) {
                        // Need to create the placeholder ...
                        Map<Name, Property> properties = Collections.emptyMap();
                        Location location = Location.create(parent, UUID.randomUUID());
                        parentPlaceholder = new PlaceholderNode(location, properties, new ArrayList<ProjectedNode>());
                        placeholdersByPath.put(parent, parentPlaceholder);
                        placeholderNodes.add(parentPlaceholder);
                        parentPlaceholder.children().add(child);
                    } else {
                        // The placeholder already exists, so make sure we're not adding it to this parent twice ...
                        List<ProjectedNode> children = parentPlaceholder.children();
                        if (!children.contains(child)) children.add(child);
                    }
                    child = parentPlaceholder;
                    path = parent;
                }
            }
        }
    }
}
