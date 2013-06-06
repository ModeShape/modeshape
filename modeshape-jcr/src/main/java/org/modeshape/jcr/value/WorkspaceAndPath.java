package org.modeshape.jcr.value;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;

/**
 * A path within a given workspace.
 */
@Immutable
public final class WorkspaceAndPath {

    private final String workspaceName;
    private final Path path;

    /**
     * Create a new combination of a path within a named workspace.
     * 
     * @param workspaceName the workspace name; may not be null
     * @param path the path; may not be null
     */
    public WorkspaceAndPath( String workspaceName,
                             Path path ) {
        this.workspaceName = workspaceName;
        this.path = path;
        assert this.workspaceName != null;
        assert this.path != null;
    }

    /**
     * Get the path.
     * 
     * @return the path; never null
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get the workspace name.
     * 
     * @return the workspace name; never null
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    @Override
    public int hashCode() {
        return HashCode.compute(workspaceName, path);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof WorkspaceAndPath) {
            WorkspaceAndPath that = (WorkspaceAndPath)obj;
            return this.workspaceName.equals(that.workspaceName) && this.path.equals(that.path);
        }
        return false;
    }

    @Override
    public String toString() {
        return workspaceName + ":/" + path;
    }

    /**
     * Create a new instance that contains the current workspace name but which uses the supplied path.
     * 
     * @param path the new path
     * @return the new WorkspaceAndPath instance, or this instance only if the supplied path is the same Path object already used
     *         by this instance; never null
     */
    public WorkspaceAndPath withPath( Path path ) {
        return path == this.path ? this : new WorkspaceAndPath(workspaceName, path);
    }
}
