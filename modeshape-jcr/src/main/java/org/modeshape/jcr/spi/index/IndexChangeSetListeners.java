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

package org.modeshape.jcr.spi.index;

import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.AbstractNodeChange;
import org.modeshape.jcr.cache.change.AbstractPropertyChange;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.cache.change.PropertyAdded;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.cache.change.PropertyRemoved;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Property;

/**
 * A set of utilities that create {@link ChangeSetListener} implementations for updating indexes. The listener implementations do
 * manage much of the complexity of processing {@link ChangeSet}s and determining which changes are relevant for an index. All of
 * the logic that defines how the indexes are updated is delegated the the {@link SingleColumnIndexOperations} and
 * {@link MultiColumnIndexOperations} implementations provided by the caller.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class IndexChangeSetListeners {

    /**
     * Create a new ChangeSetListener implementation that can update single-column indexes. The listener will correctly determine
     * when the indexed property has been added, changed or removed, and will call the supplied functions appropriately.
     * 
     * @param context the execution context that should be used by the listener; may not be null
     * @param nodeTypesSupplier the supplier for the immutable {@link NodeTypes} snapshot; may not be null
     * @param indexDefinition the definition of the single column index; may not be null and must have a
     *        {@link IndexDefinition#hasSingleColumn() single column}
     * @param operations the operations that will be called to update the index; may not be null
     * @return the listener; never null
     */
    public static ChangeSetListener create( ExecutionContext context,
                                            NodeTypes.Supplier nodeTypesSupplier,
                                            IndexDefinition indexDefinition,
                                            SingleColumnIndexOperations operations ) {
        assert indexDefinition.hasSingleColumn();
        String processKey = context.getProcessId();
        NameFactory names = context.getValueFactories().getNameFactory();
        Name nodeTypeName = names.create(indexDefinition.getNodeTypeName());
        Name propName = names.create(indexDefinition.getColumnDefinition(0).getPropertyName());
        return new SinglePropertyIndexChangeSetListener(processKey, nodeTypesSupplier, nodeTypeName, propName, operations);
    }

    /**
     * Create a new ChangeSetListener implementation that can update multi-column indexes. The listener will correctly determine
     * when
     * 
     * @param context the execution context that should be used by the listener; may not be null
     * @param nodeTypesSupplier the supplier for the immutable {@link NodeTypes} snapshot; may not be null
     * @param indexDefinition the definition of the single column index; may not be null and must have a
     *        {@link IndexDefinition#hasSingleColumn() single column}
     * @param operations the operations that will be called to update the index; may not be null
     * @return the listener; never null
     */
    public static ChangeSetListener create( ExecutionContext context,
                                            NodeTypes.Supplier nodeTypesSupplier,
                                            IndexDefinition indexDefinition,
                                            MultiColumnIndexOperations operations ) {
        assert indexDefinition.hasSingleColumn();
        String processKey = context.getProcessId();
        NameFactory names = context.getValueFactories().getNameFactory();
        Name nodeTypeName = names.create(indexDefinition.getNodeTypeName());
        Name[] propNames = new Name[indexDefinition.size()];
        for (int i = 0; i != indexDefinition.size(); ++i) {
            propNames[i] = names.create(indexDefinition.getColumnDefinition(i).getPropertyName());
        }
        return new MultiPropertyIndexChangeSetListener(processKey, nodeTypesSupplier, nodeTypeName, propNames, operations);
    }

    /**
     * A predicate function that determines whether the changes in a specified workspace should be processed. The operations will
     * always be called in the following order:
     * <ol>
     * <li>{@link #start(String, boolean)} for each of the set of changes; if this method returns {@code false}, then no other
     * methods will be called for that set of changes.
     * <li>Zero or more calls to {@link #add(NodeKey, Property)}, {@link #change(NodeKey, Property, Property)}, and
     * {@link #remove(NodeKey)} that reflect each of the changes in the set of changes for the workspace identified in
     * {@link #start(String, boolean)}</li>
     * <li>{@link #end()} to signal that all of the changes in the change set have been processed.</li>
     * </ol>
     * It is possible for {@link #start(String, boolean)} to be called and return {@code true}, but for {@link #end()} to be
     * called immediately after that. In such cases, no indexed properties were changed in the set of changes.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @NotThreadSafe
    public static interface SingleColumnIndexOperations {
        /**
         * Signal the beginning of a set of changes for a given workspace. The response dicates whether the changes should be
         * processed or skipped entirely. Only if this method returns true might {@link #add(NodeKey, Property)},
         * {@link #change(NodeKey, Property, Property)}, and {@link #remove(NodeKey)} be called; {@link #end()} will always be
         * called if this method returns true.
         * 
         * @param workspaceName the name of the workspace; never null
         * @param local true if the changes originated from this same repository instance, or false if they originated on a
         *        different repository instance
         * @return true if the set of changes should be processed, or false if they should be ignored
         */
        boolean start( String workspaceName,
                       boolean local );

        /**
         * Record the addition of the node key and property value pair to the index for the given workspace.
         * 
         * @param nodeKey the key for the node to which the property was added; never null
         * @param newProperty the newly-added property (which may be multi-valued); never null
         */
        void add( NodeKey nodeKey,
                  Property newProperty );

        /**
         * Record the change in the indexed property for the given node in the given workspace.
         * 
         * @param nodeKey the key for the node on which the property was changed; never null
         * @param newProperty the new property (which may be multi-valued); never null
         * @param oldProperty the old property (which may be multi-valued); never null
         */
        void change( NodeKey nodeKey,
                     Property newProperty,
                     Property oldProperty );

        /**
         * Record the removal of the node or the indexed property.
         * 
         * @param nodeKey the key for the node to which the property was added; never null
         */
        void remove( NodeKey nodeKey );

        /**
         * Signal the end of the set of changes for the workspace identified in the previous {@link #start(String, boolean)} call
         * that returned true.
         */
        void end();
    }

    /**
     * A predicate function that determines whether the changes in a specified workspace should be processed. The operations will
     * always be called in the following order:
     * <ol>
     * <li>{@link #start(String, boolean)} for each of the set of changes; if this method returns {@code false}, then no other
     * methods will be called for that set of changes.
     * <li>Zero or more calls to {@link #add(NodeKey, Property[])}, {@link #change(NodeKey, Property[], Property[])}, and
     * {@link #remove(NodeKey)} that reflect each of the changes in the set of changes for the workspace identified in
     * {@link #start(String, boolean)}</li>
     * <li>{@link #end()} to signal that all of the changes in the change set have been processed.</li>
     * </ol>
     * It is possible for {@link #start(String, boolean)} to be called and return {@code true}, but for {@link #end()} to be
     * called immediately after that. In such cases, no indexed properties were changed in the set of changes.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @NotThreadSafe
    public static interface MultiColumnIndexOperations {
        /**
         * Signal the beginning of a set of changes for a given workspace. The response dicates whether the changes should be
         * processed or skipped entirely. Only if this method returns true might {@link #add(NodeKey, Property[])},
         * {@link #change(NodeKey, Property[], Property[])}, and {@link #remove(NodeKey)} be called; {@link #end()} will always be
         * called if this method returns true.
         * 
         * @param workspaceName the name of the workspace; never null
         * @param local true if the changes originated from this same repository instance, or false if they originated on a
         *        different repository instance
         * @return true if the set of changes should be processed, or false if they should be ignored
         */
        boolean start( String workspaceName,
                       boolean local );

        /**
         * Record the addition of the indexed properties on a node. It is possible that only some of the indexed properties are
         * provided, and in such cases this means that only the provided properties were added and the missing properties are not
         * yet set on the given node.
         * 
         * @param nodeKey the key for the node to which the property was added; never null
         * @param newProperties the newly-added properties (which may be multi-valued), in the same order as defined by the
         *        IndexDefinition; never null
         */
        void add( NodeKey nodeKey,
                  Property[] newProperties );

        /**
         * Record the change in the indexed properties for the given node. Only those properties that are altered will be
         * provided.
         * <p>
         * For example, if an index tracks two properties, A and B, then this method might be called with the new and old A if
         * only A is changed.
         * </p>
         * 
         * @param nodeKey the key for the node on which the property was changed; never null
         * @param newProperties the new properties (each of which may be multi-valued), in the same order as defined by the
         *        IndexDefinition; never null
         * @param oldProperties the old properties (each of which may be multi-valued), in the same order as defined by the
         *        IndexDefinition; never null
         */
        void change( NodeKey nodeKey,
                     Property[] newProperties,
                     Property[] oldProperties );

        /**
         * Record the removal of the node or the indexed properties.
         * 
         * @param nodeKey the key for the node to which the property was added; never null
         */
        void remove( NodeKey nodeKey );

        /**
         * Signal the end of the set of changes for the workspace identified in the previous {@link #start(String, boolean)} call
         * that returned true.
         */
        void end();
    }

    private IndexChangeSetListeners() {
    }

    protected static interface NodeTypePredicate {
        boolean accept( AbstractNodeChange change );
    }

    @NotThreadSafe
    protected static abstract class IndexChangeSetListener implements ChangeSetListener {

        private final String localProcessKey;
        protected final NodeTypes.Supplier nodeTypesSupplier;
        private final Name nodeType;

        protected IndexChangeSetListener( String localProcessKey,
                                          NodeTypes.Supplier nodeTypesSupplier,
                                          Name nodeType ) {
            this.localProcessKey = localProcessKey;
            this.nodeTypesSupplier = nodeTypesSupplier;
            this.nodeType = nodeType;
        }

        protected final boolean isLocal( ChangeSet changeSet ) {
            return localProcessKey.equals(changeSet.getProcessKey());
        }

        protected final boolean acceptableNodeType( AbstractNodeChange change ) {
            return change.isType(nodeType, nodeTypesSupplier.getNodeTypes());
        }
    }

    @NotThreadSafe
    protected static class SinglePropertyIndexChangeSetListener extends IndexChangeSetListener {

        private final Name indexedPropertyName;
        private final SingleColumnIndexOperations ops;

        public SinglePropertyIndexChangeSetListener( String localProcessKey,
                                                     NodeTypes.Supplier nodeTypesSupplier,
                                                     Name nodeType,
                                                     Name indexedPropertyName,
                                                     SingleColumnIndexOperations ops ) {
            super(localProcessKey, nodeTypesSupplier, nodeType);
            this.indexedPropertyName = indexedPropertyName;
            this.ops = ops;
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (ops.start(changeSet.getWorkspaceName(), isLocal(changeSet))) {
                try {
                    for (Change change : changeSet) {
                        if (change instanceof AbstractNodeChange) {
                            AbstractNodeChange nodeChange = (AbstractNodeChange)change;
                            if (acceptableNodeType(nodeChange)) {
                                if (nodeChange instanceof AbstractPropertyChange) {
                                    AbstractPropertyChange propChange = (AbstractPropertyChange)nodeChange;
                                    Property property = propChange.getProperty();
                                    if (indexedPropertyName.equals(property.getName())) {
                                        if (nodeChange instanceof PropertyAdded) {
                                            ops.add(propChange.getKey(), property);
                                        } else if (nodeChange instanceof PropertyChanged) {
                                            PropertyChanged changedProperty = (PropertyChanged)nodeChange;
                                            ops.change(propChange.getKey(), property, changedProperty.getOldProperty());
                                        } else if (nodeChange instanceof PropertyRemoved) {
                                            ops.remove(propChange.getKey());
                                        }
                                    }
                                } else if (nodeChange instanceof NodeAdded) {
                                    NodeAdded added = (NodeAdded)nodeChange;
                                    Property addedProperty = added.getProperties().get(indexedPropertyName);
                                    if (addedProperty != null) {
                                        ops.add(nodeChange.getKey(), addedProperty);
                                    }
                                } else if (nodeChange instanceof NodeRemoved) {
                                    ops.remove(nodeChange.getKey());
                                }
                            }
                        }
                    }
                } finally {
                    ops.end();
                }
            }
        }
    }

    @NotThreadSafe
    protected static class MultiPropertyIndexChangeSetListener extends IndexChangeSetListener {

        private final Name[] indexedPropertyNames;
        private final MultiColumnIndexOperations ops;
        private final int numberOfProperties;
        private NodeKey nodeKey;
        private boolean hasNewProps = false;
        private boolean hasOldProps = false;
        private final Property[] newProps;
        private final Property[] oldProps;

        public MultiPropertyIndexChangeSetListener( String localProcessKey,
                                                    NodeTypes.Supplier nodeTypesSupplier,
                                                    Name nodeType,
                                                    Name[] indexedPropertyNames,
                                                    MultiColumnIndexOperations ops ) {
            super(localProcessKey, nodeTypesSupplier, nodeType);
            this.indexedPropertyNames = indexedPropertyNames;
            this.numberOfProperties = indexedPropertyNames.length;
            this.ops = ops;
            this.newProps = new Property[numberOfProperties];
            this.oldProps = new Property[numberOfProperties];
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (ops.start(changeSet.getWorkspaceName(), isLocal(changeSet))) {
                // All Change objects for a given node should be adjacent to each other, so we use this characteristic to find all
                // changes associated with a single node...
                NodeKey nodeKey = null;
                for (Change change : changeSet) {
                    if (!(change instanceof AbstractNodeChange)) {
                        // This is not a change we care about, and in fact it does tell us that the next change (if any) will be
                        // for a different node than we've seen already ...
                        continue;
                    }
                    AbstractNodeChange nodeChange = (AbstractNodeChange)change;
                    if (!acceptableNodeType(nodeChange)) {
                        // This is not a change we care about, and in fact it does tell us that the next change (if any) will be
                        // for a different node than we've seen already ...
                        continue;
                    }
                    // This is a node that we're interested in, and it might be the same node that the last change was for ...
                    if (nodeKey == null) {
                        // This is the first node we've seen, so there's nothing to do yet ...
                        nodeKey = nodeChange.getKey();
                    } else if (!nodeKey.equals(nodeChange.getKey())) {
                        // This is a different node than we've seen previously in this ChangeSet, so process the previous events
                        // ...
                        processProperties();
                        nodeKey = nodeChange.getKey();
                    }
                    // We know we care about this node type, so figure out if there are any properties we care about ...
                    if (nodeChange instanceof AbstractPropertyChange) {
                        AbstractPropertyChange propChange = (AbstractPropertyChange)nodeChange;
                        Property property = propChange.getProperty();
                        for (int i = 0; i != numberOfProperties; ++i) {
                            if (indexedPropertyNames[i].equals(property.getName())) {
                                if (nodeChange instanceof PropertyAdded) {
                                    newProps[i] = property;
                                    hasNewProps = true;
                                } else if (nodeChange instanceof PropertyChanged) {
                                    PropertyChanged changedProperty = (PropertyChanged)nodeChange;
                                    newProps[i] = property;
                                    oldProps[i] = changedProperty.getOldProperty();
                                    hasNewProps = true;
                                    hasOldProps = true;
                                } else if (nodeChange instanceof PropertyRemoved) {
                                    oldProps[i] = property;
                                    hasOldProps = true;
                                }
                                // We found the location of this property, so no need to search more ...
                                break;
                            }
                        }
                    } else if (nodeChange instanceof NodeAdded) {
                        NodeAdded added = (NodeAdded)nodeChange;
                        for (int i = 0; i != numberOfProperties; ++i) {
                            newProps[i] = added.getProperties().get(indexedPropertyNames[i]);
                            oldProps[i] = null;
                        }
                        // Record the addition ...
                        ops.add(nodeKey, newProps);
                        // And we know that we won't see other changes for this node (or if we do we don't care about them) ...
                        change = null;
                        nodeKey = null;
                        clearPropertyBuffer();
                        continue;
                    } else if (nodeChange instanceof NodeRemoved) {
                        // Record the removal ...
                        ops.remove(nodeKey);
                        // And we know that we won't see other changes for this node (or if we do we don't care about them) ...
                        clearPropertyBuffer();
                        continue;
                    }
                }
                // Process any properties that were left unprocessed after the last change ...
                processProperties();
            }
        }

        private void processProperties() {
            if (hasNewProps) {
                if (hasOldProps) {
                    // There is at least one change ...
                    ops.change(nodeKey, newProps, oldProps);
                } else {
                    // There are only new properties, so this is an add ...
                    ops.add(nodeKey, newProps);
                }
                clearPropertyBuffer();
            } else if (hasOldProps) {
                // There are only old properties, so this is a remove ...
                ops.remove(nodeKey);
                clearPropertyBuffer();
            } else {
                // Otherwise, there were no new or old properties (no changes), so nothing to do ...
            }
        }

        private void clearPropertyBuffer() {
            for (int i = 0; i != numberOfProperties; ++i) {
                newProps[i] = null;
                oldProps[i] = null;
            }
            hasNewProps = false;
            hasOldProps = false;
        }
    }
}
