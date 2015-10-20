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

package org.modeshape.jcr.spi.index.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.AbstractPropertyChange;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.cache.change.PropertyAdded;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.cache.change.PropertyRemoved;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * Utility for creating generic {@link IndexChangeAdapter} instances, which support both single and multi-valued properties.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
public class IndexChangeAdapters {

    /**
     * Creates a composite change adapter which handles the case when an index has multiple columns.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the index that should be used; may not be null
     * @param adapters an {@link Iterable} of existing "discrete" adapters.
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forMultipleColumns( ExecutionContext context,
                                                         NodeTypePredicate matcher,
                                                         String workspaceName,
                                                         ProvidedIndex<?> index,
                                                         Iterable<IndexChangeAdapter> adapters ) {
        return new MultiColumnChangeAdapter(context ,workspaceName, matcher, index, adapters);        
    } 

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "mode:nodeDepth" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeDepth( ExecutionContext context,
                                                   NodeTypePredicate matcher,
                                                   String workspaceName,
                                                   ProvidedIndex<?> index ) {
        return new NodeDepthChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:name" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeName( ExecutionContext context,
                                                  NodeTypePredicate matcher,
                                                  String workspaceName,
                                                  ProvidedIndex<?> index ) {
        return new NodeNameChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "mode:localName" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeLocalName( ExecutionContext context,
                                                       NodeTypePredicate matcher,
                                                       String workspaceName,
                                                       ProvidedIndex<?> index ) {
        return new NodeLocalNameChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:path" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodePath( ExecutionContext context,
                                                  NodeTypePredicate matcher,
                                                  String workspaceName,
                                                  ProvidedIndex<?>  index ) {
        return new NodePathChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:primaryType" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forPrimaryType( ExecutionContext context,
                                                     NodeTypePredicate matcher,
                                                     String workspaceName,
                                                     ProvidedIndex<?> index ) {
        return new PrimaryTypeChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:mixinTypes" property.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forMixinTypes( ExecutionContext context,
                                                    NodeTypePredicate matcher,
                                                    String workspaceName,
                                                    ProvidedIndex<?> index ) {
        return new MixinTypesChangeAdapter(context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a node property, either single or multi-valued.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static <T> IndexChangeAdapter forProperty( ExecutionContext context,
                                                      NodeTypePredicate matcher,
                                                      String workspaceName,
                                                      Name propertyName,
                                                      ValueFactory<T> factory,
                                                      ProvidedIndex<?> index ) {
        return new PropertyChangeAdapter<>(context, matcher, workspaceName, propertyName, factory, index);
    }
    
    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a unique-valued property, where every property value is
     * unique across all nodes.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static <T> IndexChangeAdapter forUniqueValuedProperty( ExecutionContext context,
                                                                  NodeTypePredicate matcher,
                                                                  String workspaceName,
                                                                  Name propertyName,
                                                                  ValueFactory<T> factory,
                                                                  ProvidedIndex<?> index ) {
        return new UniquePropertyChangeAdapter<>(context, matcher, workspaceName, propertyName, factory, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a enumerated properties, either single or multi-valued.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forEnumeratedProperty( ExecutionContext context,
                                                            NodeTypePredicate matcher,
                                                            String workspaceName,
                                                            Name propertyName,
                                                            ProvidedIndex<?> index ) {
        return new EnumeratedPropertyChangeAdapter(context, matcher, workspaceName, propertyName, index);
    }
    
    /**
     * Create an {@link IndexChangeAdapter} implementation that handles node type information.
     *
     * @param propertyName a symbolic name of the property that will be sent to the {@link ProvidedIndex} when the adapter
     * notices that there are either primary type of mixin type changes.
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeTypes( String propertyName, 
                                                   ExecutionContext context,
                                                   NodeTypePredicate matcher,
                                                   String workspaceName,
                                                   ProvidedIndex<?> index ) {
        return new NodeTypesChangeAdapter(propertyName, context, matcher, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles full text information.
     *
     * @param context the execution context; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forTextProperty( ExecutionContext context,
                                                      NodeTypePredicate matcher,
                                                      String workspaceName,
                                                      Name propertyName,
                                                      ValueFactory<String> factory,
                                                      ProvidedIndex<?> index ) {
        return new TextPropertyChangeAdapter(context, matcher, workspaceName, propertyName, factory,index);
    }

    private IndexChangeAdapters() {
    }
    
    protected static class MultiColumnChangeAdapter extends IndexChangeAdapter {
        private List<PathBasedChangeAdapter<?>> pathAdapters;
        private List<AbstractPropertyChangeAdapter<?>> propertyAdapters;

        protected MultiColumnChangeAdapter( ExecutionContext context,
                                            String workspaceName,
                                            NodeTypePredicate predicate,
                                            ProvidedIndex<?> index,
                                            Iterable<IndexChangeAdapter> adapters ) {
            super(context, workspaceName, predicate, index);

            pathAdapters = new ArrayList<>();
            propertyAdapters = new ArrayList<>();

            for (IndexChangeAdapter adapter : adapters) {
                if (adapter instanceof PathBasedChangeAdapter) {
                    pathAdapters.add((PathBasedChangeAdapter<?>) adapter);
                } else if (adapter instanceof  AbstractPropertyChangeAdapter) {
                    propertyAdapters.add((AbstractPropertyChangeAdapter<?>) adapter);
                }
            }
            
            assert !pathAdapters.isEmpty() || !propertyAdapters.isEmpty();
        }

        @Override
        protected void modifyProperties( NodeKey key, Name primaryType, Set<Name> mixinTypes,
                                         Map<Name, AbstractPropertyChange> propChanges ) {
            // only the property adapters should be interested in this
            if (propertyAdapters.isEmpty()) {
                return;
            }
            for (AbstractPropertyChangeAdapter<?> propertyChangeAdapter : propertyAdapters) {
                propertyChangeAdapter.modifyProperties(key, primaryType, mixinTypes, propChanges);                
            }
        }

        @Override
        protected void addNode( String workspaceName, NodeKey key, Path path, Name primaryType, Set<Name> mixinTypes,
                                Properties properties ) {
            // only the path based adapters should be interested in this since properties are handled via modify properties
            if (pathAdapters.isEmpty()) {
                return;
            }
            for (PathBasedChangeAdapter<?> pathAdapter : pathAdapters) {
                pathAdapter.addNode(workspaceName, key, path, primaryType, mixinTypes, properties);
            }
        }

        @Override
        protected void reindexNode( String workspaceName, NodeKey key, Path path, Name primaryType, Set<Name> mixinTypes,
                                    Properties properties, boolean queryable ) {
            
            //first remove all data for the given key...
            String nodeKey = nodeKey(key);
            index().remove(nodeKey);
            
            //then based on each of the adapters types, add information back to the index...
            if (!pathAdapters.isEmpty()) {
                for (PathBasedChangeAdapter<?> pathAdapter : pathAdapters) {
                    index().add(nodeKey, pathAdapter.propertyName, pathAdapter.valueOf(path));
                }   
            }
            if (!propertyAdapters.isEmpty()) {
                for (AbstractPropertyChangeAdapter<?> propertyAdapter : propertyAdapters) {
                    Property property = properties.getProperty(propertyAdapter.propertyName);
                    if (property != null) {
                        propertyAdapter.addValues(key, property);
                    }
                }
            }
        }

        @Override
        protected void removeNode( String workspaceName, NodeKey key, NodeKey parentKey, Path path, Name primaryType,
                                   Set<Name> mixinTypes ) {
            // just remove everything for that node from the index...
            index().remove(nodeKey(key));
        }


        @Override
        protected void moveNode( String workspaceName, NodeKey key, Name primaryType, Set<Name> mixinTypes, NodeKey oldParent,
                                 NodeKey newParent, Path newPath, Path oldPath ) {
            // only the path based adapters should be interested in this since properties are handled via modify properties
            if (pathAdapters.isEmpty()) {
                return;
            }
            for (PathBasedChangeAdapter<?> pathAdapter : pathAdapters) {
                pathAdapter.moveNode(workspaceName, key, primaryType, mixinTypes, oldParent, newParent, newPath, oldPath);
            }
        }

        @Override
        protected void renameNode( String workspaceName, NodeKey key, Path newPath, Path.Segment oldSegment, Name primaryType,
                                   Set<Name> mixinTypes ) {
            // only the path based adapters should be interested in this since properties are handled via modify properties
            if (pathAdapters.isEmpty()) {
                return;
            }
            for (PathBasedChangeAdapter<?> pathAdapter : pathAdapters) {
                pathAdapter.renameNode(workspaceName, key, newPath, oldSegment, primaryType, mixinTypes);
            }
        }

        @Override
        protected void reorderNode( String workspaceName, NodeKey key, Name primaryType, Set<Name> mixinTypes, NodeKey parent,
                                    Path newPath, Path oldPath, Path reorderedBeforePath ) {
            // only the path based adapters should be interested in this since properties are handled via modify properties
            if (pathAdapters.isEmpty()) {
                return;
            }
            for (PathBasedChangeAdapter<?> pathAdapter : pathAdapters) {
                pathAdapter.reorderNode(workspaceName, key, primaryType, mixinTypes, parent, newPath, oldPath, reorderedBeforePath);
            }
        }
    }

    protected static abstract class PathBasedChangeAdapter<T> extends IndexChangeAdapter {
        protected final String propertyName;
        
        protected PathBasedChangeAdapter( ExecutionContext context,
                                          NodeTypePredicate matcher,
                                          String workspaceName,
                                          ProvidedIndex<?> index,
                                          Name propertyName) {
            super(context, workspaceName, matcher, index);
            assert propertyName != null;
            this.propertyName = propertyName.getString(context.getNamespaceRegistry());
        }
        
        protected T valueOf(Path path) {
            return path.isRoot() ? convertRoot(path) : convert(path); 
        }

        protected abstract T convertRoot( Path path );

        protected abstract T convert( Path path );
        
        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties ) {
            index().add(nodeKey(key), propertyName, valueOf(path));
        }

        @Override
        protected void reindexNode( String workspaceName,
                                    NodeKey key,
                                    Path path,
                                    Name primaryType,
                                    Set<Name> mixinTypes,
                                    Properties properties,
                                    boolean queryable ) {
            String nodeKey = nodeKey(key);
            index().remove(nodeKey, propertyName, valueOf(path));
            if (queryable) {
                index().add(nodeKey(key), propertyName, valueOf(path));
            }            
        }

        @Override
        protected void moveNode( String workspaceName,
                                 NodeKey key,
                                 Name primaryType,
                                 Set<Name> mixinTypes,
                                 NodeKey oldParent,
                                 NodeKey newParent,
                                 Path newPath,
                                 Path oldPath ) {
            String nodeKey = nodeKey(key);
            index().remove(nodeKey, propertyName, valueOf(oldPath));
            index().add(nodeKey, propertyName, valueOf(newPath));
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes ) {
            index().remove(nodeKey(key));
        }

        @Override
        protected void renameNode( String workspaceName, 
                                   NodeKey key, 
                                   Path newPath, 
                                   Path.Segment oldSegment, 
                                   Name primaryType,
                                   Set<Name> mixinTypes ) {
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            Path oldPath = pathFactory.create(newPath.subpath(0, newPath.size()), oldSegment);
            String nodeKey = nodeKey(key);
            index().remove(nodeKey, propertyName, valueOf(oldPath));
            index().add(nodeKey, propertyName, valueOf(newPath));
        }

        @Override
        protected void reorderNode( String workspaceName, 
                                    NodeKey key, 
                                    Name primaryType, 
                                    Set<Name> mixinTypes, 
                                    NodeKey parent,
                                    Path newPath, 
                                    Path oldPath, 
                                    Path reorderedBeforePath ) {
            if (newPath.getLastSegment().hasIndex() || oldPath.getLastSegment().hasIndex()) {
                //TODO author=Horia Chiorean date=01/10/2015 description=See https://issues.jboss.org/browse/MODE-2510
            }
            //otherwise no SNS are involved so the nodekey and path information doesn't need to change
        }
        @Override
        public String toString() {
            return  getClass().getSimpleName() + "(\"" + index.getName() + "\" : \"" + propertyName + "\")";
        }
    }

    protected static final class NodeDepthChangeAdapter extends PathBasedChangeAdapter<Long> {
        public NodeDepthChangeAdapter( ExecutionContext context,
                                       NodeTypePredicate matcher,
                                       String workspaceName,
                                       ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, index, ModeShapeLexicon.DEPTH);
        }

        @Override
        protected Long convert( Path path ) {
            return (long)path.size();
        }

        @Override
        protected Long convertRoot( Path path ) {
            return convert(path);
        }
    }

    protected static final class NodeNameChangeAdapter extends PathBasedChangeAdapter<Name> {
        public NodeNameChangeAdapter( ExecutionContext context,
                                      NodeTypePredicate matcher,
                                      String workspaceName,
                                      ProvidedIndex<?> index) {
            super(context, matcher, workspaceName, index, JcrLexicon.NAME);
        }

        @Override
        protected Name convert( Path path ) {
            return path.getLastSegment().getName();
        }

        @Override
        protected Name convertRoot( Path path ) {
            return Path.ROOT_NAME;
        }

        @Override
        protected void reorderNode( String workspaceName, NodeKey key, Name primaryType, Set<Name> mixinTypes, NodeKey parent,
                                    Path newPath, Path oldPath, Path reorderedBeforePath ) {
            // reordering should not really change the name...
        }
    }

    protected static final class NodeLocalNameChangeAdapter extends PathBasedChangeAdapter<String> {
        public NodeLocalNameChangeAdapter( ExecutionContext context,
                                           NodeTypePredicate matcher,
                                           String workspaceName,
                                           ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, index, ModeShapeLexicon.LOCALNAME);
        }

        @Override
        protected String convert( Path path ) {
            return path.getLastSegment().getName().getLocalName();
        }

        @Override
        protected String convertRoot( Path path ) {
            return "";
        }

        @Override
        protected void reorderNode( String workspaceName, NodeKey key, Name primaryType, Set<Name> mixinTypes, NodeKey parent,
                                    Path newPath, Path oldPath, Path reorderedBeforePath ) {
            // reordering should not really change the name...
        }
    }

    protected static final class NodePathChangeAdapter extends PathBasedChangeAdapter<Path> {
        public NodePathChangeAdapter( ExecutionContext context,
                                      NodeTypePredicate matcher,
                                      String workspaceName,
                                      ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, index, JcrLexicon.PATH);
        }

        @Override
        protected Path convert( Path path ) {
            return path;
        }

        @Override
        protected Path convertRoot( Path path ) {
            return path;
        }
    }

    protected static abstract class AbstractPropertyChangeAdapter<T> extends IndexChangeAdapter {
        protected final Name propertyName;
        protected final ValueFactory<T> valueFactory;

        public AbstractPropertyChangeAdapter( ExecutionContext context,
                                              NodeTypePredicate matcher,
                                              String workspaceName,
                                              Name propertyName,
                                              ValueFactory<T> valueFactory,
                                              ProvidedIndex<?> index) {
            super(context, workspaceName, matcher, index);
            this.propertyName = propertyName;
            this.valueFactory = valueFactory;
        }

        protected final T convert( Object value ) {
            return valueFactory.create(value);
        }

        protected abstract void addValues( NodeKey key,
                                           Property property );

        protected abstract void addValue( NodeKey key,
                                          Object value );
        
        protected abstract void removeValues(NodeKey key, Property property);
        
        protected final String propertyName() {
            return propertyName.getString(context.getNamespaceRegistry());
        }

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties ) {
            // Properties on new nodes are always represented as 'PropertyAdded' events, and handled via 'modifyProperties' ...
        }

        @Override
        protected void reindexNode( String workspaceName,
                                    NodeKey key,
                                    Path path,
                                    Name primaryType,
                                    Set<Name> mixinTypes,
                                    Properties properties,
                                    boolean queryable ) {
            if (properties != null) {
                assert propertyName != null;
                Property prop = properties.getProperty(propertyName);
                if (prop != null) {
                    removeValues(key, prop);
                    if (queryable) {
                        addValues(key, prop);
                    }
                }
            }
        }

        @Override
        protected void modifyProperties( NodeKey key,
                                         Name primaryType, 
                                         Set<Name> mixinTypes, 
                                         Map<Name, AbstractPropertyChange> propChanges ) {
            AbstractPropertyChange propChange = propChanges.get(propertyName);
            if (propChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)propChange;
                removeValues(key, change.getOldProperty());
                addValues(key, change.getNewProperty());
            } else if (propChange instanceof PropertyAdded) {
                PropertyAdded added = (PropertyAdded)propChange;
                addValues(key, added.getProperty());
            } else if (propChange instanceof PropertyRemoved) {
                removeValues(key, propChange.getProperty());
            }
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes ) {
            index().remove(nodeKey(key));
        }

        @Override
        public String toString() {
            return  getClass().getSimpleName() + "(\"" + index.getName() + "\" : \"" + propertyName() + "\")"; 
        }
    }

    protected static class PropertyChangeAdapter<T> extends AbstractPropertyChangeAdapter<T> {
        public PropertyChangeAdapter( ExecutionContext context,
                                      NodeTypePredicate matcher,
                                      String workspaceName,
                                      Name propertyName,
                                      ValueFactory<T> valueFactory,
                                      ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, propertyName, valueFactory, index);
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            if (property.isEmpty()) {
                return;
            }
            String nodeKey = nodeKey(key);
            String propertyName = propertyName();
            if (property.isMultiple()) {
                index().add(nodeKey, propertyName, property.getValuesAsArray(valueFactory));
            } else {
                index().add(nodeKey, propertyName,  convert(property.getFirstValue()));
            }
        }

        @Override
        protected void addValue( NodeKey key,
                                 Object value ) {
            if (value == null) {
                return;
            }
            index().add(nodeKey(key), propertyName(), convert(value));
        }

        @Override
        protected void removeValues( NodeKey key, Property property ) {
            if (property.isEmpty()) {
                return;
            }
            String nodeKey = nodeKey(key);
            String propertyName = propertyName();
            if (property.isMultiple()) {
                index().remove(nodeKey, propertyName, property.getValuesAsArray(valueFactory));
            } else {
                index().remove(nodeKey, propertyName, convert(property.getFirstValue())); 
            }
        }
    }
    
    protected static final class PrimaryTypeChangeAdapter extends PropertyChangeAdapter<Name> {
        public PrimaryTypeChangeAdapter( ExecutionContext context,
                                         NodeTypePredicate matcher,
                                         String workspaceName,
                                         ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, JcrLexicon.PRIMARY_TYPE, context.getValueFactories().getNameFactory(), index);
        }
    }

    protected static final class MixinTypesChangeAdapter extends PropertyChangeAdapter<Name> {
        public MixinTypesChangeAdapter( ExecutionContext context,
                                        NodeTypePredicate matcher,
                                        String workspaceName,
                                        ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, JcrLexicon.MIXIN_TYPES, context.getValueFactories().getNameFactory(), index);
        }
    }

    protected static final class UniquePropertyChangeAdapter<T> extends AbstractPropertyChangeAdapter<T> {
        public UniquePropertyChangeAdapter( ExecutionContext context,
                                            NodeTypePredicate matcher,
                                            String workspaceName,
                                            Name propertyName,
                                            ValueFactory<T> valueFactory,
                                            ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, propertyName, valueFactory, index);
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            index().add(nodeKey(key), propertyName(), convert(property.getFirstValue()));
        }

        @Override
        protected final void addValue( NodeKey key,
                                       Object value ) {
            index().add(nodeKey(key), propertyName(), convert(value));
        }

        @Override
        protected void removeValues( NodeKey key, Property property ) {
            index().remove(nodeKey(key), propertyName(), convert(property.getFirstValue()));
        }
    }

    protected static class EnumeratedPropertyChangeAdapter extends PropertyChangeAdapter<String> {
        public EnumeratedPropertyChangeAdapter( ExecutionContext context,
                                                NodeTypePredicate matcher,
                                                String workspaceName,
                                                Name propertyName,
                                                ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, propertyName, context.getValueFactories().getStringFactory(), index);
        }
    }

    protected static final class NodeTypesChangeAdapter extends EnumeratedPropertyChangeAdapter {
        private final String property;
        public NodeTypesChangeAdapter( String property, 
                                       ExecutionContext context,
                                       NodeTypePredicate matcher,
                                       String workspaceName,
                                       ProvidedIndex<?> index ) {
            // note that this doesn't care about the property, for which it will use the name of the index
            super(context, matcher, workspaceName, null, index);
            this.property = property;
            assert this.property != null;
        }
        
        @Override
        protected void modifyProperties( NodeKey key,
                                         Name primaryType, 
                                         Set<Name> mixinTypes, 
                                         Map<Name, AbstractPropertyChange> propChanges ) {
            List<Object> newValues = new ArrayList<>();
            List<Object> oldValues = new ArrayList<>();
            
            AbstractPropertyChange primaryTypeChange = propChanges.get(JcrLexicon.PRIMARY_TYPE);
            if (primaryTypeChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)primaryTypeChange;
                oldValues.add(change.getOldProperty().getFirstValue());
                newValues.add(change.getNewProperty().getFirstValue());
            } else if (primaryTypeChange instanceof PropertyAdded) {
                newValues.add(primaryTypeChange.getProperty().getFirstValue());
            } else if (primaryTypeChange instanceof PropertyRemoved) {
                oldValues.add(primaryTypeChange.getProperty().getFirstValue());
            }

            AbstractPropertyChange mixinsTypeChange = propChanges.get(JcrLexicon.MIXIN_TYPES);
            if (mixinsTypeChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)mixinsTypeChange;
                Property oldProperty = change.getOldProperty();
                if (!oldProperty.isEmpty()) {
                    oldValues.addAll(Arrays.asList(oldProperty.getValuesAsArray()));
                }
                Property newProperty = change.getNewProperty();
                if (!newProperty.isEmpty()) {
                    newValues.addAll(Arrays.asList(newProperty.getValuesAsArray()));
                }
            } else if (mixinsTypeChange instanceof PropertyAdded) {
                newValues.addAll(Arrays.asList(mixinsTypeChange.getProperty().getValuesAsArray()));
            } else if (mixinsTypeChange instanceof PropertyRemoved) {
                oldValues.addAll(Arrays.asList(mixinsTypeChange.getProperty().getValuesAsArray()));
            }

            if (primaryTypeChange == null && mixinsTypeChange == null) {
                // neither the primary nor the mixins have changed, so nothing to do...
                return;
            }
            if (primaryTypeChange == null) {
                // only the mixins have changed so to have the complete information we need to add the primary type
                newValues.add(primaryType);
            } else if (mixinsTypeChange == null && mixinTypes != null) {
                // only the primary type has changed, to have the complete information we need to add the 
                newValues.addAll(mixinTypes);
            }
            
            String nodeKey = nodeKey(key);
            if (!oldValues.isEmpty()) {
                // there are values which require removal, so remove all of them because we'll submit a complete set of values 
                // below
                index().remove(nodeKey);
            }
            assert !newValues.isEmpty();
            index().add(nodeKey, property, valueFactory.create(newValues.toArray()));
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes ) {
            index().remove(nodeKey(key));
        }

        @Override
        protected void reindexNode( String workspaceName, NodeKey key, Path path, Name primaryType, Set<Name> mixinTypes,
                                    Properties properties, boolean queryable ) {
            String nodeKey = nodeKey(key);
            index().remove(nodeKey);
            if (!queryable) {
                return;
            }
            addTypeInformation(key, primaryType, mixinTypes);
        }


        private void addTypeInformation( NodeKey key, Name primaryType, Set<Name> mixinTypes ) {
            List<Name> values = new ArrayList<>();
            values.add(primaryType);
            if (!mixinTypes.isEmpty()) {
                values.addAll(mixinTypes);
            }
            index().add(nodeKey(key), property, valueFactory.create(values.toArray()));
        }
    }
    
    protected static final class TextPropertyChangeAdapter extends PropertyChangeAdapter<String> {
        public TextPropertyChangeAdapter( ExecutionContext context,
                                          NodeTypePredicate matcher, String workspaceName, Name propertyName,
                                          ValueFactory<String> valueFactory, ProvidedIndex<?> index ) {
            super(context, matcher, workspaceName, propertyName, valueFactory, index);
        }

        @Override
        protected void addValues( NodeKey key, Property property ) {
            StringBuilder builder = textFrom(property);
            if (builder.length() > 0) {
                index().add(nodeKey(key), propertyName(), builder.toString());    
            }
        }

        @Override
        protected void removeValues( NodeKey key, Property property ) {
            StringBuilder builder = textFrom(property);
            if (builder.length() > 0) {
                index().remove(nodeKey(key),propertyName(), builder.toString());
            }
        }
        
        protected StringBuilder textFrom(Property property) {
            StringBuilder builder = new StringBuilder();
            if (property.isEmpty()) {
                return builder;
            }
            if (!property.isBinary()) {
                String[] values = property.getValuesAsArray(valueFactory);  
                for (int i = 0; i < values.length; i++) {
                    builder.append(values[i]);
                    if (i < values.length - 1) {
                        builder.append(" ");
                    }
                }
            } else {
                for (Iterator<Object> valuesIterator = property.iterator(); valuesIterator.hasNext();) {
                    Object value = valuesIterator.next();
                    assert value instanceof BinaryValue;
                    BinaryValue binaryValue = (BinaryValue) value;
                    try {
                        String extractedText = context.getBinaryStore().getText(binaryValue);
                        builder.append(extractedText);
                        if (valuesIterator.hasNext()) {
                            builder.append(" ");
                        }
                    } catch (BinaryStoreException e) {
                        logger.debug(e, "Error trying to get extracted text for {0}", binaryValue);
                    }
                }
            }
            return builder;
        }
    }

}
