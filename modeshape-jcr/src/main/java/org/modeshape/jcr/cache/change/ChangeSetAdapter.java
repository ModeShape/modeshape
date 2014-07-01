/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.cache.change;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * An adapter class that processes {@link ChangeSet} instances and for each {@link Change} calls the appropriate protected method
 * that, by default, do nothing. Implementations simply override at least one of the relevant methods for the kind of changes they
 * expect.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class ChangeSetAdapter implements ChangeSetListener {

    protected final ExecutionContext context;

    /**
     * @param context the execution context in which this adapter is supposed to run; may not be null
     */
    public ChangeSetAdapter( ExecutionContext context ) {
        assert context != null;
        this.context = context;
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        final String workspaceName = changeSet.getWorkspaceName();
        if (workspaceName != null) {
            if (includesWorkspace(workspaceName)) {
                try {
                    beginWorkspaceChanges();
                    final Map<Name, AbstractPropertyChange> propChanges = new HashMap<>();
                    NodeKey lastKey = null;
                    for (Change change : changeSet) {
                        if (change instanceof AbstractNodeChange) {
                            if (change instanceof NodeAdded) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeAdded added = (NodeAdded)change;
                                addNode(workspaceName, added.getKey(), added.getPath(), added.getPrimaryType(),
                                        added.getMixinTypes(), props(added.getProperties()), added.isQueryable());
                            } else if (change instanceof NodeRemoved) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeRemoved removed = (NodeRemoved)change;
                                removeNode(workspaceName, removed.getKey(), removed.getParentKey(), removed.getPath(),
                                           removed.getPrimaryType(), removed.getMixinTypes(), removed.isQueryable());
                            } else if (change instanceof AbstractPropertyChange) {
                                AbstractPropertyChange propChange = (AbstractPropertyChange)change;
                                if (propChange.getKey().equals(lastKey)) firePropertyChanges(lastKey, propChanges);
                                propChanges.put(propChange.getProperty().getName(), propChange);
                            } else if (change instanceof NodeChanged) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeChanged nodeChanged = (NodeChanged)change;
                                changeNode(workspaceName, nodeChanged.getKey(), nodeChanged.getPath(),
                                           nodeChanged.getPrimaryType(), nodeChanged.getMixinTypes(), nodeChanged.isQueryable());
                            } else if (change instanceof NodeMoved) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeMoved moved = (NodeMoved)change;
                                moveNode(workspaceName, moved.getKey(), moved.getPrimaryType(), moved.getMixinTypes(),
                                         moved.getNewParent(), moved.getOldParent(), moved.getNewPath(), moved.getOldPath(),
                                         moved.isQueryable());
                            } else if (change instanceof NodeRenamed) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeRenamed renamed = (NodeRenamed)change;
                                renameNode(workspaceName, renamed.getKey(), renamed.getPath(), renamed.getOldSegment(),
                                           renamed.getPrimaryType(), renamed.getMixinTypes(), renamed.isQueryable());
                            } else if (change instanceof NodeReordered) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeReordered reordered = (NodeReordered)change;
                                reorderNode(workspaceName, reordered.getKey(), reordered.getPrimaryType(),
                                            reordered.getMixinTypes(), reordered.getParent(), reordered.getPath(),
                                            reordered.getOldPath(), reordered.getReorderedBeforePath(), reordered.isQueryable());
                            } else if (change instanceof NodeSequenced) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeSequenced s = (NodeSequenced)change;
                                sequenced(workspaceName, s.getKey(), s.getPath(), s.getPrimaryType(), s.getMixinTypes(),
                                          s.getOutputNodeKey(), s.getOutputNodePath(), s.getOutputPath(), s.getUserId(),
                                          s.getSelectedPath(), s.getSequencerName(), s.isQueryable());
                            } else if (change instanceof NodeSequencingFailure) {
                                firePropertyChanges(lastKey, propChanges);
                                NodeSequencingFailure f = (NodeSequencingFailure)change;
                                sequenceFailure(workspaceName, f.getKey(), f.getPath(), f.getPrimaryType(), f.getMixinTypes(),
                                                f.getOutputPath(), f.getUserId(), f.getSelectedPath(), f.getSequencerName(),
                                                f.isQueryable(), f.getCause());
                            }
                            lastKey = ((AbstractNodeChange)change).getKey();
                        }
                    }
                } finally {
                    completeWorkspaceChanges();
                }
            }
        } else {
            for (Change change : changeSet) {
                if (change instanceof WorkspaceAdded) {
                    WorkspaceAdded added = (WorkspaceAdded)change;
                    addWorkspace(added.getWorkspaceName());
                } else if (change instanceof WorkspaceRemoved) {
                    WorkspaceRemoved removed = (WorkspaceRemoved)change;
                    removeWorkspace(removed.getWorkspaceName());
                } else if (change instanceof RepositoryMetadataChanged) {
                    repositoryMetadataChanged();
                } else if (change instanceof BinaryValueUnused) {
                    BinaryValueUnused bvu = (BinaryValueUnused)change;
                    binaryValueUnused(bvu.getKey());
                }
            }
        }
    }

    /**
     * Signals the beginning of processing for the changes in a transaction described by a single {@link ChangeSet}.
     * 
     * @see #completeWorkspaceChanges()
     */
    protected void beginWorkspaceChanges() {
    }

    /**
     * Signals the end of processing for the changes in a transaction described by a single {@link ChangeSet}.
     * 
     * @see #beginWorkspaceChanges()
     */
    protected void completeWorkspaceChanges() {
    }

    private void firePropertyChanges( NodeKey key,
                                      Map<Name, AbstractPropertyChange> propChanges ) {
        if (propChanges.isEmpty()) {
            modifyProperties(key, propChanges);
            propChanges.clear();
        }
    }

    /**
     * Handle the addition, change, and removal of one or more properties of a single node. This method is called once for each
     * existing node whose properties are modified.
     * 
     * @param key the unique key for the node; may not be null
     * @param propChanges the map of property modification events, keyed by the names of the properties; never null and never
     *        empty
     */
    protected void modifyProperties( NodeKey key,
                                     Map<Name, AbstractPropertyChange> propChanges ) {
    }

    private final Properties props( final Map<Name, Property> properties ) {
        return new Properties() {
            @Override
            public org.modeshape.jcr.value.Property getProperty( Name name ) {
                return properties.get(name);
            }

            @Override
            public Iterator<Property> iterator() {
                return properties.values().iterator();
            }
        };
    }

    /**
     * Determine whether changes in the named workspace should be processed. By default this method returns {@code true}, which
     * means changes to content in all workspaces are handled by this adapter.
     * 
     * @param workspaceName the workspace that has changes in content
     * @return true if the changes should be processed, or false otherwise
     */
    protected boolean includesWorkspace( String workspaceName ) {
        return true;
    }

    /**
     * Handle the addition of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param properties the properties of the node; may not be null but may be empty
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void addNode( String workspaceName,
                            NodeKey key,
                            Path path,
                            Name primaryType,
                            Set<Name> mixinTypes,
                            Properties properties,
                            boolean queryable ) {
    }

    /**
     * Handle the removal of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param parentKey the unique key for the parent of the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void removeNode( String workspaceName,
                               NodeKey key,
                               NodeKey parentKey,
                               Path path,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               boolean queryable ) {

    }

    /**
     * Handle the change of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void changeNode( String workspaceName,
                               NodeKey key,
                               Path path,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               boolean queryable ) {

    }

    /**
     * Handle the move of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param oldParent the key of the node's parent before it was moved; may not be null
     * @param newParent the key of the node's parent after it was moved; may not be null
     * @param newPath the new path of the node after it was moved; may not be null
     * @param oldPath the old path of the node before it was moved; may not be null
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void moveNode( String workspaceName,
                             NodeKey key,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             NodeKey oldParent,
                             NodeKey newParent,
                             Path newPath,
                             Path oldPath,
                             boolean queryable ) {

    }

    /**
     * Handle the renaming of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param newPath the new path of the node after it was moved; may not be null
     * @param oldSegment the old segment of the node before it was moved; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void renameNode( String workspaceName,
                               NodeKey key,
                               Path newPath,
                               Segment oldSegment,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               boolean queryable ) {

    }

    /**
     * Handle the reordering of a node.
     * 
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param parent the key of the node's parent; may not be null
     * @param newPath the new path of the node after it was moved; may not be null
     * @param oldPath the old path of the node before it was moved; may not be null
     * @param reorderedBeforePath the path of the node before which the node was placed; null if it was moved to the end
     * @param queryable true if the node is queryable, false otherwise
     */
    protected void reorderNode( String workspaceName,
                                NodeKey key,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                NodeKey parent,
                                Path newPath,
                                Path oldPath,
                                Path reorderedBeforePath,
                                boolean queryable ) {

    }

    protected void sequenced( String workspaceName,
                              NodeKey sequencedNodeKey,
                              Path sequencedNodePath,
                              Name sequencedNodePrimaryType,
                              Set<Name> sequencedNodeMixinTypes,
                              NodeKey outputNodeKey,
                              Path outputNodePath,
                              String outputPath,
                              String userId,
                              String selectedPath,
                              String sequencerName,
                              boolean queryable ) {

    }

    protected void sequenceFailure( String workspaceName,
                                    NodeKey sequencedNodeKey,
                                    Path sequencedNodePath,
                                    Name sequencedNodePrimaryType,
                                    Set<Name> sequencedNodeMixinTypes,
                                    String outputPath,
                                    String userId,
                                    String selectedPath,
                                    String sequencerName,
                                    boolean queryable,
                                    Throwable cause ) {

    }

    /**
     * Handle the changing of the repository metadata information.
     */
    protected void repositoryMetadataChanged() {
    }

    /**
     * Handle the marking of a binary value as being unused.
     * 
     * @param key the binary key of the binary value that is no longer used; may not be null
     */
    protected void binaryValueUnused( BinaryKey key ) {
    }

    /**
     * Handle the addition of a new workspace in the repository.
     * 
     * @param workspaceName the name of the workspace that was added; may not be null
     */
    protected void addWorkspace( String workspaceName ) {
    }

    /**
     * Handle the removal of a workspace in the repository.
     * 
     * @param workspaceName the name of the workspace that was removed; may not be null
     */
    protected void removeWorkspace( String workspaceName ) {
    }

}
