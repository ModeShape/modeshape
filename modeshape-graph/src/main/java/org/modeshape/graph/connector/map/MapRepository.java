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
package org.modeshape.graph.connector.map;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.GuardedBy;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * A default implementation of a map-based repository.
 * <p>
 * {@code MapRepository} provides means for creating, retrieving, and destroying {@link MapWorkspace map-based workspaces},
 * including providing the locking primitives required for safe control of the workspaces.
 * </p>
 */
public abstract class MapRepository {

    protected final UUID rootNodeUuid;
    private final String sourceName;
    private final String defaultWorkspaceName;
    private final Map<String, MapWorkspace> workspaces = new HashMap<String, MapWorkspace>();

    /**
     * Creates a {@code MapRepository} with the given repository source name, root node UUID, and a default workspace named
     * {@code ""} (the empty string).
     * 
     * @param sourceName the name of the repository source for use in error and informational messages; may not be null or empty
     * @param rootNodeUuid the UUID that will be used as the root node UUID for each workspace in the repository; may not be null
     *        or empty
     */
    protected MapRepository( String sourceName,
                             UUID rootNodeUuid ) {
        this(sourceName, rootNodeUuid, null);
    }

    /**
     * Creates a {@code MapRepository} with the given repository source name, root node UUID, and a default workspace with the
     * given name.
     * 
     * @param sourceName the name of the repository source for use in error and informational messages; may not be null or empty
     * @param rootNodeUuid the UUID that will be used as the root node UUID for each workspace in the repository; may not be null
     *        or empty
     * @param defaultWorkspaceName the name of the default, auto-created workspace
     */
    protected MapRepository( String sourceName,
                             UUID rootNodeUuid,
                             String defaultWorkspaceName ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(rootNodeUuid, "rootNodeUUID");
        this.rootNodeUuid = rootNodeUuid;
        this.sourceName = sourceName;
        this.defaultWorkspaceName = defaultWorkspaceName != null ? defaultWorkspaceName : "";
    }

    /**
     * Initializes the repository by creating the default workspace.
     * <p>
     * Due to the ordering restrictions on constructor chaining, this method cannot be called until the repository is fully
     * initialized. <b>This method MUST be called at the end of the constructor by any class that implements {@code MapRepository}
     * .</b>
     */
    protected void initialize() {
        // Create the default workspace ...
        workspaces.put(this.defaultWorkspaceName, createWorkspace(null,
                                                                  this.defaultWorkspaceName,
                                                                  CreateConflictBehavior.DO_NOT_CREATE));

    }

