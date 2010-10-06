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
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;

/**
 * A Projector for federated repository configurations that are an offset, direct one-for-one mirror against a single source
 * repository with a subgraph projected below the federated root. In other words, the federated repository has a single projection
 * with a single "/something/below/root => /source/node" rule. Note that this project works even when the path in source is the
 * root node, such as "/something/below/root => /".
 */
@Immutable
final class OffsetMirrorProjector extends ProjectorWithPlaceholders {

    /**
     * Attempt to create an instance of the {@link OffsetMirrorProjector} with the supplied projections using the supplied
     * context.
     * 
     * @param context the context; may not be null
     * @param projections the projections in the federated repository; may not be null
     * @return the offset mirror projector, or null if the projections didn't match the criteria for such a projector
     */
    static OffsetMirrorProjector with( ExecutionContext context,
                                       List<Projection> projections ) {
        assert projections != null;
        assert context != null;
        // There must be a single projection ...
        if (projections.size() != 1) return null;
        Projection projection = projections.get(0);
        assert projection != null;
        // That projection may only have one rule ...
        if (projection.getRules().size() != 1) return null;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        // The # of paths in repository must be 1 ...
        List<Path> topLevelPaths = projection.getRules().get(0).getTopLevelPathsInRepository(pathFactory);
        if (topLevelPaths.size() != 1) return null;
        Path topLevelPath = topLevelPaths.get(0);
        assert topLevelPath != null;
        // The federated path may not be the root ...
        if (topLevelPath.isRoot()) return null;

        // The corresponding source path may or may not be the root ...
        Path sourcePath = projection.getRules().get(0).getPathInSource(topLevelPath, pathFactory);

        return new OffsetMirrorProjector(context, projections, topLevelPath, sourcePath);
    }

    private final Projection projection;
    private final Path offset;
    private final int offsetSize;
    private final Path sourcePath;

    private OffsetMirrorProjector( ExecutionContext context,
                                   List<Projection> projections,
                                   Path offset,
                                   Path sourcePath ) {
        super(context, projections);
        this.projection = projections.get(0);
        this.offset = offset;
        this.offsetSize = offset.size();
        this.sourcePath = sourcePath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.federation.Projector#project(ExecutionContext, Location, boolean)
     */
    public ProjectedNode project( ExecutionContext context,
                                  Location location,
                                  boolean requiresUpdate ) {
        if (requiresUpdate && projection.isReadOnly()) return null;
        PlaceholderNode placeholder = isPlaceholder(location);
        if (placeholder != null) return placeholder;

        Path path = location.getPath();
        Location locationInSource = location;
        if (path != null) {
            if (path.size() == offsetSize) {
                // Make sure the path is the same ...
                if (path.equals(offset)) {
                    locationInSource = location.with(sourcePath);
                } else {
                    return null; // not in the path
                }
            } else {
                // Make sure the path begins with the offset ...
                if (path.isDecendantOf(offset)) {
                    Path pathBelowOffset = path.relativeTo(offset);
                    PathFactory pathFactory = context.getValueFactories().getPathFactory();
                    Path pathInSource = pathFactory.create(sourcePath, pathBelowOffset);
                    locationInSource = location.with(pathInSource);
                } else {
                    // Not in the path
                    return null;
                }
            }
        }
        return new ProxyNode(projection, locationInSource, location);
    }

}
