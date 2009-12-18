package org.jboss.dna.graph.connector.path;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.common.util.CheckArg;

public abstract class PathRepository {

    protected final UUID rootNodeUuid;
    private final String sourceName;
    private final String defaultWorkspaceName;
    protected final Map<String, PathWorkspace> workspaces = new HashMap<String, PathWorkspace>();

    /**
     * Creates a {@code PathRepository} with the given repository source name, root node UUID, and a default workspace named
     * {@code ""} (the empty string).
     * 
     * @param sourceName the name of the repository source for use in error and informational messages; may not be null or empty
     * @param rootNodeUuid the UUID that will be used as the root node UUID for each workspace in the repository; may not be null
     *        or empty
     */
    protected PathRepository( String sourceName,
                              UUID rootNodeUuid ) {
        this(sourceName, rootNodeUuid, null);
    }

    /**
     * Creates a {@code PathRepository} with the given repository source name, root node UUID, and a default workspace with the
     * given name.
     * 
     * @param sourceName the name of the repository source for use in error and informational messages; may not be null or empty
     * @param rootNodeUuid the UUID that will be used as the root node UUID for each workspace in the repository; may not be null
     *        or empty
     * @param defaultWorkspaceName the name of the default, auto-created workspace
     */
    protected PathRepository( String sourceName,
                              UUID rootNodeUuid,
                              String defaultWorkspaceName ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(rootNodeUuid, "rootNodeUUID");
        this.rootNodeUuid = rootNodeUuid;
        this.sourceName = sourceName;
        this.defaultWorkspaceName = defaultWorkspaceName != null ? defaultWorkspaceName : "";
    }

    /**
     * Returns the UUID used by the root nodes in each workspace.
     * <p>
     * Note that the root nodes themselves are distinct objects in each workspace and a change to the root node of one workspace
     * does not imply a change to the root nodes of any other workspaces. However, the JCR specification mandates that all
     * referenceable root nodes in a repository use a common UUID (in support of node correspondence); therefore this must be
     * supported by DNA.
     * </p>
     * 
     * @return the root node UUID
     */
    public final UUID getRootNodeUuid() {
        return rootNodeUuid;
    }

    /**
     * Returns the logical name (as opposed to the class name) of the repository source that defined this instance of the
     * repository for use in error, informational, and other contextual messages.
     * 
     * @return sourceName the logical name for the repository source name
     */
    public String getSourceName() {
        return sourceName;
    }

    protected String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }

    /**
     * Returns a list of the names of the currently created workspaces
     * 
     * @return a list of the names of the currently created workspaces
     */
    public Set<String> getWorkspaceNames() {
        return workspaces.keySet();
    }

    /**
     * Returns the workspace with the given name
     * 
     * @param name the name of the workspace to return
     * @return the workspace with the given name; may be null if no workspace with the given name exists
     */
    public PathWorkspace getWorkspace( String name ) {
        if (name == null) name = defaultWorkspaceName;
        return workspaces.get(name);
    }

    /**
     * Initializes the repository by creating the default workspace.
     * <p>
     * Due to the ordering restrictions on constructor chaining, this method cannot be called until the repository is fully
     * initialized. <b>This method MUST be called at the end of the constructor by any class that implements {@code MapRepository}
     * .</b>
     */
    protected abstract void initialize();

}
