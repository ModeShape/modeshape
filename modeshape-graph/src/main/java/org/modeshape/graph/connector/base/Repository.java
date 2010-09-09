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
package org.modeshape.graph.connector.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A representation of a repository as a set of named workspaces. Workspaces can be
 * {@link #createWorkspace(Transaction, String, CreateConflictBehavior, String) created} or {@link #destroyWorkspace(String)
 * destroyed}, though the exact type of {@link Workspace} is dictated by the {@link Transaction}. All workspaces contain a root
 * node with the same {@link #getRootNodeUuid() UUID}.
 * <p>
 * Note that this class is thread-safe, since a {@link BaseRepositorySource} will contain a single instance of a concrete subclass
 * of this class. Often, the Workspace objects are used to hold onto the workspace-related content, but access to the content is
 * always done through a {@link #startTransaction(ExecutionContext, boolean) transaction}.
 * </p>
 * 
 * @param <NodeType> the node type
 * @param <WorkspaceType> the workspace type
 */
@ThreadSafe
public abstract class Repository<NodeType extends Node, WorkspaceType extends Workspace> {

    protected final BaseRepositorySource source;
    protected final UUID rootNodeUuid;
    protected final ExecutionContext context;
    private final String sourceName;
    private final String defaultWorkspaceName;
    private final Map<String, WorkspaceType> workspaces = new HashMap<String, WorkspaceType>();
    private final ReadWriteLock workspacesLock = new ReentrantReadWriteLock();

    /**
     * Creates a {@code MapRepository} with the given repository source name, root node UUID, and a default workspace with the
     * given name.
     * 
     * @param source the repository source to which this repository belongs
     * @throws IllegalArgumentException if the repository source is null, if the source's
     *         {@link BaseRepositorySource#getRepositoryContext()} is null, or if the source name is null or empty
     */
    protected Repository( BaseRepositorySource source ) {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotEmpty(source.getName(), "source.getName()");
        CheckArg.isNotNull(source.getRepositoryContext(), "source.getRepositoryContext()");
        CheckArg.isNotNull(source.getRepositoryContext().getExecutionContext(),
                           "source.getRepositoryContext().getExecutionContext()");
        CheckArg.isNotNull(source.getRootNodeUuidObject(), "source.getRootNodeUuid()");
        this.source = source;
        this.context = source.getRepositoryContext().getExecutionContext();
        this.rootNodeUuid = source.getRootNodeUuidObject();
        this.sourceName = source.getName();
        this.defaultWorkspaceName = source.getDefaultWorkspaceName() != null ? source.getDefaultWorkspaceName() : "";
    }

    /**
     * Initializes the repository by creating the default workspace.
     * <p>
     * Due to the ordering restrictions on constructor chaining, this method cannot be called until the repository is fully
     * initialized. <b>This method MUST be called at the end of the constructor by any class that implements {@code MapRepository}
     * .</b>
     */
    protected void initialize() {
        Transaction<NodeType, WorkspaceType> txn = startTransaction(context, false);
        try {
            // Create the default workspace ...
            workspaces.put(this.defaultWorkspaceName, createWorkspace(txn,
                                                                      this.defaultWorkspaceName,
                                                                      CreateConflictBehavior.DO_NOT_CREATE,
                                                                      null));
        } finally {
            txn.commit();
        }

    }

    public ExecutionContext getContext() {
        return context;
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
     * Get the names of the available workspaces that have been loaded.
     * 
     * @return the immutable names of the workspaces.
     */
    public Set<String> getWorkspaceNames() {
        try {
            workspacesLock.readLock().lock();
            return Collections.unmodifiableSet(new HashSet<String>(workspaces.keySet()));
        } finally {
            workspacesLock.readLock().unlock();
        }
    }

    /**
     * Returns the workspace with the given name.
     * 
     * @param txn the transaction attempting to get the workspace, and which may be used to create the workspace object if needed;
     *        may not be null
     * @param name the name of the workspace to return
     * @return the workspace with the given name; may be null if no workspace with the given name exists
     */
    public WorkspaceType getWorkspace( Transaction<NodeType, WorkspaceType> txn,
                                       String name ) {
        if (name == null) name = defaultWorkspaceName;
        Lock lock = workspacesLock.readLock();
        try {
            lock.lock();
            WorkspaceType workspace = workspaces.get(name);
            if (workspace == null && getWorkspaceNames().contains(name)) {
                workspace = txn.getWorkspace(name, null);
                workspaces.put(name, workspace);
            }
            return workspace;
        } finally {
            lock.unlock();
        }
    }

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
     * </p>
     * <p>
     * If {@code nameOfWorkspaceToClone} is given, this method will clone the content in this original workspace into the new
     * workspace. However, if no workspace with the name {@code nameOfWorkspaceToClone} exists, the method will create an empty
     * workspace.
     * </p>
     * 
     * @param txn the transaction creating the workspace; may not be null
     * @param name the requested name of the workspace. The name of the workspace that is returned from this method may not be the
     *        same as the requested name; may not be null
     * @param existingWorkspaceBehavior the behavior to use in case a workspace with the requested name already exists in the
     *        repository
     * @param nameOfWorkspaceToClone the name of the workspace from which the content should be cloned; be null if the new
     *        workspace is to be empty (other than the root node)
     * @return the newly created workspace with an exact copy of the contents from the workspace named {@code
     *         nameOfWorkspaceToClone} or {@code null} if a workspace with the requested name already exists in the repository and
     *         {@code behavior == CreateConflictBehavior#DO_NOT_CREATE}.
     * @throws InvalidWorkspaceException if the workspace could not be created
     */
    public WorkspaceType createWorkspace( Transaction<NodeType, WorkspaceType> txn,
                                          String name,
                                          CreateConflictBehavior existingWorkspaceBehavior,
                                          String nameOfWorkspaceToClone ) throws InvalidWorkspaceException {
        String newName = name;
        Lock lock = workspacesLock.writeLock();
        try {
            lock.lock();
            boolean conflictingName = workspaces.containsKey(newName);
            if (conflictingName) {
                switch (existingWorkspaceBehavior) {
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

            WorkspaceType original = nameOfWorkspaceToClone != null ? getWorkspace(txn, nameOfWorkspaceToClone) : null;
            WorkspaceType workspace = txn.getWorkspace(name, original);
            workspaces.put(name, workspace);
            return workspace;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the named workspace from the {@code #workspaces workspaces map}.
     * 
     * @param name the name of the workspace to remove
     * @return {@code true} if a workspace with that name previously existed in the map
     */
    public boolean destroyWorkspace( String name ) {
        try {
            workspacesLock.writeLock().lock();
            return workspaces.remove(name) != null;
        } finally {
            workspacesLock.writeLock().unlock();
        }
    }

    /**
     * Create a RequestProcessor instance that should be used to process a set of requests within the supplied transaction.
     * 
     * @param txn the transaction; may not be null
     * @return the request processor
     */
    public RequestProcessor createRequestProcessor( Transaction<NodeType, WorkspaceType> txn ) {
        RepositoryContext repositoryContext = this.source.getRepositoryContext();
        Observer observer = repositoryContext != null ? repositoryContext.getObserver() : null;
        return new Processor<NodeType, WorkspaceType>(txn, this, observer, source.areUpdatesAllowed());
    }

    /**
     * Begin a transaction, hinting whether the transaction will be used only to read the content. If this is called, then the
     * transaction must be either {@link Transaction#commit() committed} or {@link Transaction#rollback() rolled back}.
     * 
     * @param context the context in which the transaction is to be performed; may not be null
     * @param readonly true if the transaction will not modify any content, or false if changes are to be made
     * @return the transaction; never null
     * @see Transaction#commit()
     * @see Transaction#rollback()
     */
    public abstract Transaction<NodeType, WorkspaceType> startTransaction( ExecutionContext context,
                                                                           boolean readonly );
}
