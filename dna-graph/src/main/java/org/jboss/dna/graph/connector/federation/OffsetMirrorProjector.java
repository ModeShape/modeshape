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

import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;

/**
 * A Projector for federated repository configurations that are an offset, direct one-for-one mirror against a single source
 * repository that is projected below the federated root. In other words, the federated repository has a single projection with a
 * single "/something/below/root => /" rule.
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
        if (projections.size() != 1) return null;
        Projection projection = projections.get(0);
        assert projection != null;
        if (projection.getRules().size() != 1) return null;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        List<Path> topLevelPaths = projection.getRules().get(0).getTopLevelPathsInRepository(pathFactory);
        if (topLevelPaths.size() != 1) return null;
        Path topLevelPath = topLevelPaths.get(0);
        assert topLevelPath != null;
        if (topLevelPath.isRoot()) return null;
        return new OffsetMirrorProjector(context, projections, topLevelPath);
    }

    private final Projection projection;
    private final Path offset;
    private final int offsetSize;

    private OffsetMirrorProjector( ExecutionContext context,
                                   List<Projection> projections,
                                   Path offset ) {
        super(context, projections);
        this.projection = projections.get(0);
        this.offset = offset;
        this.offsetSize = offset.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.federation.Projector#project(ExecutionContext, Location, boolean)
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
                    locationInSource = location.with(context.getValueFactories().getPathFactory().createRootPath());
                } else {
                    return null; // not in the path
                }
            } else {
                // Make sure the path begins with the offset ...
                if (path.isAtOrBelow(offset)) {
                    locationInSource = location.with(path.subpath(offsetSize));
                } else {
                    // Not in the path
                    return null;
                }
            }
        }
        return new ProxyNode(projection, locationInSource, location);
    }

}
