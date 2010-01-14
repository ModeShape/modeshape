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
package org.modeshape.graph.connector.path;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * Extension of {@link PathRepository} for repositories that support modification of nodes as well as access to the nodes.
 */
@ThreadSafe
public abstract class WritablePathRepository extends PathRepository {

    public WritablePathRepository( PathRepositorySource source ) {
        super(source);
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
    protected abstract WritablePathWorkspace createWorkspace( ExecutionContext context,
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
    public WritablePathWorkspace createWorkspace( ExecutionContext context,
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

        WritablePathWorkspace workspace = createWorkspace(context, name);
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
    public WritablePathWorkspace createWorkspace( ExecutionContext context,
                                                  String name,
                                                  CreateConflictBehavior existingWorkspaceBehavior,
                                                  String nameOfWorkspaceToClone ) {
        WritablePathWorkspace workspace = createWorkspace(context, name, existingWorkspaceBehavior);
        if (workspace == null) {
            // Unable to create because of a duplicate name ...
            return null;
        }
        PathWorkspace original = getWorkspace(nameOfWorkspaceToClone);

        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path rootPath = pathFactory.createRootPath();

        if (original != null) {
            // Copy the properties of the root node ...
            PathNode root = workspace.getNode(rootPath);
            PathNode origRoot = original.getNode(rootPath);
            workspace.removeProperties(context, rootPath, root.getProperties().keySet());
            workspace.setProperties(context, rootPath, origRoot.getProperties());

            // Loop over each child and call this method to copy the immediate children (and below).
            for (Segment childSegment : origRoot.getChildSegments()) {
                Path childPath = pathFactory.create(origRoot.getPath(), childSegment);
                PathNode originalNode = original.getNode(childPath);

                workspace.copyNode(context, originalNode, original, root, childSegment.getName(), true);
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
    public boolean destroyWorkspace( String name ) {
        return workspaces.remove(name) != null;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

}
