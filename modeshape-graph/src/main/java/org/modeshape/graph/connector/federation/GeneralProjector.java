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

import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;

/**
 * A Projector for federated repository configurations that are an offset, direct one-for-one mirror against a single source
 * repository that is projected below the federated root. In other words, the federated repository has a single projection with a
 * single "/something/below/root => /" rule.
 */
@Immutable
final class GeneralProjector extends ProjectorWithPlaceholders {

    /**
     * Attempt to create an instance of the {@link GeneralProjector} with the supplied projections using the supplied context.
     * 
     * @param context the context; may not be null
     * @param projections the projections in the federated repository; may not be null
     * @return the offset mirror projector, or null if the projections didn't match the criteria for such a projector
     */
    static GeneralProjector with( ExecutionContext context,
                                  List<Projection> projections ) {
        assert projections != null;
        assert context != null;
        return new GeneralProjector(context, projections);
    }

    private final List<Projection> projections;

    private GeneralProjector( ExecutionContext context,
                              List<Projection> projections ) {
        super(context, projections);
        this.projections = projections;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.federation.Projector#project(ExecutionContext, Location, boolean)
     */
    public ProjectedNode project( ExecutionContext context,
                                  Location location,
                                  boolean requiresUpdate ) {
        PlaceholderNode placeholder = isPlaceholder(location);
        if (placeholder != null) return placeholder;

        // Find the location of the desired node ...
        Path path = location.getPath();
        if (path == null) {
            // There are only identification properties, so return a ProxyNode for each source/workspace ...
            ProjectedNode result = null;
            for (Projection projection : projections) {
                if (requiresUpdate && projection.isReadOnly()) continue;
                ProxyNode proxy = new ProxyNode(projection, location, location, false);
                if (result == null) {
                    result = proxy;
                } else {
                    result.add(proxy);
                }
            }
            return result;
        }
        // We have a path, so see if the path is one of the existing placeholder nodes ...
        ProjectedNode result = isPlaceholder(location);
        if (result != null) return result;

        // Otherwise this node lives below one (or more) of the projections, and we figure this out each time ...
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        for (Projection projection : projections) {
            if (requiresUpdate && projection.isReadOnly()) continue;
            // Project the federated repository path into the paths as they would exist in the source ...
            Set<Path> pathsInSource = projection.getPathsInSource(path, pathFactory);
            for (Path pathInSource : pathsInSource) {
                // Create a ProxyNode for this projection ...
                Location locationInSource = Location.create(pathInSource);
                boolean samePath = pathInSource.equals(path);
                ProxyNode proxy = new ProxyNode(projection, locationInSource, location, samePath);
                if (result == null) {
                    result = proxy;
                } else {
                    result.add(proxy);
                }
            }
        }

        // If there is only one projection ...
        if (result != null && !result.hasNext() && result.isProxy()) {
            // Then we want to include the supplied location's ID properties down to the source ...
            ProxyNode proxy = result.asProxy();
            result = new ProxyNode(proxy.projection(), location.with(result.location().getPath()), location,
                                   proxy.isSameLocationAsOriginal());
        }
        return result;
    }

}
