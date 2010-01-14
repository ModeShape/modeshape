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
 * A Projector for federated repository configurations that are a direct one-for-one mirror against a single source repository. In
 * other words, the federated repository has a single projection with a single "/ => /" rule.
 */
@Immutable
final class MirrorProjector implements Projector {

    /**
     * Attempt to create an instance of the {@link MirrorProjector} with the supplied projections using the supplied context.
     * 
     * @param context the context; may not be null
     * @param projections the projections in the federated repository; may not be null
     * @return the mirror projector, or null if the projections didn't match the criteria for such a projector
     */
    static MirrorProjector with( ExecutionContext context,
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
        if (!topLevelPaths.get(0).isRoot()) return null;
        return new MirrorProjector(projection);
    }

    private final Projection projection;

    private MirrorProjector( Projection projection ) {
        this.projection = projection;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation <i>always<i> returns a single {@link ProxyNode} for the location in the single projection.
     * </p>
     * 
     * @see org.modeshape.graph.connector.federation.Projector#project(ExecutionContext, Location, boolean)
     */
    public ProjectedNode project( ExecutionContext context,
                                  Location location,
                                  boolean requiresUpdate ) {
        if (requiresUpdate && projection.isReadOnly()) return null;
        return new ProxyNode(projection, location, location);
    }

}
