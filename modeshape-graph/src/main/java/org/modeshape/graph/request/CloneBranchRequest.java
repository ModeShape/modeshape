package org.modeshape.graph.request;

import java.util.Collections;
import java.util.Set;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Instruction that a branch be cloned from one workspace into another. Cloning a branch differs from cloning a branch in that:
 * <ol>
 * <li>Nodes can be cloned within the same workspace or to another workspace; cloned nodes must be cloned from one workspace into
 * another.</li>
 * <li>Copied nodes always get new UUIDs; cloned nodes always maintain their UUIDs and hence must define the behavior that occurs
 * if a node with the same UUID already exists in the new workspace.</li>
 * <li>Nodes can be cloned to a specific name under a specific parent, but can only be added as a new child node at the end of the
 * new parent's children; nodes can be cloned to an exact location among the parent's children, replacing the existing node at
 * that location.</li>
 * </ol>
 */
public class CloneBranchRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final String fromWorkspace;
    private final String intoWorkspace;
    private final Location from;
    private final Location into;
    private final Name desiredName;
    private final Path.Segment desiredSegment;
    private final boolean removeExisting;
    private Set<Location> removedExistingNodes;
    private Location actualFromLocation;
    private Location actualIntoLocation;

    /**
     * Create a request to clone a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be cloned
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the clone should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be cloned
     * @param nameForClone the desired name for the node that results from the clone, or null if the name of the original should
     *        be used
     * @param exactSegmentForClone the exact {@link Path.Segment segment} at which the cloned tree should be rooted.
     * @param removeExisting whether any nodes in the intoWorkspace with the same UUIDs as a node in the source branch should be
     *        removed (if true) or a {@link UuidAlreadyExistsException} should be thrown.
     * @throws IllegalArgumentException if any of the parameters are null except for {@code nameForClone} or {@code
     *         exactSegmentForClone}. Exactly one of {@code nameForClone} and {@code exactSegmentForClone} must be null.
     */
    public CloneBranchRequest( Location from,
                               String fromWorkspace,
                               Location into,
                               String intoWorkspace,
                               Name nameForClone,
                               Path.Segment exactSegmentForClone,
                               boolean removeExisting ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(into, "into");
        CheckArg.isNotNull(fromWorkspace, "fromWorkspace");
        CheckArg.isNotNull(intoWorkspace, "intoWorkspace");
        CheckArg.isNotSame(from, fromWorkspace, into, intoWorkspace);
        assert nameForClone == null ? exactSegmentForClone != null : exactSegmentForClone == null;
        this.from = from;
        this.into = into;
        this.fromWorkspace = fromWorkspace;
        this.intoWorkspace = intoWorkspace;
        this.desiredName = nameForClone;
        this.desiredSegment = exactSegmentForClone;
        this.removeExisting = removeExisting;
    }

    /**
     * Get the location defining the top of the branch to be cloned
     * 
     * @return the from location; never null
     */
    public Location from() {
        return from;
    }

    /**
     * Get the location defining the parent where the new clone is to be placed
     * 
     * @return the to location; never null
     */
    public Location into() {
        return into;
    }

    /**
     * Get the name of the workspace containing the branch to be cloned.
     * 
     * @return the name of the workspace containing the branch to be cloned; never null
     */
    public String fromWorkspace() {
        return fromWorkspace;
    }

    /**
     * Get the name of the workspace where the clone is to be placed
     * 
     * @return the name of the workspace where the clone is to be placed; never null
     */
    public String intoWorkspace() {
        return intoWorkspace;
    }

    /**
     * Determine whether this clone operation is within the same workspace.
     * 
     * @return true if this operation is to be performed within the same workspace, or false if the workspace of the
     *         {@link #from() original} is different than that of the {@link #into() clone}
     */
    public boolean isSameWorkspace() {
        return false;
    }

    /**
     * Get the name of the clone if it is to be different than that of the original.
     * 
     * @return the desired name of the clone, or null if an {@link #desiredSegment() exact segment} is specified.
     */
    public Name desiredName() {
        return desiredName;
    }

    /**
     * Get the exact {@link Path.Segment segment} at which the clone should be rooted
     * 
     * @return the desired segment of the clone, or null if the desired name should be used to generate a new child node for the
     *         {@code into} location
     */
    public Path.Segment desiredSegment() {
        return desiredSegment;
    }

    /**
     * Gets whether the clone should remove existing nodes in the {@link #intoWorkspace new workspace} with the same UUID as any
     * of the nodes in the source branch or should throw an exception if such conflict is detected.
     * 
     * @return whether the clone should remove existing nodes in the {@link #intoWorkspace new workspace} with the same UUID as
     *         any of the nodes in the source branch or should throw an exception if such conflict is detected; true indicates
     *         that the nodes should be removed and false indicates that an exception should be thrown
     */
    public boolean removeExisting() {
        return removeExisting;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param fromLocation the actual location of the node being cloned
     * @param intoLocation the actual location of the new clone of the node
     * @throws IllegalArgumentException if the either location is null; or if the either location does not have a path
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocations( Location fromLocation,
                                    Location intoLocation ) {
        checkNotFrozen();
        CheckArg.isNotNull(fromLocation, "intoLocation");
        CheckArg.isNotNull(intoLocation, "intoLocation");
        if (!fromLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(fromLocation));
        }
        if (!intoLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(intoLocation));
        }

        if (desiredSegment != null && !desiredSegment.equals(intoLocation.getPath().getLastSegment())) {
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotAtCorrectChildSegment.text(intoLocation,
                                                                                                       desiredSegment));
        }

        if (desiredName != null && !desiredName.equals(intoLocation.getPath().getLastSegment().getName())) {
            throw new IllegalArgumentException(
                                               GraphI18n.actualLocationDoesNotHaveCorrectChildName.text(intoLocation, desiredName));
        }

        this.actualFromLocation = fromLocation;
        this.actualIntoLocation = intoLocation;
    }

    /**
     * Get the actual location of the node before being cloned.
     * 
     * @return the actual location of the node before being moved, or null if the actual location was not set
     */
    public Location getActualLocationBefore() {
        return actualFromLocation;
    }

    /**
     * Get the actual location of the node after being cloned.
     * 
     * @return the actual location of the node after being cloned, or null if the actual location was not set
     */
    public Location getActualLocationAfter() {
        return actualIntoLocation;
    }

    /**
     * Set the locations of the nodes that were removed by this operation, if {@link #removeExisting()} is true.
     * 
     * @param existingNodesThatWereRemoved the (immutable) set of existing node locations; may be null
     */
    public void setRemovedNodes( Set<Location> existingNodesThatWereRemoved ) {
        this.removedExistingNodes = existingNodesThatWereRemoved;
    }

    /**
     * Get the set of nodes that were removed because of this clone operation.
     * 
     * @return the immutable set of locations of the nodes that were removed; never null but possibly empty
     */
    public Set<Location> getRemovedNodes() {
        return removedExistingNodes != null ? removedExistingNodes : Collections.<Location>emptySet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return into.hasPath() && into.getPath().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualIntoLocation != null ? actualIntoLocation : into;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return intoWorkspace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(from, fromWorkspace, into, intoWorkspace);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualFromLocation = null;
        this.actualIntoLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            CloneBranchRequest that = (CloneBranchRequest)obj;
            if (!this.from().isSame(that.from())) return false;
            if (!this.into().isSame(that.into())) return false;
            if (!this.fromWorkspace.equals(that.fromWorkspace)) return false;
            if (!this.intoWorkspace.equals(that.intoWorkspace)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (desiredName != null) {
            return "clone branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into() + " with name "
                   + desiredName + " in the \"" + intoWorkspace + "\" workspace";
        }
        return "clone branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into() + " in the \""
               + intoWorkspace + "\" workspace as child " + desiredSegment();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not clone the results.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#clone()
     */
    @Override
    public CloneBranchRequest clone() {
        CloneBranchRequest result = new CloneBranchRequest(actualFromLocation != null ? actualFromLocation : from, fromWorkspace,
                                                           actualIntoLocation != null ? actualIntoLocation : into, intoWorkspace,
                                                           desiredName, desiredSegment, removeExisting);
        result.setRemovedNodes(removedExistingNodes);
        result.setActualLocations(actualFromLocation, actualIntoLocation);
        return result;
    }

    @Override
    public RequestType getType() {
        return RequestType.CLONE_BRANCH;
    }
}
