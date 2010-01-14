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

import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Path.Segment;

/**
 * A Projector for federated repository configurations that have a single mirrored projection (a projection that is a one-for-one
 * mapping to a single source) and a single branch projection that maps a single path in the source to a single path in the
 * federated repository.
 * <p>
 * For example, consider a federated repository that maps the "/" path in source1 into "/" in the federated repository, and the
 * "/path/in/source" path into the "/federated/path" branch in the federated repository. Thus, anything at or below
 * "/federated/path" goes into the (If this were the only projection, then a {@link MirrorProjector} could be used.)
 */
@Immutable
final class BranchedMirrorProjector extends ProjectorWithPlaceholders {

    /**
     * Attempt to create an instance of the {@link BranchedMirrorProjector} with the supplied projections using the supplied
     * context.
     * 
     * @param context the context; may not be null
     * @param projections the projections in the federated repository; may not be null
     * @return the branched mirror projector, or null if the projections didn't match the criteria for such a projector
     */
    static BranchedMirrorProjector with( ExecutionContext context,
                                         List<Projection> projections ) {
        assert projections != null;
        assert context != null;
        if (projections.size() != 2) return null;
        Projection first = projections.get(0);
        Projection second = projections.get(1);
        if (first.getRules().size() != 1) return null;
        if (second.getRules().size() != 1) return null;
        Rule firstRule = first.getRules().get(0);
        Rule secondRule = second.getRules().get(0);
        assert firstRule != null;
        assert secondRule != null;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        List<Path> firstTopLevelPaths = first.getRules().get(0).getTopLevelPathsInRepository(pathFactory);
        if (firstTopLevelPaths.size() != 1) return null;
        List<Path> secondTopLevelPaths = second.getRules().get(0).getTopLevelPathsInRepository(pathFactory);
        if (secondTopLevelPaths.size() != 1) return null;
        Path firstTopLevelPath = firstTopLevelPaths.get(0);
        Path secondTopLevelPath = secondTopLevelPaths.get(0);
        if (firstTopLevelPath.isRoot()) {
            // We're good, so create the instance ...
            return new BranchedMirrorProjector(context, projections, first, second, secondTopLevelPath,
                                               secondRule.getPathInSource(secondTopLevelPath, pathFactory));
        }
        // the second top-level path must be a root ...
        if (!secondTopLevelPath.isRoot()) return null;
        // We're good, so create the instance ...
        return new BranchedMirrorProjector(context, projections, second, first, firstTopLevelPath,
                                           firstRule.getPathInSource(firstTopLevelPath, pathFactory));
    }

    private final Projection mirrorProjection;
    private final Projection branchProjection;
    private final Path branchFederatedPath;
    private final Path branchSourcePath;
    private final boolean branchSourceUsesSamePath;

    BranchedMirrorProjector( ExecutionContext context,
                             List<Projection> projections,
                             Projection mirrorProjection,
                             Projection branchProjection,
                             Path branchFederatedPath,
                             Path branchSourcePath ) {
        super(context, projections);
        assert mirrorProjection != null;
        assert branchProjection != null;
        assert branchFederatedPath != null;
        assert branchSourcePath != null;
        this.mirrorProjection = mirrorProjection;
        this.branchProjection = branchProjection;
        this.branchFederatedPath = branchFederatedPath;
        this.branchSourcePath = branchSourcePath;
        this.branchSourceUsesSamePath = branchSourcePath.equals(branchFederatedPath);
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
        assert location != null;
        if (location.hasPath()) {
            Path path = location.getPath();
            if (path.isRoot()) {
                // It is a projection of the mirror's root and the placeholder for the branch root ...
                if (requiresUpdate && mirrorProjection.isReadOnly()) return null;
                ProxyNode result = new ProxyNode(mirrorProjection, location, location.with(branchSourcePath),
                                                 branchSourceUsesSamePath);
                result.add(isPlaceholder(location));
                return result;
            }
            ProjectedNode onBranch = isOnBranch(path, location, context);
            if (onBranch != null) {
                if (requiresUpdate && branchProjection.isReadOnly()) return null;
                return onBranch;
            }
            // Otherwise it is a proxy to the mirror only ...
            if (requiresUpdate && mirrorProjection.isReadOnly()) return null;
            return new ProxyNode(mirrorProjection, location, location, true);
        }

        // The location has no path and only identifier properties ...
        if (requiresUpdate) {
            if (branchProjection.isReadOnly()) {
                // Can't update branch ...
                if (mirrorProjection.isReadOnly()) return null;
                return new ProxyNode(mirrorProjection, location, location, true); // no paths
            }
            if (mirrorProjection.isReadOnly()) {
                // Can't update mirror ...
                return new ProxyNode(branchProjection, location, location, branchSourceUsesSamePath); // no paths
            }
        }

        // This is just a read, so create one projection for the mirror, and a second one for the branch.
        ProjectedNode result = new ProxyNode(mirrorProjection, location, location, true);
        result.add(new ProxyNode(branchProjection, location, location, branchSourceUsesSamePath)); // no paths
        return result;
    }

    protected final ProjectedNode isOnBranch( Path federatedPath,
                                              Location location,
                                              ExecutionContext context ) {
        Iterator<Segment> branchIter = branchFederatedPath.iterator();
        Iterator<Segment> federIter = federatedPath.iterator();
        // Look at the first path ...
        if (branchIter.hasNext() && federIter.hasNext()) {
            if (!branchIter.next().equals(federIter.next())) return null; // not on branch
        }
        // Otherwise, the federated path is on the branch, but how far ...
        while (branchIter.hasNext() && federIter.hasNext()) {
            if (!branchIter.next().equals(federIter.next())) {
                // Didn't make it all the way along the branch ...
                return null;
            }
        }
        if (branchIter.hasNext()) {
            // Didn't make it all the way down to the top of the branch, but it is a placeholder ...
            return isPlaceholder(location);
        }
        // Otherwise it is within the brach ...
        Location locationInSource = location;
        if (!branchSourceUsesSamePath) {
            // The source uses a different path ...
            if (federIter.hasNext()) {
                Path subpath = federatedPath.subpath(branchFederatedPath.size());
                Path sourcePath = context.getValueFactories().getPathFactory().create(branchSourcePath, subpath);
                locationInSource = location.with(sourcePath);
            } else {
                locationInSource = location.with(branchSourcePath);
            }
        }
        return new ProxyNode(branchProjection, locationInSource, location, branchSourceUsesSamePath);
    }
}
