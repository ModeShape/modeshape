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

package org.modeshape.jcr.index.local;

import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.AbstractPropertyChange;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.spi.index.provider.IndexChangeAdapter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Utility for creating {@link IndexChangeAdapter} instances.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class IndexChangeAdapters {

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "mode:nodeDepth" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeDepth( ExecutionContext context,
                                                   String workspaceName,
                                                   LocalDuplicateIndex<Long> index ) {
        return new NodeDepthChangeAdapter(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:name" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeName( ExecutionContext context,
                                                  String workspaceName,
                                                  LocalDuplicateIndex<Name> index ) {
        return new NodeNameChangeAdapter(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "mode:localName" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeLocalName( ExecutionContext context,
                                                       String workspaceName,
                                                       LocalDuplicateIndex<String> index ) {
        return new NodeLocalNameChangeAdapter(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:path" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodePath( ExecutionContext context,
                                                  String workspaceName,
                                                  LocalDuplicateIndex<Path> index ) {
        return new NodePathChangeAdapter(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:primaryType" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forPrimaryType( ExecutionContext context,
                                                     String workspaceName,
                                                     LocalDuplicateIndex<Name> index ) {
        return new PrimaryTypeChangeAdatper(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles the "jcr:mixinTypes" property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forMixinTypes( ExecutionContext context,
                                                    String workspaceName,
                                                    LocalDuplicateIndex<Name> index ) {
        return new PrimaryTypeChangeAdatper(context, workspaceName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a single-valued property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static <T> IndexChangeAdapter forSingleValuedProperty( ExecutionContext context,
                                                                  String workspaceName,
                                                                  Name propertyName,
                                                                  ValueFactory<T> factory,
                                                                  LocalDuplicateIndex<T> index ) {
        return new SingleValuedPropertyChangeAdapter<T>(context, workspaceName, propertyName, factory, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a multi-valued property.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static <T> IndexChangeAdapter forMultiValuedProperty( ExecutionContext context,
                                                                 String workspaceName,
                                                                 Name propertyName,
                                                                 ValueFactory<T> factory,
                                                                 LocalDuplicateIndex<T> index ) {
        return new MultiValuedPropertyChangeAdapter<T>(context, workspaceName, propertyName, factory, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a unique-valued property, where every property value is
     * unique across all nodes.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param factory the value factory for the property's value type; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static <T> IndexChangeAdapter forUniqueValuedProperty( ExecutionContext context,
                                                                  String workspaceName,
                                                                  Name propertyName,
                                                                  ValueFactory<T> factory,
                                                                  LocalUniqueIndex<T> index ) {
        return new UniquePropertyChangeAdapter<T>(context, workspaceName, propertyName, factory, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a single-valued enumerated property. Because all
     * enumerated values are distinct, they are treated as strings.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forSingleValuedEnumeratedProperty( ExecutionContext context,
                                                                        String workspaceName,
                                                                        Name propertyName,
                                                                        LocalEnumeratedIndex index ) {
        return new SingleValuedEnumeratedPropertyChangeAdapter(context, workspaceName, propertyName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles a multi-valued enumerated property. Because all enumerated
     * values are distinct, they are treated as strings.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param propertyName the name of the property; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forMultiValuedEnumeratedProperty( ExecutionContext context,
                                                                       String workspaceName,
                                                                       Name propertyName,
                                                                       LocalEnumeratedIndex index ) {
        return new MultiValuedEnumeratedPropertyChangeAdapter(context, workspaceName, propertyName, index);
    }

    /**
     * Create an {@link IndexChangeAdapter} implementation that handles node type information.
     *
     * @param context the execution context; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param index the local index that should be used; may not be null
     * @return the new {@link IndexChangeAdapter}; never null
     */
    public static IndexChangeAdapter forNodeTypes( ExecutionContext context,
                                                   String workspaceName,
                                                   LocalIndex<String> index ) {
        return new NodeTypesChangeAdapter(context, workspaceName, index);
    }

    private IndexChangeAdapters() {
    }

    protected static abstract class PathBasedChangeAdapter<T> extends IndexChangeAdapter {
        private final LocalDuplicateIndex<T> index;
        private final boolean includeRoot;

        protected PathBasedChangeAdapter( ExecutionContext context,
                                          String workspaceName,
                                          LocalDuplicateIndex<T> index,
                                          boolean includeRoot ) {
            super(context, workspaceName);
            this.index = index;
            this.includeRoot = includeRoot;
        }

        protected abstract T convertRoot( Path path );

        protected abstract T convert( Path path );

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
            if (path.isRoot() && includeRoot) {
                index.add(nodeKey(key), convertRoot(path));
            } else {
                index.add(nodeKey(key), convert(path));
            }
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes,
                                   boolean queryable ) {
            if (includeRoot || path.isRoot()) {
                index.remove(nodeKey(key));
            }
        }

        @Override
        protected void changeNode( String workspaceName,
                                   NodeKey key,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes,
                                   boolean queryable ) {
            if (path.isRoot() && includeRoot) {
                String nodeKey = nodeKey(key);
                index.remove(nodeKey);
                index.add(nodeKey, convertRoot(path));
            } else {
                String nodeKey = nodeKey(key);
                index.remove(nodeKey);
                index.add(nodeKey, convert(path));
            }
        }
    }

    protected static final class NodeDepthChangeAdapter extends PathBasedChangeAdapter<Long> {
        public NodeDepthChangeAdapter( ExecutionContext context,
                                       String workspaceName,
                                       LocalDuplicateIndex<Long> index ) {
            super(context, workspaceName, index, true);
        }

        @Override
        protected Long convert( Path path ) {
            return (long)path.size();
        }

        @Override
        protected Long convertRoot( Path path ) {
            return (long)path.size();
        }
    }

    protected static final class NodeNameChangeAdapter extends PathBasedChangeAdapter<Name> {
        public NodeNameChangeAdapter( ExecutionContext context,
                                      String workspaceName,
                                      LocalDuplicateIndex<Name> index ) {
            super(context, workspaceName, index, true);
        }

        @Override
        protected Name convert( Path path ) {
            return path.getLastSegment().getName();
        }

        @Override
        protected Name convertRoot( Path path ) {
            return Path.ROOT_NAME;
        }
    }

    protected static final class NodeLocalNameChangeAdapter extends PathBasedChangeAdapter<String> {
        public NodeLocalNameChangeAdapter( ExecutionContext context,
                                           String workspaceName,
                                           LocalDuplicateIndex<String> index ) {
            super(context, workspaceName, index, true);
        }

        @Override
        protected String convert( Path path ) {
            return path.getLastSegment().getName().getLocalName();
        }

        @Override
        protected String convertRoot( Path path ) {
            return "";
        }
    }

    protected static final class NodePathChangeAdapter extends PathBasedChangeAdapter<Path> {
        public NodePathChangeAdapter( ExecutionContext context,
                                      String workspaceName,
                                      LocalDuplicateIndex<Path> index ) {
            super(context, workspaceName, index, true);
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

    protected static String nodeKey( NodeKey key ) {
        return key.toString();
    }

    protected static abstract class AbstractPropertyChangeAdapter<T> extends IndexChangeAdapter {
        protected final Name propertyName;
        protected final ValueFactory<T> valueFactory;

        public AbstractPropertyChangeAdapter( ExecutionContext context,
                                              String workspaceName,
                                              Name propertyName,
                                              ValueFactory<T> valueFactory ) {
            super(context, workspaceName);
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

        protected abstract void removeValues( NodeKey key );

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
            Property prop = properties.getProperty(propertyName);
            if (prop != null) {
                addValues(key, prop);
            }
        }

        @Override
        protected void modifyProperties( NodeKey key,
                                         Map<Name, AbstractPropertyChange> propChanges ) {
            AbstractPropertyChange propChange = propChanges.get(propertyName);
            if (propChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)propChange;
                removeValues(key);
                addValues(key, change.getNewProperty());
            }
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes,
                                   boolean queryable ) {
            removeValues(key);
        }
    }

    protected static abstract class PropertyChangeAdapter<T> extends AbstractPropertyChangeAdapter<T> {
        protected final LocalDuplicateIndex<T> index;

        public PropertyChangeAdapter( ExecutionContext context,
                                      String workspaceName,
                                      Name propertyName,
                                      ValueFactory<T> valueFactory,
                                      LocalDuplicateIndex<T> index ) {
            super(context, workspaceName, propertyName, valueFactory);
            this.index = index;
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            for (Object value : property) {
                index.add(nodeKey(key), convert(value));
            }
        }

        @Override
        protected final void addValue( NodeKey key,
                                       Object value ) {
            index.add(nodeKey(key), convert(value));
        }

        @Override
        protected void removeValues( NodeKey key ) {
            index.remove(nodeKey(key));
        }
    }

    protected static final class SingleValuedPropertyChangeAdapter<T> extends PropertyChangeAdapter<T> {
        public SingleValuedPropertyChangeAdapter( ExecutionContext context,
                                                  String workspaceName,
                                                  Name propertyName,
                                                  ValueFactory<T> valueFactory,
                                                  LocalDuplicateIndex<T> index ) {
            super(context, workspaceName, propertyName, valueFactory, index);
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            index.add(nodeKey(key), convert(property.getFirstValue()));
        }
    }

    protected static final class MultiValuedPropertyChangeAdapter<T> extends PropertyChangeAdapter<T> {
        public MultiValuedPropertyChangeAdapter( ExecutionContext context,
                                                 String workspaceName,
                                                 Name propertyName,
                                                 ValueFactory<T> valueFactory,
                                                 LocalDuplicateIndex<T> index ) {
            super(context, workspaceName, propertyName, valueFactory, index);
        }
    }

    protected static final class PrimaryTypeChangeAdatper extends PropertyChangeAdapter<Name> {
        public PrimaryTypeChangeAdatper( ExecutionContext context,
                                         String workspaceName,
                                         LocalDuplicateIndex<Name> index ) {
            super(context, workspaceName, JcrLexicon.PRIMARY_TYPE, context.getValueFactories().getNameFactory(), index);
        }

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
            addValue(key, primaryType);
        }
    }

    protected static final class MixinTypesChangeAdatper extends PropertyChangeAdapter<Name> {
        public MixinTypesChangeAdatper( ExecutionContext context,
                                        String workspaceName,
                                        LocalDuplicateIndex<Name> index ) {
            super(context, workspaceName, JcrLexicon.MIXIN_TYPES, context.getValueFactories().getNameFactory(), index);
        }

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
            if (mixinTypes != null && !mixinTypes.isEmpty()) {
                for (Name mixinType : mixinTypes) {
                    addValue(key, mixinType);
                }
            }
        }
    }

    protected static final class UniquePropertyChangeAdapter<T> extends AbstractPropertyChangeAdapter<T> {
        protected final LocalUniqueIndex<T> index;

        public UniquePropertyChangeAdapter( ExecutionContext context,
                                            String workspaceName,
                                            Name propertyName,
                                            ValueFactory<T> valueFactory,
                                            LocalUniqueIndex<T> index ) {
            super(context, workspaceName, propertyName, valueFactory);
            this.index = index;
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            index.add(nodeKey(key), convert(property.getFirstValue()));
        }

        @Override
        protected final void addValue( NodeKey key,
                                       Object value ) {
            index.add(nodeKey(key), convert(value));
        }

        @Override
        protected void removeValues( NodeKey key ) {
            index.remove(nodeKey(key));
        }
    }

    protected static abstract class EnumeratedPropertyChangeAdapter extends AbstractPropertyChangeAdapter<String> {
        protected final LocalIndex<String> index;

        public EnumeratedPropertyChangeAdapter( ExecutionContext context,
                                                String workspaceName,
                                                Name propertyName,
                                                LocalIndex<String> index ) {
            super(context, workspaceName, propertyName, context.getValueFactories().getStringFactory());
            this.index = index;
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            for (Object value : property) {
                index.add(nodeKey(key), convert(value));
            }
        }

        @Override
        protected final void addValue( NodeKey key,
                                       Object value ) {
            index.add(nodeKey(key), convert(value));
        }

        @Override
        protected void removeValues( NodeKey key ) {
            index.remove(nodeKey(key));
        }
    }

    protected static final class SingleValuedEnumeratedPropertyChangeAdapter extends EnumeratedPropertyChangeAdapter {
        public SingleValuedEnumeratedPropertyChangeAdapter( ExecutionContext context,
                                                            String workspaceName,
                                                            Name propertyName,
                                                            LocalEnumeratedIndex index ) {
            super(context, workspaceName, propertyName, index);
        }

        @Override
        protected void addValues( NodeKey key,
                                  Property property ) {
            index.add(nodeKey(key), convert(property.getFirstValue()));
        }
    }

    protected static final class MultiValuedEnumeratedPropertyChangeAdapter extends EnumeratedPropertyChangeAdapter {
        public MultiValuedEnumeratedPropertyChangeAdapter( ExecutionContext context,
                                                           String workspaceName,
                                                           Name propertyName,
                                                           LocalEnumeratedIndex index ) {
            super(context, workspaceName, propertyName, index);
        }
    }

    protected static final class NodeTypesChangeAdapter extends EnumeratedPropertyChangeAdapter {
        public NodeTypesChangeAdapter( ExecutionContext context,
                                       String workspaceName,
                                       LocalIndex<String> index ) {
            super(context, workspaceName, null, index);
        }

        protected final void removeValues( NodeKey key,
                                           Property oldProperty ) {
            for (Object value : oldProperty) {
                removeValue(key, value);
            }
        }

        protected final void removeValue( NodeKey key,
                                          Object value ) {
            index.remove(nodeKey(key), convert(value));
        }

        @Override
        protected void addNode( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
            addValue(key, primaryType);
            if (!mixinTypes.isEmpty()) {
                for (Name mixinType : mixinTypes) {
                    addValue(key, mixinType);
                }
            }
        }

        @Override
        protected void modifyProperties( NodeKey key,
                                         Map<Name, AbstractPropertyChange> propChanges ) {
            AbstractPropertyChange propChange = propChanges.get(JcrLexicon.PRIMARY_TYPE);
            if (propChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)propChange;
                addValue(key, change.getNewProperty().getFirstValue());
                removeValue(key, change.getOldProperty().getFirstValue());
            }
            propChange = propChanges.get(JcrLexicon.MIXIN_TYPES);
            if (propChange instanceof PropertyChanged) {
                PropertyChanged change = (PropertyChanged)propChange;
                removeValues(key, change.getOldProperty());
                addValues(key, change.getNewProperty());
            }
        }

        @Override
        protected void removeNode( String workspaceName,
                                   NodeKey key,
                                   NodeKey parentKey,
                                   Path path,
                                   Name primaryType,
                                   Set<Name> mixinTypes,
                                   boolean queryable ) {
            removeValues(key);
        }
    }

}