    protected String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }

    /**
     * Returns the UUID used by the root nodes in each workspace.
     * <p>
     * Note that the root nodes themselves are distinct objects in each workspace and a change to the root node of one workspace
     * does not imply a change to the root nodes of any other workspaces. However, the JCR specification mandates that all
     * referenceable root nodes in a repository use a common UUID (in support of node correspondence); therefore this must be
     * supported by ModeShape.
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

    /**
     * Returns a list of the names of the currently created workspaces
     * 
     * @return a list of the names of the currently created workspaces
     */
    @GuardedBy( "getLock()" )
    public Set<String> getWorkspaceNames() {
        return workspaces.keySet();
    }

    /**
     * Returns the workspace with the given name
     * 
     * @param name the name of the workspace to return
     * @return the workspace with the given name; may be null if no workspace with the given name exists
     */
    @GuardedBy( "getLock()" )
    public MapWorkspace getWorkspace( String name ) {
        if (name == null) name = defaultWorkspaceName;
        return workspaces.get(name);
    }

    /**
     * Creates a new workspace with the given name containing only a root node.
     * <p>
     * <b>This method does NOT automatically add the newly created workspace to the {@link #workspaces workspace map} or check to
     * see if a workspace already exists in this repository with the same name.</b>
     * </p>
     * 
     * @param context the context in which the workspace is to be created
     * @param name the name of the workspace
     * @return the newly created workspace; may not be null
     */
    @GuardedBy( "getLock()" )
    protected abstract MapWorkspace createWorkspace( ExecutionContext context,
                                                     String name );

    /**
     * Attempts to create a workspace with the given name with name-collision behavior determined by the behavior parameter.
     * <p>
     * This method will first check to see if a workspace already exists with the given name. If no such workspace exists, the
     * method will create a new workspace with the given name, add it to the {@code #workspaces workspaces map}, and return it. If
     * a workspace with the requested name already exists and the {@code behavior} is {@link CreateConflictBehavior#DO_NOT_CREATE}
     * , this method will return {@code null} without modifying the state of the repository. If a workspace with the requested
     * name already exists and the {@code behavior} is {@link CreateConflictBehavior#CREATE_WITH_ADJUSTED_NAME}, this method will
     * generate a unique new name for the workspace, create a new workspace with the given name, added it to the {@code
     * #workspaces workspaces map}, and return it.
     * 
     * @param context the context in which the workspace is to be created; may not be null
     * @param name the requested name of the workspace. The name of the workspace that is returned from this method may not be the
     *        same as the requested name; may not be null
     * @param behavior the behavior to use in case a workspace with the requested name already exists in the repository
     * @return the newly created workspace or {@code null} if a workspace with the requested name already exists in the repository
     *         and {@code behavior == CreateConflictBehavior#DO_NOT_CREATE}.
     */
    @GuardedBy( "getLock()" )
    public MapWorkspace createWorkspace( ExecutionContext context,
                                         String name,
                                         CreateConflictBehavior behavior ) {
        String newName = name;
        boolean conflictingName = workspaces.containsKey(newName);
        if (conflictingName) {
            switch (behavior) {
                case DO_NOT_CREATE:
                    return null;
                case CREATE_WITH_ADJUSTED_NAME:
                    int counter = 0;
                    do {
                        newName = name + (++counter);
                    } while (workspaces.containsKey(newName));
                    break;
            }
        }
        assert workspaces.containsKey(newName) == false;

        MapWorkspace workspace = createWorkspace(context, name);
        workspaces.put(name, workspace);
        return workspace;
    }

    /**
     * Attempts to create a workspace with the requested name as in the
     * {@link #createWorkspace(ExecutionContext, String, CreateConflictBehavior)} method and then clones the content from the
     * given source workspace into the new workspace if the creation was successful.
     * <p>
     * If no workspace with the name {@code nameOfWorkspaceToClone} exists, the method will return an empty workspace.
     * </p>
     * 
     * @param context the context in which the workspace is to be created; may not be null
     * @param name the requested name of the workspace. The name of the workspace that is returned from this method may not be the
     *        same as the requested name; may not be null
     * @param existingWorkspaceBehavior the behavior to use in case a workspace with the requested name already exists in the
     *        repository
     * @param nameOfWorkspaceToClone the name of the workspace from which the content should be cloned; may not be null
     * @return the newly created workspace with an exact copy of the contents from the workspace named {@code
     *         nameOfWorkspaceToClone} or {@code null} if a workspace with the requested name already exists in the repository and
     *         {@code behavior == CreateConflictBehavior#DO_NOT_CREATE}.
     */
    @GuardedBy( "getLock()" )
    public MapWorkspace createWorkspace( ExecutionContext context,
                                         String name,
                                         CreateConflictBehavior existingWorkspaceBehavior,
                                         String nameOfWorkspaceToClone ) {
        MapWorkspace workspace = createWorkspace(context, name, existingWorkspaceBehavior);
        if (workspace == null) {
            // Unable to create because of a duplicate name ...
            return null;
        }
        MapWorkspace original = getWorkspace(nameOfWorkspaceToClone);
        if (original != null) {
            // Copy the properties of the root node ...
            MapNode root = workspace.getRoot();
            MapNode origRoot = original.getRoot();
            root.getProperties().clear();
            root.getProperties().putAll(origRoot.getProperties());

            // Loop over each child and call this method to copy the immediate children (and below).
            // Note that this makes the copy have the same UUID as the original.
            for (MapNode originalNode : origRoot.getChildren()) {
                original.cloneNode(context, originalNode, workspace, root, originalNode.getName().getName(), null, true, null);
            }
        }

        workspaces.put(name, workspace);
        return workspace;
    }

    /**
     * Removes the named workspace from the {@code #workspaces workspaces map}.
     * 
     * @param name the name of the workspace to remove
     * @return {@code true} if a workspace with that name previously existed in the map
     */
    @GuardedBy( "getLock()" )
    public boolean destroyWorkspace( String name ) {
        return workspaces.remove(name) != null;
    }

    /**
     * Begin a transaction, hinting whether the transaction will be used only to read the content. If this is called, then the
     * transaction must be either {@link MapRepositoryTransaction#commit() committed} or
     * {@link MapRepositoryTransaction#rollback() rolled back}.
     * 
     * @param readonly true if the transaction will not modify any content, or false if changes are to be made
     * @return the transaction; never null
     * @see MapRepositoryTransaction#commit()
     * @see MapRepositoryTransaction#rollback()
     */
    public abstract MapRepositoryTransaction startTransaction( boolean readonly );
}
